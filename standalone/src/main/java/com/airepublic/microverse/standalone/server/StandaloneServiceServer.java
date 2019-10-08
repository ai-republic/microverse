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
package com.airepublic.microverse.standalone.server;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.nio.protocol.BasicAsyncRequestHandler;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.ServiceCreateDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceCreateDescriptorList;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.descriptor.WebCall;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.microverse.core.server.AbstractServiceServer;
import com.airepublic.microverse.core.server.IClassLoaderCreator;

/**
 * Base class for all Service-Servers which provides functionality manage the server and to handle
 * requests generically in separate threads.
 *
 * @author Torsten Oltmanns
 *
 */
public class StandaloneServiceServer extends AbstractServiceServer {
	private final static Logger LOG = LoggerFactory.getLogger(StandaloneServiceServer.class);
	private final IClassLoaderCreator classLoaderCreator = new StandaloneClassLoaderCreator();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private int port;
	private boolean useSSL;
	private HttpServer httpServer;
	private final UriHttpAsyncRequestHandlerMapper requestHandlerMapper = new UriHttpAsyncRequestHandlerMapper();
	private final WebCall heartbeatUri = WebCall.create("heartbeat", "GET");


	/**
	 * Create a {@link StandaloneServiceServer} for the specified {@link ServiceDescriptor
	 * ServiceDescriptors} on the specified port.
	 *
	 * @param port the server port
	 * @param useSSL flag to indicate whether to use SSL secured communication
	 * @return the {@link StandaloneServiceServer}
	 * @throws ServiceException
	 */
	public final static StandaloneServiceServer create(final int port, final boolean useSSL) throws ServiceException {
		System.setProperty(Configuration.SERVER_PORT, "" + port);
		System.setProperty(Configuration.SERVER_USESSL, "" + useSSL);
		final StandaloneServiceServer server = new StandaloneServiceServer();

		return server;
	}


	/**
	 * Constructor for CDI environments. Reads <code>microverse.server.port</code> and
	 * <code>microverse.server.useSSL</code> from system-properties.
	 */
	private StandaloneServiceServer() {
		try {
			final int port = Configuration.getServerPort();
			final boolean useSSL = Configuration.getServerUseSSL();
			initialize(port, useSSL);
		} catch (final Exception e) {
			LOG.error("Could not initialize standalone-service-server on port: " + port + ", useSSL: " + useSSL, e);
		}
	}


	public void initialize(final int port, final boolean useSSL) throws ServiceException {
		this.port = port;
		this.useSSL = useSSL;

		createHttpServer(port, useSSL);

		super.init();

		try {
			// start the server
			httpServer.start();
		} catch (final IOException e) {
			throw new ServiceException("Could not start standalone HTTP-Server!", e);
		}

		loadConfiguredServices();
	}


	/**
	 * Loads configured services from a directory found by the system property
	 * 'microverse.service.deploy.dir'. The directory must contain a file 'microverse.config'
	 * containing a serialized (json)list of {@link ServiceCreateDescriptor}. All configured
	 * service-bundles must be specified relatively to the config directory.
	 *
	 * @throws ServiceException
	 */
	protected void loadConfiguredServices() throws ServiceException {
		try {
			final String configDirStr = Configuration.getServiceDeployDir();

			if (configDirStr != null) {
				final java.nio.file.Path configDir = Paths.get(configDirStr);

				if (!Files.exists(configDir)) {
					LOG.warn("Could not auto-load services due to invalid configuration directory: " + configDir);
					return;
				}

				final java.nio.file.Path configFile = configDir.resolve(Configuration.getAutoDeployDeploymentConfigFilename());

				if (!Files.exists(configFile)) {
					LOG.warn("Could not auto-load services due to missing configuration file '" + Configuration.getAutoDeployDeploymentConfigFilename() + "' in: " + configDir);
					return;
				}

				final byte[] configBytes = Files.readAllBytes(configFile);
				final ServiceCreateDescriptorList descriptors = MarshallerFactory.get("application/json").deserialize(configBytes, ServiceCreateDescriptorList.class);

				deployConfiguredServices(configDir, descriptors);
			}
		} catch (final Exception e) {
			throw new ServiceException(e);
		}
	}


	/**
	 * Loads configured services from a directory found by the system property
	 * 'microservice.service.deploy.dir'. The directory must contain a file 'microverse.config'
	 * containing a serialized (json)list of {@link ServiceCreateDescriptor}. All configured
	 * service-bundles must be specified relatively to the config directory.
	 *
	 * @throws ServiceException
	 */
	protected void deployConfiguredServices(final Path configDir, final ServiceCreateDescriptorList descriptors) throws ServiceException {
		try {
			for (final ServiceCreateDescriptor descriptor : descriptors.getDescriptors()) {
				LOG.info("Deploying service: " + descriptor);
				addService(configDir.resolve(descriptor.getBundle()));
			}
		} catch (final Exception e) {
			throw new ServiceException(e);
		}
	}


	@Override
	protected IClassLoaderCreator getClassLoaderCreator() {
		return classLoaderCreator;
	}


	/**
	 * Create the {@link HttpServer} on the specified port.
	 *
	 * @param port the port to run the server on
	 * @param useSSL flag to indicate whether to use SSL secured communication
	 * @throws ServiceException
	 */
	protected void createHttpServer(final int port, final boolean useSSL) throws ServiceException {
		SSLContext sslContext = null;

		if (useSSL) {
			try {
				// initialize SSL context
				final URL url = getClass().getClassLoader().getResource("servicebroker.keystore");

				if (url != null) {
					sslContext = SSLContexts.custom().loadKeyMaterial(url, "changeme".toCharArray(), "changeme".toCharArray()).build();
				} else {
					StandaloneServiceServer.LOG.warn("Keystore file servicebroker.keystore not found");
				}
			} catch (final Exception e) {
				throw new ServiceException("Error initialzing SSL context!", e);
			}
		}

		try {
			// initialize the server configuration
			final ServerBootstrap bootstrap = ServerBootstrap.bootstrap().setListenerPort(port).setServerInfo("Test/1.1").setSslContext(sslContext).setExceptionLogger(msg -> LOG.error("Error:", msg));
			bootstrap.setHandlerMapper(requestHandlerMapper);

			requestHandlerMapper.register("/" + heartbeatUri.getUri(), new BasicAsyncRequestHandler((request, response, context) -> process(request, response, context)));
			// create the server
			httpServer = bootstrap.create();
		} catch (final Exception e) {
			throw new ServiceException("Error starting HTTP server on port " + port, e);
		}
	}


	@Override
	protected void onServiceRegistration(final ServiceDescriptor serviceDescriptor) {
		requestHandlerMapper.register("/" + serviceDescriptor.getServiceUri().getUri(), new BasicAsyncRequestHandler((request, response, context) -> process(request, response, context)));
	}


	@Override
	protected void onServiceUnregistration(final ServiceDescriptor serviceDescriptor) {
		requestHandlerMapper.unregister("/" + serviceDescriptor.getServiceUri());
	}


	/**
	 * Starts the service server.
	 *
	 * @throws IOException
	 */
	public synchronized void start() throws ServiceException {
		running.set(true);
		try {
			httpServer.start();

			for (final ServiceDescriptor serviceDescriptor : getServiceDescriptors()) {
				LOG.info("Service '" + serviceDescriptor.getServiceClass() + "' servicing on '" + serviceDescriptor.getHost() + ":" + serviceDescriptor.getPort() + "/" + serviceDescriptor.getServiceUri() + "' has started");
			}
		} catch (final IOException e) {
			StandaloneServiceServer.LOG.info("Service-Server could not be started on port: " + port + " using SSL=" + useSSL);
			throw new ServiceException("Service-Server final could not be final started on port: " + port + " final using SSL=" + useSSL, e);
		}
	}


	/**
	 * Restarts the service server.
	 *
	 * @throws Exception
	 */
	public synchronized void restart() throws ServiceException {
		if (running.get()) {
			shutDown();
			createHttpServer(port, useSSL);
			start();
		}
	}


	/**
	 * Adds a service which services the specified service-class via the service-interface.
	 *
	 * @param bundleZip the path to the service-bundle
	 * @throws ServiceException
	 */
	public void addService(final Path bundleZip) throws ServiceException {
		try {
			final String host = InetAddress.getLocalHost().getCanonicalHostName();
			addService(host, port, useSSL, Files.newInputStream(bundleZip));
		} catch (final UnknownHostException e) {
			throw new ServiceException("Localhost could not be resolved!", e);
		} catch (final IOException e) {
			throw new ServiceException("Service-bundle could not be read: " + bundleZip, e);
		}
	}


	@Override
	protected WebCall getServiceWebCall(final String serviceId, final String serviceVersion) {
		return WebCall.create(serviceId + "/" + serviceVersion, "POST");
	}


	@Override
	protected WebCall getHeartbeatWebCall(final String serviceId, final String serviceVersion) {
		return heartbeatUri;
	}


	/**
	 * Processes the request and answers to the response using the context information.
	 *
	 * @param request the request
	 * @param response the response
	 * @param context the context
	 */
	protected void process(final HttpRequest request, final HttpResponse response, final HttpContext context) {
		try {
			StandaloneServiceServer.LOG.debug("request: " + request.getRequestLine());

			// determie the requested service by its URI
			final String uri = request.getRequestLine().getUri();

			// check for heartbeat request
			if (uri.equals("/heartbeat")) {
				final Integer status = heartbeat();
				response.setStatusCode(status);
				return;
			}

			final Header contentTypeHeader = request.getFirstHeader(HttpHeaders.CONTENT_TYPE);

			if (contentTypeHeader == null) {
				LOG.error("No content-type header specified in request!");
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				response.setReasonPhrase("No content-type header specified in request!");
				return;
			}

			final Serializable result = process(uri, contentTypeHeader.getValue(), ((HttpEntityEnclosingRequest) request).getEntity().getContent());

			// and write the result (if one was returned)
			if (result != null) {
				// get accept header to determine how to serialize result
				final String mimeType = ServiceUtils.getMimeTypeForResponse(request.getFirstHeader(HttpHeaders.ACCEPT));

				if (mimeType == null) {
					LOG.error("No accept header specified in request!");
					response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
					response.setReasonPhrase("No accept header specified in request!");
					return;
				}

				response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
				response.setEntity(EntityBuilder.create().setBinary(MarshallerFactory.get(mimeType).serialize(result)).build());
			}

			response.setStatusCode(HttpStatus.SC_OK);
		} catch (final Throwable e) {
			LOG.error("Error calling service method!", e);
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			response.setReasonPhrase("Error calling service method!");
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public Integer heartbeat() {
		return HttpStatus.SC_OK;
	}


	/**
	 * Returns <code>true</code> if the server is running and has not been killed.
	 *
	 * @return <code>true</code> if the server is running and has not been killed
	 */
	public boolean isRunning() {
		return running.get();
	}


	/**
	 * Returns the flag whether the server is using SSL secured communiation.
	 *
	 * @return the useSSL flag
	 */
	public boolean isUseSSL() {
		return useSSL;
	}


	/**
	 * Stops the server and all open sub-processes and disconnects.
	 */
	public void shutDown() {
		try {
			close();
		} catch (final Exception e) {
			LOG.error("Error while shutting down: could not close all services cleanly!", e);
		}

		running.set(false);

		try {
			httpServer.shutdown(10, TimeUnit.SECONDS);
		} catch (final Exception e) {
			// shutdown quietly
		}
	}
}
