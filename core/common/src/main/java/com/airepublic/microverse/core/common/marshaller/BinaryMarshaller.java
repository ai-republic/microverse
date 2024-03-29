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

import org.apache.commons.lang3.SerializationUtils;

/**
 * Marshaller supporting serialization as binary Java objects.
 *
 * @author Torsten Oltmanns
 *
 */
public class BinaryMarshaller implements IMarshaller {

	@Override
	public byte[] serialize(final Serializable object) {
		return SerializationUtils.serialize(object);
	}


	@Override
	public <T> T deserialize(final InputStream inputStream, final Class<T> clazz) {
		return SerializationUtils.deserialize(inputStream);
	}


	@Override
	public <T> T deserialize(final byte[] objectData, final Class<T> clazz) {
		return SerializationUtils.deserialize(objectData);
	}


	@Override
	public String getMimeType() {
		return "application/octet-stream";
	}
}
