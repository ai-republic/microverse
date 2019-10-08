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
import java.util.ArrayList;

import org.apache.commons.lang3.SerializationUtils;

/**
 * Describes the remote method call with its parameters.
 *
 * @author Torsten Oltmanns
 *
 */
public class MethodCall implements Serializable {
	private static final long serialVersionUID = -6050997247296881913L;
	private String methodName;
	private ArrayList<byte[]> parameters;


	private MethodCall() {
	}


	private MethodCall(final String methodName, final ArrayList<byte[]> parameters) {
		this.methodName = methodName;
		this.parameters = parameters;
	}


	public static MethodCall create(final String methodName, final Object[] parameters) {
		final ArrayList<byte[]> params = new ArrayList<>();

		for (final Object param : parameters) {
			if (param instanceof Serializable) {
				params.add(SerializationUtils.serialize((Serializable) param));
			} else {
				throw new RuntimeException("Parameter " + param + " for method " + methodName + " does not implement Serializable");
			}
		}
		return new MethodCall(methodName, params);
	}


	public final String getMethodName() {
		return methodName;
	}


	public final ArrayList<byte[]> getParameters() {
		return parameters;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
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
		final MethodCall other = (MethodCall) obj;
		if (methodName == null) {
			if (other.methodName != null) {
				return false;
			}
		} else if (!methodName.equals(other.methodName)) {
			return false;
		}
		if (parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if (!parameters.equals(other.parameters)) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return "MethodCall [methodName=" + methodName + ", parameters=" + parameters + "]";
	}

}
