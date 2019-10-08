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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Factory to provide {@link IMarshaller} for a given mime-type.
 *
 * @author Torsten Oltmanns
 *
 */
public class MarshallerFactory {
	private static ServiceLoader<IMarshaller> loader = ServiceLoader.load(IMarshaller.class, MarshallerFactory.class.getClassLoader());
	private static Map<String, IMarshaller> marshallerForMimeTypeMap = new HashMap<>();

	static {
		try {
			final Iterator<IMarshaller> marshallers = loader.iterator();

			while (marshallers.hasNext()) {
				final IMarshaller marshaller = marshallers.next();
				marshallerForMimeTypeMap.put(marshaller.getMimeType(), marshaller);
			}
		} catch (final ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
	}


	/**
	 * Get the {@link IMarshaller} for the specified mime-type.
	 *
	 * @param mimeType the mime-type
	 * @return the {@link IMarshaller}
	 * @throws IllegalArgumentException if no {@link IMarshaller} could be found for the specified
	 *         mime-type
	 */
	public static IMarshaller get(final String mimeType) throws IllegalArgumentException {
		return marshallerForMimeTypeMap.get(mimeType);
	}


	/**
	 * Gets the mime-types for all registered marshallers.
	 *
	 * @return the set of supported mime-types
	 */
	public static List<String> getSupportedMimeTypes() {
		return Collections.unmodifiableList(marshallerForMimeTypeMap.keySet().stream().collect(Collectors.toList()));
	}


	/**
	 * Add a supported marshaller.
	 *
	 * @param marshaller the marshaller
	 */
	public static void addMarshaller(final IMarshaller marshaller) {
		marshallerForMimeTypeMap.put(marshaller.getMimeType(), marshaller);
	}


	/**
	 * Remove all configured marshallers.
	 */
	public static void clear() {
		marshallerForMimeTypeMap.clear();
	}
}
