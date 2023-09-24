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
package com.airepublic.microverse.core.common;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.AbstractDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.descriptor.WebCall;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Common methods used by various classes.
 *
 * @author Torsten Oltmanns
 *
 */
public class ServiceUtils {
	public final static String MULTICAST_GROUP_ADDRESS = "224.0.0.0";
	public final static int MULTICAST_PORT = 5000;
	private final static Logger LOG = LoggerFactory.getLogger(ServiceUtils.class);


	/**
	 * Creates a comma-separated string of all mime-types supported by the {@link MarshallerFactory}
	 * .
	 *
	 * @return a comma-separated string of all supported mime-types
	 */
	public static String getAcceptedMimeTypes() {
		final StringBuffer mimeTypes = new StringBuffer();

		for (final String mimeType : MarshallerFactory.getSupportedMimeTypes()) {
			if (mimeTypes.length() > 0) {
				mimeTypes.append(",");
			}

			mimeTypes.append(mimeType);
		}

		return mimeTypes.toString();
	}


	/**
	 * Gets the mime-type from the request header 'accept' to determine the content-type of the
	 * response content.
	 *
	 * @param headers the request headers
	 * @return the response mime-type
	 */
	public static String getMimeTypeForResponse(final Header acceptHeader) {
		if (acceptHeader != null && acceptHeader.getElements().length > 0) {
			return acceptHeader.getElements()[0].getName();
		}

		return null;
	}


	/**
	 * Gets the mime-type from the response header 'content-type' to determine how to deserialize
	 * the response entity.
	 *
	 * @param response the response
	 * @return the mime-type
	 */
	public static String getMimeTypeFromResponse(final HttpResponse response) {
		return response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
	}


	/**
	 * Queries the supported mime-types from the REST server.
	 *
	 * @throws ServiceException
	 */
	public static List<String> getSupportedMediaTypesOfRestServer(final String host, final int port, final String mediaTypesQueryUri) throws ServiceException {
		try {
			final HttpClient httpClient = HttpClientBuilder.create().build();
			final HttpHost httpHost = new HttpHost(host, port);
			final HttpGet httpGet = new HttpGet(mediaTypesQueryUri);

			final HttpResponse httpResponse = httpClient.execute(httpHost, httpGet);
			validateResponse(httpGet, httpResponse);

			// read the comma-separated string from the response
			final String supportedMimeTypeString = IOUtils.toString(httpResponse.getEntity().getContent());

			// and convert it to a list
			final String[] mimeTypes = supportedMimeTypeString.trim().split(",");

			final List<String> supportedMimeTypes = new ArrayList<>();
			for (final String mimeType : mimeTypes) {
				supportedMimeTypes.add(mimeType.trim());
			}

			return supportedMimeTypes;
		} catch (final ServiceException e) {
			throw e;
		} catch (final Exception e) {
			LOG.error("Could not obtain supported media-types from the REST server!", e);
			throw new ServiceException("Could not obtain supported media-types from the REST server!", e);
		}
	}


	/**
	 * Gets a {@link IMarshaller} by querying the REST server's supported mime-types and trying to
	 * find a match by the clients supported mime-types.
	 *
	 * @param host the host of the REST server
	 * @param port the port of the REST server
	 * @param restServerUri the URI which returns the mime-types in text/plain
	 * @return the {@link IMarshaller}
	 * @throws ServiceException
	 */
	public static IMarshaller getMarshallerToCommunicateWithRestServer(final String host, final int port, final String restServerUri) throws ServiceException {
		// first determine which mime-types the rest server supports
		final List<String> registryServerMimeTypes = ServiceUtils.getSupportedMediaTypesOfRestServer(host, port, restServerUri);

		// try to get a matching marshaller for the supported mime-type
		IMarshaller marshaller = null;

		for (final String mimeType : registryServerMimeTypes) {
			marshaller = MarshallerFactory.get(mimeType);

			if (marshaller != null) {
				break;
			}
		}

		// check if a common mime-type was found
		if (marshaller == null) {
			throw new ServiceException("Could not find common mime-type to communicate with reqistry server. A marshaller for one of the following mime-types must be provided: " + registryServerMimeTypes);
		}

		return marshaller;
	}


	/**
	 * Validates the response if the status OK was returned, otherwise it will throw a
	 * {@link ServiceException}.
	 *
	 * @param httpResponse the response
	 * @throws ServiceException
	 */
	public static void validateResponse(final HttpRequest httpRequest, final HttpResponse httpResponse) throws ServiceException {
		if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			LOG.error("Error communicating with " + httpRequest.getRequestLine() + ": received HttpStatus: " + httpResponse.getStatusLine());
			throw new ServiceException("Error communicating with " + httpRequest.getRequestLine() + ": received HttpStatus: " + httpResponse.getStatusLine());
		}
	}


	/**
	 * Gets the fully qualified URL string to the service.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor}
	 * @return the URL string
	 */
	public static String getServiceURL(final ServiceDescriptor serviceDescriptor) {
		return buildURL(serviceDescriptor.getHost(), serviceDescriptor.getPort(), serviceDescriptor.isUseSSL(), serviceDescriptor.getServiceUri().getUri());
	}


	/**
	 * Gets the fully qualified URL string to the service.
	 *
	 * @param host the host
	 * @param port the port
	 * @param useSSL the flag whether to use SSL
	 * @param uri the uri to the resource
	 * @return the URL string
	 */
	public static String buildURL(final String host, final int port, final boolean useSSL, final String uri) {
		final StringBuffer buf = new StringBuffer();

		if (useSSL) {
			buf.append("https://");
		} else {
			buf.append("http://");
		}

		buf.append(host);
		buf.append(":");
		buf.append(port);

		if (!uri.startsWith("/")) {
			buf.append("/");
		}

		buf.append(uri);

		return buf.toString();
	}


	public static HttpResponse executeRequest(final AbstractDescriptor descriptor, final WebCall webCall, final Serializable entity) throws ServiceException {
		return executeRequest(descriptor.getHost(), descriptor.getPort(), descriptor.isUseSSL(), webCall, descriptor.getSupportedMimeTypes(), entity);
	}


	public static HttpResponse executeRequest(final String host, final int port, final boolean useSSL, final WebCall webCall, final List<String> remoteSupportedMimeTypes, final Serializable entity) throws ServiceException {
		HttpUriRequest request = null;
		IMarshaller marshaller = null;

		// determine the marshaller base on the specified remote mime-types
		if (remoteSupportedMimeTypes != null) {
			marshaller = determineCommonMimeType(remoteSupportedMimeTypes);
		} else if (webCall.getWebMethod().equalsIgnoreCase("POST")) {
			// if no mime-types were specified and its a POST request, then
			// throw exception - cannot serialize object without marshaller
			throw new ServiceException("Cannot call POST web-method without specifying the supported mime-types of the remote server!");
		}

		if (webCall.getWebMethod().equalsIgnoreCase("GET")) {
			request = new HttpGet(buildURL(host, port, useSSL, webCall.getUri()));
		} else if (webCall.getWebMethod().equalsIgnoreCase("POST")) {
			request = new HttpPost(buildURL(host, port, useSSL, webCall.getUri()));
			((HttpPost) request).setEntity(EntityBuilder.create().setBinary(marshaller.serialize(entity)).build());
		} else if (webCall.getWebMethod().equalsIgnoreCase("DELETE")) {
			request = new HttpGet(buildURL(host, port, useSSL, webCall.getUri()));
		} else {
			throw new ServiceException("Unsupported web-call web method: " + webCall);
		}

		if (marshaller != null) {
			request.addHeader(HttpHeaders.CONTENT_TYPE, marshaller.getMimeType());
		}

		request.addHeader(HttpHeaders.ACCEPT, getAcceptedMimeTypes());

		return executeRequest(request, useSSL);
	}


	/**
	 * Executes the specified request.
	 *
	 * @param request the request
	 * @param useSSL whether to use SSL for the connection
	 * @return the response
	 * @throws ServiceException
	 */
	public static HttpResponse executeRequest(final HttpUriRequest request, final boolean useSSL) throws ServiceException {
		// configure SSL security
		SSLContext sslContext = null;

		if (useSSL) {
			try {
				final URL url = ServiceUtils.class.getClassLoader().getResource("servicebroker.truststore");
				sslContext = SSLContexts.custom().loadTrustMaterial(url, "changeme".toCharArray(), new TrustSelfSignedStrategy()).build();
			} catch (final Exception e) {
				LOG.error("Unable to configure SSL context!", e);
				throw new IllegalArgumentException("Unable to configure SSL context!", e);
			}
		}

		try {
			final HttpClient client = HttpClientBuilder.create().setSSLContext(sslContext).build();
			request.addHeader(HttpHeaders.ACCEPT, getAcceptedMimeTypes());

			if (!request.containsHeader(HttpHeaders.CONTENT_TYPE)) {
				request.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
			}

			// send the request to the remote service
			LOG.debug("Execute http request: " + request);
			final long startTime = System.currentTimeMillis();
			final HttpResponse response = client.execute(request);
			LOG.debug("Executed http request: " + request + " in (" + (System.currentTimeMillis() - startTime) + "ms)");
			validateResponse(request, response);

			return response;
		} catch (final Exception e) {
			throw new ServiceException(e);
		}
	}


	/**
	 * Determine which mime-type and corresponding marshaller is mutually supported.
	 *
	 * @param remoteSupportedMimeTypes the mime-types supported by the remote server
	 * @return a {@link IMarshaller} or null
	 * @throws ServiceException
	 */

	public static IMarshaller determineCommonMimeType(final List<String> remoteSupportedMimeTypes) throws ServiceException {
		// determine which mime-type and corresponding marshaller is mutually
		// supported by this lookup and the
		// registry-server
		IMarshaller marshaller = null;

		for (final String mimeType : remoteSupportedMimeTypes) {
			marshaller = MarshallerFactory.get(mimeType);

			if (marshaller != null) {
				break;
			}
		}

		// check if a common mime-type was found
		if (marshaller == null) {
			throw new ServiceException("Could not find common mime-type to communicate with. A marshaller for one of the following mime-types must be provided: " + remoteSupportedMimeTypes);
		}

		return marshaller;
	}
}
