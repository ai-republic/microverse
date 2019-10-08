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
package com.airepublic.microverse.discovery.configured;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import com.airepublic.microverse.discovery.configured.RegistryEntry.RegistryEntryId;

/**
 * Entity to represent an entry of a RegistryServer in the table REGISTRIES.
 *
 * @author Torsten Oltmanns
 *
 */
@Entity
@Table(name = "REGISTRIES")
@IdClass(RegistryEntryId.class)
public class RegistryEntry {
	public static class RegistryEntryId implements Serializable {
		private static final long serialVersionUID = -7533892746563986436L;
		private String host;
		private int port;
		private boolean useSSL;
		private String contextRoot;


		public RegistryEntryId() {
		}


		public RegistryEntryId(final String host, final int port, final boolean useSSL, final String contextRoot) {
			this.host = host;
			this.port = port;
			this.useSSL = useSSL;
			this.contextRoot = contextRoot;
		}


		public String getHost() {
			return host;
		}


		public int getPort() {
			return port;
		}


		public boolean isUseSSL() {
			return useSSL;
		}


		public String getContextRoot() {
			return contextRoot;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((contextRoot == null) ? 0 : contextRoot.hashCode());
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + port;
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
			final RegistryEntryId other = (RegistryEntryId) obj;
			if (contextRoot == null) {
				if (other.contextRoot != null) {
					return false;
				}
			} else if (!contextRoot.equals(other.contextRoot)) {
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
	}

	@Id
	private String host;
	@Id
	private int port;
	@Id
	private boolean useSSL;
	@Id
	private String contextRoot;


	public RegistryEntry() {
	}


	public RegistryEntry(final String host, final int port, final boolean useSSL, final String contextRoot) {
		this.host = host;
		this.port = port;
		this.useSSL = useSSL;
		this.contextRoot = contextRoot;
	}


	public String getHost() {
		return host;
	}


	public void setHost(final String host) {
		this.host = host;
	}


	public int getPort() {
		return port;
	}


	public void setPort(final int port) {
		this.port = port;
	}


	public boolean isUseSSL() {
		return useSSL;
	}


	public void setUseSSL(final boolean useSSL) {
		this.useSSL = useSSL;
	}


	public String getContextRoot() {
		return contextRoot;
	}


	public void setContextRoot(final String contextRoot) {
		this.contextRoot = contextRoot;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contextRoot == null) ? 0 : contextRoot.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
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
		final RegistryEntry other = (RegistryEntry) obj;
		if (contextRoot == null) {
			if (other.contextRoot != null) {
				return false;
			}
		} else if (!contextRoot.equals(other.contextRoot)) {
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
		return "RegistryEntry [host=" + host + ", port=" + port + ", useSSL=" + useSSL + ", contextRoot=" + contextRoot + "]";
	}
}
