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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MethodDescriptor implements Serializable {
  private static final long serialVersionUID = 4910474995954882588L;
  private String returnType;
  private String methodName;
  private Map<String, String> params;

  private MethodDescriptor() {}

  private MethodDescriptor(final String returnType, final String methodName, final Map<String, String> params) {
    this.returnType = returnType;
    this.methodName = methodName;
    this.params = Collections.unmodifiableMap(params);
  }

  public static final MethodDescriptor create(final Method method) {
    final Map<String, String> params = new LinkedHashMap<>();

    for (final Parameter param : method.getParameters()) {
      params.put(param.getName(), param.getType().getName());
    }

    return new MethodDescriptor(method.getReturnType().getName(), method.getName(), params);
  }

  public String getReturnType() {
    return returnType;
  }

  public String getMethodName() {
    return methodName;
  }

  public Map<String, String> getParams() {
    return params;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
    result = prime * result + ((params == null) ? 0 : params.hashCode());
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
    final MethodDescriptor other = (MethodDescriptor) obj;
    if (methodName == null) {
      if (other.methodName != null) {
        return false;
      }
    } else if (!methodName.equals(other.methodName)) {
      return false;
    }
    if (params == null) {
      if (other.params != null) {
        return false;
      }
    } else if (!params.equals(other.params)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "MethodDescriptor [returnType=" + returnType + ", methodName=" + methodName + ", params=" + params + "]";
  }

}
