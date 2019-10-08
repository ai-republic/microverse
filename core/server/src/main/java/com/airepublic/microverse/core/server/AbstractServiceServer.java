/**
   Copyright 2015 Torsten Oltmanns, ai-republic GmbH, Germany

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.airepublic.microverse.core.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.Action;
import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.MethodCall;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.descriptor.WebCall;
import com.airepublic.microverse.core.discovery.RegistryDiscovererService;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Base class for all service-servers.
 *
 * @author Torsten Oltmanns
 *
 */
@Singleton
public abstract class AbstractServiceServer implements Closeable {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractServiceServer.class);
	private final Map<String, ServiceContainer> serviceUriMap = new HashMap<>();
	private Path serviceDir;

	private static final String SERVICE_ID = "service-id=";
	private static final String SERVICE_VERSION = "service-version=";
	private static final String SERVICE_CLASS = "service-class=";
	private static final String SERVICE_INTERFACE = "service-interface=";


	/**
	 * Constructor.
	 */
	public AbstractServiceServer() {

	}


	@PostConstruct
	public void init() {
		final String dir = Configuration.getServiceDeployDir();

		if (dir != null) {
			serviceDir = Paths.get(dir);
		} else {
			LOG.warn("No deploy directory configured. Please set the system property '" + Configuration.getServiceDeployDir() + "'!");
			serviceDir = Paths.get(".");
		}
	}


	/**
	 * Gets the {@link IClassLoaderCreator} used to load the service-container.
	 *
	 * @return the {@link IClassLoaderCreator}
	 */
	protected abstract IClassLoaderCreator getClassLoaderCreator();


	/**
	 * Gets the supported mime-types the service server supports for marshalling/unmarshalling
	 * requests/responses.
	 *
	 * @return the list of mime-types
	 */
	public List<String> getSupportedMimeTypes() {
		return MarshallerFactory.getSupportedMimeTypes();
	}


	/**
	 * Adds a service described by the zip file in the input stream.
	 *
	 * @param host the host the service is running on
	 * @param port the port the service is running on
	 * @param useSSL flag, whether to use SSL to connect to the service
	 * @param inputStream the request content input-stream containing the service-bundle zipfile
	 *        bytes
	 * @return the {@link ServiceDescriptor} with all parameters set
	 */
	protected ServiceDescriptor addService(final String host, final int port, final boolean useSSL, final InputStream inputStream) throws ServiceException {
		ServiceDescriptor serviceDescriptor = null;

		try {
			synchronized (serviceUriMap) {
				final byte[] serviceBundleZip = IOUtils.toByteArray(inputStream);

				// set server parameters
				serviceDescriptor = readMicroserviceClass(serviceBundleZip);

				serviceDescriptor.setHost(host);
				serviceDescriptor.setPort(port);
				serviceDescriptor.setUseSSL(useSSL);
				serviceDescriptor.setServiceUri(getServiceWebCall(serviceDescriptor.getId(), serviceDescriptor.getVersion()));
				serviceDescriptor.setHeartbeatUri(getHeartbeatWebCall(serviceDescriptor.getId(), serviceDescriptor.getVersion()));

				// set the supported mime-types available on this server
				serviceDescriptor.setSupportedMimeTypes(getSupportedMimeTypes());

				// create the service-container
				final ServiceContainer serviceContainer = ServiceContainer.create(serviceDescriptor, serviceBundleZip, serviceDir, getClassLoaderCreator());

				final String serviceUri = "/" + serviceDescriptor.getServiceUri().getUri();

				// add/swap the uri to the new service
				final ServiceContainer oldServiceContainer = serviceUriMap.put(serviceUri, serviceContainer);
				propagateServiceToRegistry(serviceDescriptor, Action.ADD);

				onServiceRegistration(serviceDescriptor);

				// clean up the old container it it was a replacement
				if (oldServiceContainer != null) {
					oldServiceContainer.close();
					LOG.debug("Service was replaced: " + serviceContainer.getServiceDescriptor());
				} else {
					LOG.debug("Service was added: " + serviceContainer.getServiceDescriptor());
				}
			}

			return serviceDescriptor;
		} catch (final ServiceException e) {
			throw e;
		} catch (final Exception e) {
			throw new ServiceException("Could not add service: " + serviceDescriptor, e);
		}
	}


	protected abstract WebCall getServiceWebCall(String serviceId, String serviceVersion);


	protected abstract WebCall getHeartbeatWebCall(String serviceId, String serviceVersion);


	private ServiceDescriptor readMicroserviceClass(final byte[] bundleZip) throws ServiceException {
		final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bundleZip));
		ZipEntry entry;
		String id = null;
		String version = null;
		String serviceClass = null;
		String serviceInterface = null;

		try {
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().endsWith(Configuration.getAutoDeployServiceDescriptorFilename())) {
					final BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
					String line = reader.readLine();

					while (line != null) {

						if (line.startsWith(SERVICE_ID)) {
							id = line.substring(SERVICE_ID.length()).trim();
						} else if (line.startsWith(SERVICE_VERSION)) {
							version = line.substring(SERVICE_VERSION.length()).trim();
						} else if (line.startsWith(SERVICE_CLASS)) {
							serviceClass = line.substring(SERVICE_CLASS.length()).trim();
						} else if (line.startsWith(SERVICE_INTERFACE)) {
							serviceInterface = line.substring(SERVICE_INTERFACE.length()).trim();
						}

						line = reader.readLine();
					}

					reader.close();
					break;
				}

				zis.closeEntry();
			}

			zis.close();

			return ServiceDescriptor.create(id, version, serviceClass, serviceInterface);
		} catch (final Exception e) {
			throw new ServiceException("Error reading auto-deploy service-descriptor file '" + Configuration.getAutoDeployServiceDescriptorFilename() + "' from bundle!", e);
		}
	}


	/**
	 * Removes a service described by the {@link ServiceDescriptor} in the request content stream.
	 *
	 * @param mimeType the mimeType the {@link ServiceDescriptor} is encoded in the input-stream
	 * @param inputStream the request content input-stream
	 */
	protected void removeService(final String serviceId, final String serviceVersion) throws ServiceException {
		try {
			synchronized (serviceUriMap) {
				ServiceContainer serviceContainer = serviceUriMap.remove("/" + getServiceWebCall(serviceId, serviceVersion).getUri());

				if (serviceContainer != null) {
					final ServiceDescriptor serviceDescriptor = serviceContainer.getServiceDescriptor();
					LOG.info("Shutting down service '" + serviceId + ":" + serviceVersion + "' servicing on '" + serviceDescriptor.getHost() + ":" + serviceDescriptor.getPort() + "/" + serviceDescriptor.getServiceUri() + "'");

					propagateServiceToRegistry(serviceDescriptor, Action.REMOVE);
					serviceContainer.close();
					serviceContainer = null;
					onServiceUnregistration(serviceDescriptor);
				} else {
					LOG.warn("Service '" + serviceId + ":" + serviceVersion + "' could not be found and removed!");
				}
			}
		} catch (final Exception e) {
			LOG.error("Could not remove service: " + serviceId + ":" + serviceVersion, e);
			throw new ServiceException("Could not remove service: " + serviceId + ":" + serviceVersion, e);
		}
	}


	/**
	 * Propagates the service change to the registries.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor}
	 * @param action the synchonization action
	 */
	// TODO request registries each time or use RegistryCache?
	void propagateServiceToRegistry(final ServiceDescriptor serviceDescriptor, final Action action) throws ServiceException {
		try {
			for (final RegistryDescriptor registryDescriptor : RegistryDiscovererService.requestRegistries()) {
				try {
					WebCall webCall = null;

					switch (action) {
						case ADD:
							webCall = registryDescriptor.getRegisterUri();
							break;
						case REMOVE:
							webCall = registryDescriptor.getUnregisterUri();
							break;
					}

					ServiceUtils.executeRequest(registryDescriptor, webCall, serviceDescriptor);
				} catch (final Exception e) {
					LOG.warn("Service (" + serviceDescriptor + ") could not be propagated to registry (" + registryDescriptor + ")", e);
					throw new ServiceException("Service (" + serviceDescriptor + ") could not be propagated to registry (" + registryDescriptor + ")", e);
				}
			}
		} catch (final Exception e) {
			LOG.warn("Registries could not be discovered for service propagation!", e);
			throw new ServiceException("Registries could not be discovered for service propagation!", e);
		}
	}


	/**
	 * Called before a service is registered with the registry-servers.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor}
	 */
	protected abstract void onServiceRegistration(ServiceDescriptor serviceDescriptor);


	/**
	 * Called before a service is registered with the registry-servers.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor}
	 */
	protected abstract void onServiceUnregistration(ServiceDescriptor serviceDescriptor);


	/**
	 * Unmarshal the {@link ServiceDescriptor} from the request input-stream.
	 *
	 * @param headers the request headers
	 * @param inputStream the {@link InputStream}
	 * @return the {@link ServiceDescriptor}
	 * @throws ServiceException
	 */
	protected <T> T unmarshal(final String mimeType, final InputStream inputStream, final Class<T> clazz) throws ServiceException {
		T result = null;

		// try to get a marshaller configured to handle the request content-type
		final IMarshaller marshaller = MarshallerFactory.get(mimeType);

		if (marshaller != null) {
			// deserialize
			result = marshaller.deserialize(inputStream, clazz);
		} else {
			LOG.error("No marshaller found for mime-type: " + mimeType);
			throw new ServiceException("No marshaller found for mime-type: " + mimeType);
		}

		return result;
	}


	/**
	 * Processes the {@link MethodCall} serialized in the input-stream.
	 *
	 * @param uri the URI where the service is registered
	 * @param mimeType the mime-type of the content in the input-stream
	 * @param inputStream the input-stream containing the serialized {@link MethodCall}
	 * @throws ServiceException
	 */
	protected Serializable process(final String uri, final String mimeType, final InputStream inputStream) throws ServiceException {
		try {
			final IMarshaller marshaller = MarshallerFactory.get(mimeType);

			if (marshaller == null) {
				LOG.error("Could not find marshaller for content-type " + mimeType);
				throw new ServiceException("Could not find marshaller for content-type " + mimeType);
			}

			// check if its a service call
			final ServiceContainer serviceContainer = serviceUriMap.get(uri);

			// if a service was found
			if (serviceContainer == null) {
				LOG.error("Could not find service for URI " + uri);
				throw new ServiceException("Could not find service for URI " + uri);
			}

			LOG.info("Found service '" + serviceContainer.getServiceDescriptor() + "' to process request!");

			// call the service method
			final MethodCall call = marshaller.deserialize(inputStream, MethodCall.class);

			return process(serviceContainer, call);
		} catch (final Throwable e) {
			LOG.error("Error calling service method!", e);
			throw new ServiceException("Error calling service method!", e);
		}

	}


	/**
	 * Processes the {@link MethodCall} on the specified {@link Service}.
	 *
	 * @param service the {@link Service}
	 * @param call the {@link MethodCall}
	 * @throws ServiceException
	 */
	private Serializable process(final ServiceContainer serviceContainer, final MethodCall call) throws Throwable {
		LOG.info("Calling method: " + call + " on service: " + serviceContainer.getServiceDescriptor());
		final Object service = serviceContainer.getService();

		final Serializable result = (Serializable) service.getClass().getMethod("invoke", byte[].class).invoke(service, SerializationUtils.serialize(call));

		LOG.info("Method: " + call + " on service: " + serviceContainer.getServiceDescriptor() + " returned: " + result);

		return result;
	}


	@PreDestroy
	@Override
	public synchronized void close() {
		for (final ServiceContainer serviceContainer : new ArrayList<ServiceContainer>(serviceUriMap.values())) {
			final ServiceDescriptor serviceDescriptor = serviceContainer.getServiceDescriptor();

			try {
				removeService(serviceDescriptor.getId(), serviceDescriptor.getVersion());
			} catch (final ServiceException e) {
				LOG.error("Could not remove service: " + serviceDescriptor + " on closing the server!", e);
			}
		}
	}


	/**
	 * Gets the list of {@link ServiceDescriptor}s which this service-server services.
	 *
	 * @return the list of {@link ServiceDescriptor}s
	 */
	public List<ServiceDescriptor> getServiceDescriptors() {
		return Collections.unmodifiableList(serviceUriMap.values().stream().map(container -> container.getServiceDescriptor()).collect(Collectors.toList()));
	}


	/**
	 * This method should be called by a monitoring service to check whether this service-server is
	 * still running.
	 *
	 * @return OK response
	 */
	public abstract <T> T heartbeat();

}
