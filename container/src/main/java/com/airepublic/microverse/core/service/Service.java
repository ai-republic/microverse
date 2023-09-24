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
package com.airepublic.microverse.core.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.SerializationUtils;
import org.jboss.weld.environment.se.Weld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.descriptor.MethodCall;
import com.airepublic.microverse.core.descriptor.MethodDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * The service class to connect with.
 *
 * @author Torsten Oltmanns TODO Create own ServiceContainer using own classloader and base-install
 *         package with weld,etc.
 */
public class Service implements Serializable, Closeable {
	private static final long serialVersionUID = 4100978577535571632L;
	private static Logger LOG = LoggerFactory.getLogger(Service.class);
	private ServiceDescriptor serviceDescriptor;
	private Object serviceDelegate;
	private Weld weld;


	/**
	 * Constructor.
	 *
	 */
	public Service() {
	}


	public Service(final byte[] serializedServiceDescriptor) throws ServiceException {
		try {
			serviceDescriptor = SerializationUtils.deserialize(serializedServiceDescriptor);
			final Class<?> serviceInterface = Class.forName(serviceDescriptor.getServiceInterface());

			if (!serviceInterface.isInterface()) {
				LOG.error("The service-interface is a class. It must be an interface to build a " + ServiceDescriptor.class.getSimpleName());
				throw new ServiceException("The service-class is an interface. It must be a class to build a " + ServiceDescriptor.class.getSimpleName());
			}

			for (final Method method : serviceInterface.getMethods()) {
				serviceDescriptor.addMethod(MethodDescriptor.create(method));
			}

			final Class<?> serviceClass = getClass().getClassLoader().loadClass(serviceDescriptor.getServiceClass());
			weld = new Weld(serviceDescriptor.getServiceClass() + "#" + System.currentTimeMillis());
			weld.setClassLoader(getClass().getClassLoader());

			LOG.info("Creating service-class: " + serviceClass.getName());
			serviceDelegate = weld.initialize().select(serviceClass).get();
		} catch (final Exception e) {
			throw new ServiceException("Could not create service-class for service-descriptor: " + serviceDescriptor + ". Make sure the service-class has a default constructor and implements the service-interface.", e);
		}
	}


	/**
	 * Gets the {@link ServiceDescriptor}.
	 *
	 * @return the {@link ServiceDescriptor}
	 */
	public ServiceDescriptor getServiceDescriptor() {
		return serviceDescriptor;
	}


	/**
	 * Gets the serialized {@link ServiceDescriptor}.
	 *
	 * @return the serialized {@link ServiceDescriptor}
	 */
	public byte[] getSerializedServiceDescriptor() {
		return SerializationUtils.serialize(serviceDescriptor);
	}


	public Serializable invoke(final byte[] methodCall) throws ServiceException, Throwable {
		final MethodCall call = SerializationUtils.deserialize(methodCall);
		return invoke(call);
	}


	/**
	 * Invokes the specified method on the delegate service-class and returns the result.
	 *
	 * @param call the {@link MethodCall}
	 * @return the result (which must be {@link Serializable}
	 * @throws Exception
	 */
	public Serializable invoke(final MethodCall call) throws ServiceException, Throwable {
		LOG.debug("Method " + call + " invoked");
		Class<?>[] parameterTypes;
		Object[] parameters;

		// check if parameters need to be set
		if (call.getParameters() != null) {
			parameterTypes = new Class[call.getParameters().size()];
			parameters = new Object[call.getParameters().size()];

			for (int i = 0; i < call.getParameters().size(); i++) {
				final Object obj = SerializationUtils.deserialize(call.getParameters().get(i));
				parameterTypes[i] = obj.getClass();
				parameters[i] = obj;
			}
		} else {
			// otherwise empty parameter class array for the method invoke call
			parameterTypes = new Class[0];
			parameters = null;
		}

		Method method;

		try {
			// get the corresponding method of the service-delegate
			method = serviceDelegate.getClass().getMethod(call.getMethodName(), parameterTypes);
		} catch (final NoSuchMethodException e) {
			throw new ServiceException("The service-class '" + serviceDelegate.getClass().getName() + "' doesn't have an accessible method: " + call, e);
		}

		try {
			// invoke the method with the specified parameters
			return (Serializable) method.invoke(serviceDelegate, parameters);
		} catch (final IllegalAccessException e) {
			throw new ServiceException("The service-class '" + serviceDelegate.getClass().getName() + "' doesn't have an accessible method: " + call, e);
		} catch (final InvocationTargetException e) {
			throw new ServiceException("The service-class '" + serviceDelegate.getClass().getName() + "' doesn't have an accessible method: " + call, e);
		} catch (final Throwable t) {
			throw t;
		}
	}


	@Override
	public void close() throws IOException {
		serviceDelegate = null;
		serviceDescriptor = null;

		try {
			weld.shutdown();
			weld.resetAll();
		} catch (final Exception e) {
			LOG.error("Error shutting down CDI-Container for service: " + serviceDescriptor, e);
		}

		LOG = null;
		weld = null;
	}
}
