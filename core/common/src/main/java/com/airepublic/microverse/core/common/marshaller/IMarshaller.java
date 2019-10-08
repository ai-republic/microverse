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
package com.airepublic.microverse.core.common.marshaller;

import java.io.InputStream;
import java.io.Serializable;

import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Interface to transform the method calls and return object to a individual format.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IMarshaller {
  /**
   * Serialize the specified {@link Serializable} object.
   * 
   * @param object the {@link Serializable} object
   * @return the serialized bytes
   */
  public byte[] serialize(Serializable object) throws ServiceException;

  /**
   * Deserializes the object from the input-stream.
   * 
   * @param inputStream the input stream
   * @return the deserialized object
   */
  public <T> T deserialize(InputStream inputStream, Class<T> clazz) throws ServiceException;

  /**
   * Deserializes the object from the specified object data.
   * 
   * @param objectData the object data
   * @return the deserialized object
   */
  public <T> T deserialize(byte[] objectData, Class<T> clazz) throws ServiceException;

  /**
   * Gets the supported mime-type for this marshaller.
   * 
   * @return the mime-type
   */
  public String getMimeType();
}
