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
package com.airepublic.microverse.core.server;

import java.util.List;

import com.airepublic.microverse.core.descriptor.ServiceDescriptor;

/**
 * Interface defining a service-server.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IServiceServer {

  // /**
  // * Gets the mime-type the server uses to marshal/unmarshal requests/responses.
  // *
  // * @return the mime-type
  // */
  // String getMimeType();

  /**
   * Gets the list of {@link ServiceDescriptor}s which this service-server services.
   *
   * @return the list of {@link ServiceDescriptor}s
   */
  List<ServiceDescriptor> getServiceDescriptors();

  /**
   * Gets the supported mime-types the service server supports for marshalling/unmarshalling requests/responses.
   * 
   * @return the list of mime-types
   */
  List<String> getSupportedMimeTypes();
}
