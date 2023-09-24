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
import java.util.List;

/**
 * A desciptor describing a registry-server.
 *
 * @author Torsten Oltmanns
 *
 */
public class RegistryDescriptor extends AbstractDescriptor implements Serializable {
	private static final long serialVersionUID = 4118584459832233062L;
	private final WebCall registerUri;
	private final WebCall unregisterUri;
	private final WebCall serviceUri;
	private final WebCall allServicesUri;
	private final WebCall mediaTypesUri;
	private final WebCall addRegistryUri;
	private final WebCall removeRegistryUri;


	/**
	 * Creates a {@link RegistryDescriptor}.
	 *
	 * @param host the host where the registry server resides
	 * @param port the port
	 * @param useSSL flag, whether the registry server requires SSL connections
	 * @param contextRoot the context-root of the registry server
	 * @return
	 */
	public static RegistryDescriptor create(final String host, final int port, final boolean useSSL, final String contextRoot, final List<String> supportedMimeTypes) {
		final WebCall registerUri = WebCall.create(contextRoot + "/registry/register", "POST");
		final WebCall unregisterUri = WebCall.create(contextRoot + "/registry/unregister", "POST");
		final WebCall serviceUri = WebCall.create(contextRoot + "/registry/get", "GET");
		final WebCall allServicesUri = WebCall.create(contextRoot + "/registry/getall", "GET");
		final WebCall heartbeatUri = WebCall.create(contextRoot + "/registry/heartbeat", "GET");
		final WebCall addRegistryUri = WebCall.create(contextRoot + "/registry/addregistry", "POST");
		final WebCall removeRegistryUri = WebCall.create(contextRoot + "/registry/removeregistry", "POST");
		final WebCall mediaTypesUri = WebCall.create(contextRoot + "/registry/mediatypes", "GET");

		return new RegistryDescriptor(host, port, useSSL, registerUri, unregisterUri, serviceUri, allServicesUri, heartbeatUri, mediaTypesUri, addRegistryUri, removeRegistryUri, supportedMimeTypes);
	}


	/**
	 * Constructor.
	 *
	 * @param host the host where the registry server resides
	 * @param port the port
	 * @param useSSL flag, whether the registry server requires SSL connections
	 * @param contextRoot the context-root of the registry server
	 */
	private RegistryDescriptor(final String host, final int port, final boolean useSSL, final WebCall registerUri, final WebCall unregisterUri, final WebCall serviceUri, final WebCall allServicesUri, final WebCall heartBeatUri, final WebCall mediaTypesUri, final WebCall addRegistryUri, final WebCall removeRegistryUri, final List<String> supportedMimeTypes) {
		setHost(host);
		setPort(port);
		setUseSSL(useSSL);
		this.registerUri = registerUri;
		this.unregisterUri = unregisterUri;
		this.serviceUri = serviceUri;
		this.allServicesUri = allServicesUri;
		setHeartbeatUri(heartBeatUri);
		this.mediaTypesUri = mediaTypesUri;
		this.addRegistryUri = addRegistryUri;
		this.removeRegistryUri = removeRegistryUri;
		setSupportedMimeTypes(supportedMimeTypes);
	}


	/**
	 * @return the registerUri
	 */
	public WebCall getRegisterUri() {
		return registerUri;
	}


	/**
	 * @return the unregisterUri
	 */
	public WebCall getUnregisterUri() {
		return unregisterUri;
	}


	/**
	 * @return the serviceUri
	 */
	public WebCall getServiceUri() {
		return serviceUri;
	}


	/**
	 * @return the allServicesUri
	 */
	public WebCall getAllServicesUri() {
		return allServicesUri;
	}


	/**
	 * @return the mediaTypesUri
	 */
	public WebCall getMediaTypesUri() {
		return mediaTypesUri;
	}


	public WebCall getAddRegistryUri() {
		return addRegistryUri;
	}


	public WebCall getRemoveRegistryUri() {
		return removeRegistryUri;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((addRegistryUri == null) ? 0 : addRegistryUri.hashCode());
		result = prime * result + ((allServicesUri == null) ? 0 : allServicesUri.hashCode());
		result = prime * result + ((mediaTypesUri == null) ? 0 : mediaTypesUri.hashCode());
		result = prime * result + ((registerUri == null) ? 0 : registerUri.hashCode());
		result = prime * result + ((removeRegistryUri == null) ? 0 : removeRegistryUri.hashCode());
		result = prime * result + ((serviceUri == null) ? 0 : serviceUri.hashCode());
		result = prime * result + ((unregisterUri == null) ? 0 : unregisterUri.hashCode());
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
		final RegistryDescriptor other = (RegistryDescriptor) obj;
		if (addRegistryUri == null) {
			if (other.addRegistryUri != null) {
				return false;
			}
		} else if (!addRegistryUri.equals(other.addRegistryUri)) {
			return false;
		}
		if (allServicesUri == null) {
			if (other.allServicesUri != null) {
				return false;
			}
		} else if (!allServicesUri.equals(other.allServicesUri)) {
			return false;
		}
		if (mediaTypesUri == null) {
			if (other.mediaTypesUri != null) {
				return false;
			}
		} else if (!mediaTypesUri.equals(other.mediaTypesUri)) {
			return false;
		}
		if (registerUri == null) {
			if (other.registerUri != null) {
				return false;
			}
		} else if (!registerUri.equals(other.registerUri)) {
			return false;
		}
		if (removeRegistryUri == null) {
			if (other.removeRegistryUri != null) {
				return false;
			}
		} else if (!removeRegistryUri.equals(other.removeRegistryUri)) {
			return false;
		}
		if (serviceUri == null) {
			if (other.serviceUri != null) {
				return false;
			}
		} else if (!serviceUri.equals(other.serviceUri)) {
			return false;
		}
		if (unregisterUri == null) {
			if (other.unregisterUri != null) {
				return false;
			}
		} else if (!unregisterUri.equals(other.unregisterUri)) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return "RegistryDescriptor [registerUri=" + registerUri + ", unregisterUri=" + unregisterUri + ", serviceUri=" + serviceUri + ", allServicesUri=" + allServicesUri + ", mediaTypesUri=" + mediaTypesUri + ", addRegistryUri=" + addRegistryUri + ", removeRegistryUri=" + removeRegistryUri + ", getHeartbeatUri()=" + getHeartbeatUri() + ", getHost()=" + getHost() + ", getPort()=" + getPort()
				+ ", getSupportedMimeTypes()=" + getSupportedMimeTypes() + ", isUseSSL()=" + isUseSSL() + "]";
	}

}
