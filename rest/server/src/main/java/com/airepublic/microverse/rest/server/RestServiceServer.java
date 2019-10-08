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
package com.airepublic.microverse.rest.server;

import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.descriptor.WebCall;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.microverse.core.server.AbstractServiceServer;
import com.airepublic.microverse.core.server.IClassLoaderCreator;

/**
 * Base class for all Service-Servers which provides functionality manage the server and to handle
 * requests generically in separate threads.<br/>
 *
 * @author Torsten Oltmanns
 *
 */
@Singleton
@Path("/server")
public class RestServiceServer extends AbstractServiceServer {
	private final static Logger LOG = LoggerFactory.getLogger(RestServiceServer.class);
	private final IClassLoaderCreator classLoaderCreator = new RestClassLoaderCreator();
	private String pathToThisResource;
	private final Object pathToThisResourceSync = new Object();


	@Override
	@PostConstruct
	public void init() {
		super.init();
		LOG.info("----------- Service-Server has been initialized! -----------");

	}


	@Override
	protected IClassLoaderCreator getClassLoaderCreator() {
		return classLoaderCreator;
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


	/**
	 * Adds a service described by the {@link ServiceDescriptor} in the request content stream. If
	 * no mime-type is set in the {@link ServiceDescriptor} the mime-type of the request will be set
	 * as service mime-type.
	 *
	 * @param request the request
	 * @param headers the http-headers
	 * @param inputStream the request content input stream
	 * @return
	 */
	@Path("add")
	@POST
	public Response addService(@Context final UriInfo uriInfo, @Context final HttpServletRequest request, @Context final HttpHeaders headers, final InputStream inputStream) {
		try {
			synchronized (pathToThisResourceSync) {
				if (pathToThisResource == null) {
					// set uri relative to this server's context-root
					// first determine the path to this resource
					String pathToThisResource = uriInfo.getAbsolutePath().getRawPath().substring(0, uriInfo.getAbsolutePath().getRawPath().lastIndexOf('/'));

					// remove the leading '/' if it exists
					if (pathToThisResource.startsWith("/")) {
						pathToThisResource = pathToThisResource.substring(1);
					}

					this.pathToThisResource = pathToThisResource;
				}
			}

			final ServiceDescriptor serviceDescriptor = addService(request.getServerName(), request.getServerPort(), request.isSecure(), inputStream);

			return Response.ok().entity(MarshallerFactory.get(getMimeTypeForResponse(headers)).serialize(serviceDescriptor)).build();
		} catch (final Exception e) {
			LOG.error("Could not add service!", e);
			return Response.status(Status.BAD_REQUEST).entity("Could not add service!" + e).build();
		}
	}


	@Override
	protected WebCall getServiceWebCall(final String serviceId, final String serviceVersion) {
		// build the URI to point to the process method
		return WebCall.create(pathToThisResource + "/process/" + serviceId + "/" + serviceVersion, "POST");
	}


	@Override
	protected WebCall getHeartbeatWebCall(final String serviceId, final String serviceVersion) {
		return WebCall.create(pathToThisResource + "/heartbeat", "GET");
	}


	/**
	 * Removes a service described by the {@link ServiceDescriptor} in the request content stream.
	 *
	 * @param headers the http-headers
	 * @param inputStream the request content input stream
	 * @return
	 */
	@Path("remove/{serviceId}/{serviceVersion : .+}")
	@POST
	public Response removeService(@Context final HttpHeaders headers, final @PathParam("serviceId") String serviceId, final @PathParam("serviceVersion") String serviceVersion) {
		try {
			if (headers.getMediaType() == null) {
				LOG.error("No content-type specified in request!");
				return Response.status(Status.BAD_REQUEST).entity("No content-type specified in request!").build();
			}

			removeService(serviceId, serviceVersion);
			return Response.ok().build();
		} catch (final Exception e) {
			LOG.error("Could not remove service: " + serviceId + ":" + serviceVersion, e);
			return Response.status(Status.BAD_REQUEST).entity("Could not remove service: " + serviceId + ":" + serviceVersion + "\n" + e).build();
		}
	}


	@Override
	protected void onServiceRegistration(final ServiceDescriptor serviceDescriptor) {
	}


	@Override
	protected void onServiceUnregistration(final ServiceDescriptor serviceDescriptor) {
	}


	/**
	 * Gets the mime-type from the request header 'content-type' to determine the mime-type of the
	 * request content.
	 *
	 * @param headers the request headers
	 * @return the mime-type
	 */
	protected String getMimeTypeFromRequest(final HttpHeaders headers) {
		return headers.getMediaType().getType() + "/" + headers.getMediaType().getSubtype();
	}


	/**
	 * Gets the mime-type from the request header 'accept' to determine the content-type of the
	 * response content.
	 *
	 * @param headers the request headers
	 * @return the response mime-type
	 */
	protected String getMimeTypeForResponse(final HttpHeaders headers) {
		if (headers.getAcceptableMediaTypes() != null && !headers.getAcceptableMediaTypes().isEmpty()) {
			final MediaType accept = headers.getAcceptableMediaTypes().get(0);

			return accept.getType() + "/" + accept.getSubtype();
		}

		return null;
	}


	/**
	 * Unmarshal the {@link ServiceDescriptor} from the request input-stream.
	 *
	 * @param headers the request headers
	 * @param inputStream the {@link InputStream}
	 * @return the {@link ServiceDescriptor}
	 */
	protected ServiceDescriptor unmarshalServiceDescriptor(final HttpHeaders headers, final InputStream inputStream) {
		ServiceDescriptor serviceDescriptor = null;
		// get mime-types of the request
		final String mimeType = getMimeTypeFromRequest(headers);

		// try to get a marshaller configured to handle the request content-type
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
	 * Processes the request and answers to the response using the context information.
	 *
	 * @param request the request
	 * @param response the response
	 * @param context the context
	 */
	@Path("process/{serviceClass : .+}")
	@POST
	public Response process(@Context final HttpHeaders headers, final InputStream inputStream, @Context final UriInfo uriInfo) {
		try {
			// determie the requested service by its URI
			final String uri = uriInfo.getAbsolutePath().getPath();
			// get mime-types of the request
			if (headers.getMediaType() == null) {
				LOG.error("No content-type specified in request!");
				return Response.status(Status.BAD_REQUEST).entity("No content-type specified in request!").build();
			}

			final String mimeType = getMimeTypeFromRequest(headers);

			final Serializable result = process(uri, mimeType, inputStream);

			// and write the result (if one was returned)
			if (result != null) {
				// use the accept header to determine marshaller for
				// serialization
				final String returnMimeType = getMimeTypeForResponse(headers);

				if (returnMimeType == null) {
					LOG.error("No accept header found in request!");
					return Response.status(Status.BAD_REQUEST).entity("No accept header found in request!").build();
				}

				final IMarshaller marshaller = MarshallerFactory.get(returnMimeType);

				if (marshaller == null) {
					LOG.error("No marshaller found for mime-type: " + mimeType);
					return Response.status(Status.BAD_REQUEST).entity("No marshaller to serialize respone found for mime-type: " + mimeType).build();
				}

				return Response.ok().header(HttpHeaders.CONTENT_TYPE, returnMimeType).entity(marshaller.serialize(result)).build();
			}

			return Response.ok().build();
		} catch (final Throwable e) {
			LOG.error("Error calling service method!", e);
			return Response.status(Status.BAD_REQUEST).entity("Error calling service method!").build();
		}
	}


	/**
	 * This method should be called by a monitoring service to check whether this service-server is
	 * still running.
	 *
	 * @return {@link Response} OK
	 */
	@Override
	@SuppressWarnings("unchecked")
	@Path("heartbeat")
	@GET
	public Response heartbeat() {
		return Response.ok().build();
	}

}
