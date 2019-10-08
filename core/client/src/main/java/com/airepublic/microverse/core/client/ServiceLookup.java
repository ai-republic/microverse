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
package com.airepublic.microverse.core.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.descriptor.WebCall;
import com.airepublic.microverse.core.discovery.RegistryDiscovererService;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Lookup class for a {@linkplain RestServiceRegistryBinary}.
 *
 * @author Torsten Oltmanns
 *
 */
@Singleton
public class ServiceLookup implements Serializable, Closeable {
	private static final long serialVersionUID = -5644675193469937011L;
	private final static Logger LOG = LoggerFactory.getLogger(ServiceLookup.class);


	/**
	 * Creates a new {@link ServiceLookup}.
	 *
	 * @throws ServiceException
	 */
	public static ServiceLookup create() throws ServiceException {
		return new ServiceLookup();
	}


	/**
	 * Constructor.
	 */
	private ServiceLookup() {
	}


	/**
	 * Register the specified {@link ServiceDescriptor}s with the registry servers.
	 *
	 * @param serviceDescriptors the {@link ServiceDescriptor}s
	 * @throws ServiceException
	 */
	public void registerServices(final List<ServiceDescriptor> serviceDescriptors) throws ServiceException {
		for (final ServiceDescriptor serviceDescriptor : serviceDescriptors) {
			registerService(serviceDescriptor);
		}
	}


	/**
	 * Register the specified {@link ServiceDescriptor} with the registry servers.
	 *
	 * @param serviceDescriptors the {@link ServiceDescriptor}
	 * @throws ServiceException
	 */
	public void registerService(final ServiceDescriptor serviceDescriptor) throws ServiceException {
		// notify all know registries about the new service
		for (final RegistryDescriptor registryDescriptor : RegistryDiscovererService.requestRegistries()) {
			try {
				ServiceUtils.executeRequest(registryDescriptor, registryDescriptor.getRegisterUri(), serviceDescriptor);
			} catch (final Exception e) {
				LOG.error("Unable to register: " + serviceDescriptor, e);
				throw new ServiceException("Unable to register: " + serviceDescriptor, e);
			}
		}
	}


	/**
	 * Register the specified {@link ServiceDescriptor}s with the registry-server.
	 *
	 * @param serviceDescriptors the {@link ServiceDescriptor}s
	 * @throws ServiceException
	 */
	public void unregisterServices(final List<ServiceDescriptor> serviceDescriptors) throws ServiceException {
		for (final ServiceDescriptor serviceDescriptor : serviceDescriptors) {
			unregisterService(serviceDescriptor);
		}
	}


	/**
	 * Register the specified {@link ServiceDescriptor} with the registry-server.
	 *
	 * @param serviceDescriptors the {@link ServiceDescriptor}
	 * @throws ServiceException
	 */
	public void unregisterService(final ServiceDescriptor serviceDescriptor) throws ServiceException {
		// notify all know registries about the removed service
		for (final RegistryDescriptor registryDescriptor : RegistryDiscovererService.requestRegistries()) {
			try {
				ServiceUtils.executeRequest(registryDescriptor, registryDescriptor.getUnregisterUri(), serviceDescriptor);
			} catch (final Exception e) {
				LOG.error("Unable to unregister: " + serviceDescriptor, e);
				throw new ServiceException("Unable to unregister: " + serviceDescriptor, e);
			}
		}
	}


	/**
	 * Gets a {@link ServiceDescriptor} for the specified service class.
	 *
	 * @param serviceId the service-class
	 * @return the service client
	 * @throws Exception
	 */
	public final ServiceDescriptor getServiceDescriptor(final String serviceId, final String serviceVersion) throws ServiceException {
		ServiceDescriptor serviceDescriptor = null;

		// query all know registries about the requested service
		for (final RegistryDescriptor registryDescriptor : RegistryDiscovererService.requestRegistries()) {
			try {

				// execute the request
				final HttpResponse response = ServiceUtils.executeRequest(registryDescriptor, WebCall.create(registryDescriptor.getServiceUri().getUri() + "/" + serviceId + "/" + serviceVersion, registryDescriptor.getServiceUri().getWebMethod()), null);

				// deserialize with the marshaller for the response content-type
				final String mimeType = ServiceUtils.getMimeTypeFromResponse(response);
				final IMarshaller responseMarshaller = MarshallerFactory.get(mimeType);

				serviceDescriptor = responseMarshaller.deserialize(response.getEntity().getContent(), ServiceDescriptor.class);

				// check if one was found
				if (serviceDescriptor != null) {
					// then stop searching the registries
					break;
				}
			} catch (final Exception e) {
				LOG.debug("Unable to find for service '" + serviceId + ":" + serviceVersion + "' on " + registryDescriptor, e);
			}
		}

		if (serviceDescriptor == null) {
			LOG.error("Unable to find the service '" + serviceId + ":" + serviceVersion + "' on any registries!");
			throw new ServiceException("Unable to find the service '" + serviceId + ":" + serviceVersion + "' on any registries!");
		}

		return serviceDescriptor;
	}


	/**
	 * Gets a service client for the specified service interface.
	 *
	 * @param serviceInterface the service-interface providing the service functionality
	 * @param serviceClass the underlying service-class for the service-interface
	 * @return the service client
	 * @throws Exception
	 */
	public final <T> T getServiceClient(final Class<T> serviceInterface, final String serviceId, final String serviceVersion) throws ServiceException {
		try {
			final ServiceDescriptor serviceDescriptor = getServiceDescriptor(serviceId, serviceVersion);
			return getServiceClient(serviceDescriptor);
		} catch (final Exception e) {
			LOG.error("Unable to create Service-Client for service '" + serviceId + ":" + serviceVersion + "' and service-interface '" + serviceInterface.getName(), e);
			throw new ServiceException("Unable to create Service-Client for service '" + serviceId + ":" + serviceVersion + "' and service-interface '" + serviceInterface.getName(), e);
		}

	}


	/**
	 * Gets a service client for the specified service interface.
	 *
	 * @param serviceInterface the service-interface providing the service functionality
	 * @param serviceClass the underlying service-class for the service-interface
	 * @return the service client
	 * @throws Exception
	 */
	public final <T> T getServiceClient(final ServiceDescriptor serviceDescriptor) throws ServiceException {
		try {
			return createServiceClient(serviceDescriptor);
		} catch (final Exception e) {
			LOG.error("Unable to create Service-Client for service " + serviceDescriptor, e);
			throw new ServiceException("Unable to create Service-Client for service '" + serviceDescriptor, e);
		}

	}


	/**
	 * Create a service client which proxies the calls to the service server.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor}
	 * @return the client to the server
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	protected final <T> T createServiceClient(final ServiceDescriptor serviceDescriptor) throws ServiceException {
		// find common mime-types
		final List<String> supportedMimeTypes = new ArrayList<>(serviceDescriptor.getSupportedMimeTypes());
		supportedMimeTypes.retainAll(MarshallerFactory.getSupportedMimeTypes());

		if (supportedMimeTypes.isEmpty()) {
			throw new ServiceException("No common mime-types for client and server! Server supports: " + serviceDescriptor.getSupportedMimeTypes() + ", client: " + MarshallerFactory.getSupportedMimeTypes());
		}

		try {
			// load the service-interface class to create a proxy for
			final Class<?> serviceInterface = Class.forName(serviceDescriptor.getServiceInterface());
			// get compatible marshaller to serialize the request content
			final IMarshaller marshaller = MarshallerFactory.get(supportedMimeTypes.get(0));

			return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { serviceInterface }, new ServiceClientInvocationHandler(serviceDescriptor, marshaller));
		} catch (final Exception e) {
			throw new ServiceException("Could not create service-client!", e);
		}
	}


	@Override
	public void close() throws IOException {
	}
}
