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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.MethodCall;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * {@link InvocationHandler} to proxy service calls.
 *
 * @author Torsten Oltmanns
 *
 */
public class ServiceClientInvocationHandler implements InvocationHandler {
	private final static Logger LOG = LoggerFactory.getLogger(ServiceClientInvocationHandler.class);
	private final ServiceDescriptor serviceDescriptor;


	/**
	 * Constructor.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor} of the service to proxy
	 * @param marshaller the {@link IMarshaller} used to serialize request content
	 */
	public ServiceClientInvocationHandler(final ServiceDescriptor serviceDescriptor, final IMarshaller marshaller) {
		this.serviceDescriptor = serviceDescriptor;
	}


	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		// create a method call
		final MethodCall call = MethodCall.create(method.getName(), args);

		// send the request to the remote service
		final HttpResponse response = ServiceUtils.executeRequest(serviceDescriptor, serviceDescriptor.getServiceUri(), call);

		if (response.getEntity() != null && response.getEntity().getContentLength() > 0 && response.getHeaders(HttpHeaders.CONTENT_TYPE) != null && response.getHeaders(HttpHeaders.CONTENT_TYPE).length > 0) {
			// deserialize with the marshaller for the response content-type
			final String mimeType = ServiceUtils.getMimeTypeFromResponse(response);
			final IMarshaller responseMarshaller = MarshallerFactory.get(mimeType);

			if (responseMarshaller == null) {
				LOG.error("Could not find a marshaller for the response content-type: " + mimeType);
				throw new ServiceException("Could not find a marshaller for the response content-type: " + mimeType);
			}

			return responseMarshaller.deserialize(response.getEntity().getContent(), method.getReturnType());
		}

		return null;
	}

}
