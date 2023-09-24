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
package com.airepublic.microverse.core.common.marshaller;

import java.io.InputStream;
import java.io.Serializable;

import com.airepublic.microverse.core.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Marshaller supporting serialization as Json objects.
 *
 * @author Torsten Oltmanns
 *
 */
public class JsonMarshaller implements IMarshaller {
	private final ObjectMapper mapper = new ObjectMapper();


	@Override
	public byte[] serialize(final Serializable object) throws ServiceException {
		try {
			return mapper.writeValueAsString(object).getBytes();
		} catch (final Exception e) {
			throw new ServiceException("Could not serialize object: " + object, e);
		}
	}


	@Override
	public <T> T deserialize(final InputStream inputStream, final Class<T> clazz) throws ServiceException {
		try {
			return mapper.readValue(inputStream, clazz);
		} catch (final Exception e) {
			throw new ServiceException("Could not deserialize object from input-stream!", e);
		}
	}


	@Override
	public <T> T deserialize(final byte[] objectData, final Class<T> clazz) throws ServiceException {
		try {
			return mapper.readValue(objectData, clazz);
		} catch (final Exception e) {
			throw new ServiceException("Could not deserialize object from object data: " + objectData, e);
		}
	}


	@Override
	public String getMimeType() {
		return "application/json";
	}
}
