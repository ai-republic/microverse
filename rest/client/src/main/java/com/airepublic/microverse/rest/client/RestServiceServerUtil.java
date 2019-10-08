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
package com.airepublic.microverse.rest.client;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.HttpHeaders;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;

import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.exception.ServiceException;

public class RestServiceServerUtil {
	private final String host;
	private final int port;
	private final boolean useSSL;
	private final String contextRoot;


	private RestServiceServerUtil(final String host, final int port, final boolean useSSL, final String contextRoot) throws ServiceException {
		this.host = host;
		this.port = port;
		this.useSSL = useSSL;
		this.contextRoot = contextRoot;
	}


	/**
	 * Creates the utitlity class to connect and manage the RestServiceServer at the specified host, port and context-root.
	 *
	 * @param host the host of the RestServiceServer
	 * @param port the port of the RestServiceServer
	 * @param useSSL whether to use SSL to connect to the RestServiceServer
	 * @param contextRoot the context-root of the rest-server
	 * @throws ServiceException
	 */
	public static RestServiceServerUtil create(final String host, final int port, final boolean useSSL, final String contextRoot) throws ServiceException {
		return new RestServiceServerUtil(host, port, useSSL, contextRoot);
	}


	/**
	 * Add the service for the specified service-bundle to the RestServiceServer at the host and port.
	 *
	 * @param serviceBundlePath the path to the service-bundle
	 * @return the {@link ServiceDescriptor} filled with server parameters
	 * @throws ServiceException
	 */
	public RestServiceServerUtil addService(final Path serviceBundlePath) throws ServiceException {
		try {
			final HttpPost post = new HttpPost(ServiceUtils.buildURL(host, port, useSSL, contextRoot + "/server/add"));
			// post.addHeader(HttpHeaders.CONTENT_TYPE, marshaller.getMimeType());
			post.addHeader(HttpHeaders.ACCEPT, ServiceUtils.getAcceptedMimeTypes());
			post.setEntity(EntityBuilder.create().setBinary(Files.readAllBytes(serviceBundlePath)).build());

			ServiceUtils.executeRequest(post, useSSL);

			return this;
		} catch (final ServiceException e) {
			throw e;
		} catch (final Exception e) {
			throw new ServiceException("Could not add service-bundle " + serviceBundlePath, e);
		}
	}


	public void removeService(final String serviceId, final String serviceVersion) throws ServiceException {
		final HttpPost post = new HttpPost(ServiceUtils.buildURL(host, port, useSSL, contextRoot + "/server/remove/" + serviceId + "/" + serviceVersion));

		try {
			ServiceUtils.executeRequest(post, useSSL);
		} catch (final ServiceException e) {
			throw e;
		} catch (final Exception e) {
			throw new ServiceException("Could not remove service " + serviceId + ":" + serviceVersion, e);
		}
	}

}
