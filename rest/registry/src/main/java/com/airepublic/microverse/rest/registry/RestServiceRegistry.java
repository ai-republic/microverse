/**
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
package com.airepublic.microverse.rest.registry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.microverse.core.registry.ServiceRegistry;

/**
 * Service registry for the service platform.<br/>
 * IMPORTANT: for the discovery to work the 'microverse.registry.host', 'microverse.registry.port'
 * and 'microverse.registry.useSSL' system-properties must be set!
 *
 * @author Torsten Oltmanns
 *
 */
@Singleton
@Path("/")
public class RestServiceRegistry extends ServiceRegistry {
	private final static Logger LOG = LoggerFactory.getLogger(RestServiceRegistry.class);


	@PostConstruct
	public void init() {
		String host;
		int port;
		boolean useSSL;
		String contextRoot;
		try {
			host = Configuration.getRegistryHost();
			port = Configuration.getRegistryPort();
			useSSL = Configuration.getRegistryUseSSL();
			contextRoot = Configuration.getRegistryContextroot();

			if (contextRoot.charAt(0) != '/') {
				contextRoot = "/" + contextRoot;
			}
		} catch (final Exception e) {
			LOG.error("System properties not correctly configured! Make sure you have set the following:\n-Dmicroverse.registry.host=<hostname> -Dmicroverse.registry.port=<port> -Dmicroverse.registry.useSSL=<boolean> -Dmicroverse.registry.contextroot=<context root of registry-webapp>");
			throw new RuntimeException("System properties not correctly configured! Make sure you have set the following:\n-Dmicroverse.registry.host=<hostname> -Dmicroverse.registry.port=<port> -Dmicroverse.registry.useSSL=<boolean> -Dmicroverse.registry.contextroot=<context root of registry-webapp>");
		}

		final RegistryDescriptor registryDescriptor = RegistryDescriptor.create(host, port, useSSL, contextRoot, MarshallerFactory.getSupportedMimeTypes());
		try {
			start(registryDescriptor);
		} catch (final Exception e) {
			LOG.error("Could not announce new registry on: " + registryDescriptor.getHost() + ":" + registryDescriptor.getPort() + " (SSL=" + registryDescriptor.isUseSSL() + ") via service-discovery!", e);
		}

		LOG.info("----------- Service-Registry has initialized! -----------");
	}


	@Path("mediatypes")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getSupportedMediaTypes() {
		// generate a list of supported acceptable mime-types to set as
		// accept-header
		final StringBuffer mimeTypes = new StringBuffer();

		for (final String mimeType : MarshallerFactory.getSupportedMimeTypes()) {
			if (mimeTypes.length() > 0) {
				mimeTypes.append(",");
			}

			mimeTypes.append(mimeType);
		}

		return Response.ok().entity(mimeTypes.toString()).build();
	}


	@Path("addregistry")
	@POST
	public Response addRegistryRequest(@Context final HttpHeaders headers, final InputStream inputStream) throws ServiceException {
		try {
			final RegistryDescriptor registryDescriptor = unmarshalRegistryDescriptor(headers, inputStream);

			// if no marshaller was found return error
			if (registryDescriptor == null) {
				LOG.error("Adding service-registry failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
				return Response.status(Status.BAD_REQUEST).entity("Adding service-registry failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();
			}

			addRegistry(registryDescriptor);

			// otherwise registration was successful - return ok
			return Response.ok().build();
		} catch (final Exception e) {
			LOG.error("Adding service-registry failed: " + e, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Adding service-registry failed: " + e).entity(e).build();
		}
	}


	@Path("removeregistry")
	@POST
	public Response removeRegistryRequest(@Context final HttpHeaders headers, final InputStream inputStream) throws ServiceException {
		try {
			final RegistryDescriptor registryDescriptor = unmarshalRegistryDescriptor(headers, inputStream);

			// if no marshaller was found return error
			if (registryDescriptor == null) {
				LOG.error("Removing service-registry failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
				return Response.status(Status.BAD_REQUEST).entity("Removing service-registry failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();
			}

			removeRegistry(registryDescriptor);

			// otherwise registration was successful - return ok
			return Response.ok().build();
		} catch (final Exception e) {
			LOG.error("Removing service-registry failed: " + e, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Removing service-registry failed: " + e).entity(e).build();
		}
	}


	/**
	 * Register a {@link ServiceDescriptor}.
	 *
	 * @param serviceDescriptor the descriptor of the service to register
	 * @return the status OK if registration was successful, otherwise status BAD_REQUEST
	 */
	@Path("register")
	@POST
	public Response registerRequest(@Context final HttpHeaders headers, final InputStream is) {
		try {
			final ServiceDescriptor serviceDescriptor = unmarshalServiceDescriptor(headers, is);
			LOG.info("Registering service: " + serviceDescriptor);

			// if no marshaller was found return error
			if (serviceDescriptor == null) {
				LOG.error("Registration failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
				return Response.status(Status.BAD_REQUEST).entity("Registration failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();
			}

			registerService(serviceDescriptor);

			LOG.info("Registration successful: " + serviceDescriptor);
			// otherwise registration was successful - return ok
			return Response.ok().build();
		} catch (final Exception e) {
			LOG.error("Registration failed: " + e, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Registration failed: " + e).entity(e).build();
		}
	}


	/**
	 * Unregister a {@link ServiceDescriptor}.
	 *
	 * @param serviceDescriptor the descriptor of the service to unregister
	 * @return the status OK if registration was successful, otherwise status BAD_REQUEST
	 * @throws ServiceException
	 */
	@Path("unregister")
	@POST
	public Response unregisterRequest(@Context final HttpHeaders headers, final InputStream is) throws ServiceException {
		final ServiceDescriptor serviceDescriptor = unmarshalServiceDescriptor(headers, is);

		// if no marshaller was found return error
		if (serviceDescriptor == null) {
			LOG.error("Unregistration failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unregistration failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();
		}

		unregisterService(serviceDescriptor);
		return Response.ok().build();
	}


	/**
	 * Unmarshal the {@link ServiceDescriptor} from the request input-stream.
	 *
	 * @param headers the request headers
	 * @param inputStream the {@link InputStream}
	 * @return the {@link ServiceDescriptor}
	 */
	protected ServiceDescriptor unmarshalServiceDescriptor(@Context final HttpHeaders headers, final InputStream inputStream) {
		// get mime-types of the request
		final MediaType mediaType = headers.getMediaType();
		ServiceDescriptor serviceDescriptor = null;

		// try to get a marshaller configured to handle the request content-type
		final String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();
		final IMarshaller marshaller = MarshallerFactory.get(mimeType);

		if (marshaller != null) {
			// deserialize
			try {
				serviceDescriptor = marshaller.deserialize(inputStream, ServiceDescriptor.class);
			} catch (final ServiceException e) {
			}
		}

		return serviceDescriptor;
	}


	/**
	 * Unmarshal the {@link RegistryDescriptor} from the request input-stream.
	 *
	 * @param headers the request headers
	 * @param inputStream the {@link InputStream}
	 * @return the {@link RegistryDescriptor}
	 */
	protected RegistryDescriptor unmarshalRegistryDescriptor(@Context final HttpHeaders headers, final InputStream inputStream) {
		// get mime-types of the request
		final MediaType mediaType = headers.getMediaType();
		RegistryDescriptor registryDescriptor = null;

		// try to get a marshaller configured to handle the request content-type
		final String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();
		final IMarshaller marshaller = MarshallerFactory.get(mimeType);

		if (marshaller != null) {
			// deserialize
			try {
				registryDescriptor = marshaller.deserialize(inputStream, RegistryDescriptor.class);
			} catch (final ServiceException e) {
			}
		}

		return registryDescriptor;
	}


	/**
	 * Gets a {@link ServiceDescriptor}.
	 *
	 * @param serviceId the id of the service
	 * @param serviceVersion the version of the service
	 * @return the status OK with the {@link ServiceDescriptor} as entity if lookup was successful,
	 *         otherwise status BAD_REQUEST
	 */
	@Path("get/{serviceId}/{serviceVersion}")
	@GET
	public Response getServiceDescriptorRequest(@Context final HttpHeaders headers, @PathParam("serviceId") final String serviceId, @PathParam("serviceVersion") final String serviceVersion) {
		final ServiceDescriptor serviceDescriptor = getServiceDescriptor(serviceId, serviceVersion);

		if (serviceDescriptor == null) {
			LOG.error("Getting ServiceDescriptor failed: could not find ServiceDescriptor for: " + serviceId + ":" + serviceVersion);
			return Response.status(Status.BAD_REQUEST).entity("Getting ServiceDescriptor failed: could not find ServiceDescriptor for: " + serviceId + ":" + serviceVersion).build();
		}

		// get mime-types of the request
		final List<MediaType> acceptableTypes = headers.getAcceptableMediaTypes();
		IMarshaller marshaller = null;
		MediaType matchMediaType = null;

		for (final MediaType mediaType : acceptableTypes) {
			marshaller = MarshallerFactory.get(mediaType.getType() + "/" + mediaType.getSubtype());

			if (marshaller != null) {
				matchMediaType = mediaType;
				break;
			}
		}

		if (marshaller == null) {
			LOG.error("Getting ServiceDescriptor failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
			return Response.status(Status.BAD_REQUEST).entity("Getting ServiceDescriptor failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();
		}

		try {
			return Response.ok(marshaller.serialize(serviceDescriptor), matchMediaType).build();
		} catch (final ServiceException e) {
			LOG.error("Getting ServiceDescriptor failed: could not marshall ServiceDescriptor with content-type: " + matchMediaType, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Getting ServiceDescriptor failed: could not marshall ServiceDescriptor with content-type: " + matchMediaType).build();
		}
	}


	/**
	 * Gets all {@link ServiceDescriptor}s.
	 *
	 * @param serviceClass the classname of the service
	 * @return the status OK with the {@link ServiceDescriptor} as entity if lookup was successful,
	 *         otherwise status BAD_REQUEST
	 */
	@Path("getall")
	@GET
	public Response getAllServiceDescriptorsRequest(@Context final HttpHeaders headers) {
		final ArrayList<ServiceDescriptor> serviceDescriptors = getAllServiceDescriptors();

		// get mime-types of the request
		final List<MediaType> acceptableTypes = headers.getAcceptableMediaTypes();
		IMarshaller marshaller = null;
		MediaType matchMediaType = null;

		for (final MediaType mediaType : acceptableTypes) {
			marshaller = MarshallerFactory.get(mediaType.getType() + "/" + mediaType.getSubtype());

			if (marshaller != null) {
				matchMediaType = mediaType;
				break;
			}
		}

		if (marshaller == null) {
			LOG.error("Getting ServiceDescriptor failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
			return Response.status(Status.BAD_REQUEST).entity("Getting ServiceDescriptor failed: could not find marshaller for content-type: " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();
		}

		try {
			return Response.ok(marshaller.serialize(serviceDescriptors), matchMediaType).build();
		} catch (final ServiceException e) {
			LOG.error("Getting ServiceDescriptor failed: could not marshall ServiceDescriptor with content-type: " + matchMediaType, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Getting ServiceDescriptor failed: could not marshall ServiceDescriptor with content-type: " + matchMediaType).build();
		}
	}


	/**
	 * This method should be called by a monitoring service to check whether this registry is still
	 * running.
	 *
	 * @return {@link Response} OK
	 */
	@Path("heartbeat")
	@GET
	public Response heartbeat() {
		return Response.ok().build();
	}
}
