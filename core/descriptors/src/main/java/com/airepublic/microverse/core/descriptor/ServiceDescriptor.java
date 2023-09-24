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
package com.airepublic.microverse.core.descriptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Class to build a Service Descriptor from a service class.
 *
 * @author Torsten Oltmanns
 *
 */
public class ServiceDescriptor extends AbstractDescriptor implements Serializable {
	private static final long serialVersionUID = -617012619515535775L;

	private String id;
	private String version;
	private WebCall serviceUri;
	private String serviceClass;
	private String serviceInterface;

	private final List<MethodDescriptor> methods = new ArrayList<>();


	/**
	 * Creates a {@link ServiceDescriptor} for the specified service-class using the
	 * service-interface for the client->server communication.
	 *
	 * @param serviceClass the service-class
	 * @param serviceInterface the service-interface
	 * @return the {@link ServiceDescriptor}
	 * @throws ServiceException
	 */
	public static ServiceDescriptor create(final String id, final String version, final String serviceClass, final String serviceInterface) {
		final ServiceDescriptor sd = new ServiceDescriptor();
		sd.id = id;
		sd.version = version;
		sd.serviceClass = serviceClass;
		sd.serviceInterface = serviceInterface;

		return sd;
	}


	/**
	 * Constructor.
	 */
	private ServiceDescriptor() {

	}


	public String getId() {
		return id;
	}


	/**
	 * @return the serviceClass
	 */
	public String getServiceClass() {
		return serviceClass;
	}


	public final String getServiceInterface() {
		return serviceInterface;
	}


	public final void addMethod(final MethodDescriptor method) {
		methods.add(method);
	}


	public final List<MethodDescriptor> getMethods() {
		return methods;
	}


	public final WebCall getServiceUri() {
		return serviceUri;
	}


	public final void setServiceUri(final WebCall uri) {
		serviceUri = uri;
	}


	public String getVersion() {
		return version;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((serviceClass == null) ? 0 : serviceClass.hashCode());
		result = prime * result + ((serviceInterface == null) ? 0 : serviceInterface.hashCode());
		result = prime * result + ((serviceUri == null) ? 0 : serviceUri.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}


	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ServiceDescriptor other = (ServiceDescriptor) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (methods == null) {
			if (other.methods != null) {
				return false;
			}
		} else if (!methods.equals(other.methods)) {
			return false;
		}
		if (serviceClass == null) {
			if (other.serviceClass != null) {
				return false;
			}
		} else if (!serviceClass.equals(other.serviceClass)) {
			return false;
		}
		if (serviceInterface == null) {
			if (other.serviceInterface != null) {
				return false;
			}
		} else if (!serviceInterface.equals(other.serviceInterface)) {
			return false;
		}
		if (serviceUri == null) {
			if (other.serviceUri != null) {
				return false;
			}
		} else if (!serviceUri.equals(other.serviceUri)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return "ServiceDescriptor [id=" + id + ", version=" + version + ", serviceUri=" + serviceUri + ", serviceClass=" + serviceClass + ", serviceInterface=" + serviceInterface + ", methods=" + methods + "]";
	}

}
