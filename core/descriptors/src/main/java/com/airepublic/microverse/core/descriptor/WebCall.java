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

/**
 * Defines the URI and web-method (GET, POST, DELETE, etc.) of a web-call.
 *
 * @author Torsten Oltmanns
 *
 */
public class WebCall implements Serializable {
	private static final long serialVersionUID = 6761096140664771548L;
	private String uri;
	private String webMethod;


	public static WebCall create(final String uri, final String webMethod) {
		final WebCall webCall = new WebCall();
		webCall.uri = uri;
		webCall.webMethod = webMethod;

		return webCall;
	}


	public WebCall() {

	}


	public String getUri() {
		return uri;
	}


	public void setUri(final String uri) {
		this.uri = uri;
	}


	public String getWebMethod() {
		return webMethod;
	}


	public void setWebMethod(final String webMethod) {
		this.webMethod = webMethod;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((webMethod == null) ? 0 : webMethod.hashCode());
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
		final WebCall other = (WebCall) obj;
		if (uri == null) {
			if (other.uri != null) {
				return false;
			}
		} else if (!uri.equals(other.uri)) {
			return false;
		}
		if (webMethod == null) {
			if (other.webMethod != null) {
				return false;
			}
		} else if (!webMethod.equals(other.webMethod)) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return "WebCall [uri=" + uri + ", webMethod=" + webMethod + "]";
	}

}
