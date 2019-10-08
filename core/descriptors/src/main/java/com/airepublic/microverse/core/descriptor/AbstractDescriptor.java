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
package com.airepublic.microverse.core.descriptor;

import java.io.Serializable;
import java.util.List;

/**
 * Abstraction of a descriptor defining the host/post of a server as well as whether to use SSL, its
 * supported mime-types and a heartbeat URI.
 *
 * @author Torsten Oltmanns
 *
 */
public class AbstractDescriptor implements Serializable {
	private static final long serialVersionUID = 6380526588264570736L;
	private WebCall heartbeatUri;
	private String host;
	private int port;
	private List<String> supportedMimeTypes;
	private boolean useSSL;


	public final WebCall getHeartbeatUri() {
		return heartbeatUri;
	}


	public final String getHost() {
		return host;
	}


	public final int getPort() {
		return port;
	}


	public final List<String> getSupportedMimeTypes() {
		return supportedMimeTypes;
	}


	public final void setSupportedMimeTypes(final List<String> supportedMimeTypes) {
		this.supportedMimeTypes = supportedMimeTypes;
	}


	/**
	 * @return the useSSL
	 */
	public boolean isUseSSL() {
		return useSSL;
	}


	public final void setHeartbeatUri(final WebCall heartbeatUri) {
		this.heartbeatUri = heartbeatUri;
	}


	public final void setHost(final String host) {
		this.host = host;
	}


	public final void setPort(final int port) {
		this.port = port;
	}


	/**
	 * @param useSSL the useSSL to set
	 */
	public void setUseSSL(final boolean useSSL) {
		this.useSSL = useSSL;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heartbeatUri == null) ? 0 : heartbeatUri.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + ((supportedMimeTypes == null) ? 0 : supportedMimeTypes.hashCode());
		result = prime * result + (useSSL ? 1231 : 1237);
		return result;
	}


	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractDescriptor other = (AbstractDescriptor) obj;
		if (heartbeatUri == null) {
			if (other.heartbeatUri != null) {
				return false;
			}
		} else if (!heartbeatUri.equals(other.heartbeatUri)) {
			return false;
		}
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		if (useSSL != other.useSSL) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return "AbstractDescriptor [heartbeatUri=" + heartbeatUri + ", host=" + host + ", port=" + port + ", supportedMimeTypes=" + supportedMimeTypes + ", useSSL=" + useSSL + "]";
	}

}
