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
package com.airepublic.microverse.core.discovery;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import com.airepublic.microverse.core.descriptor.RegistryDescriptor;

/**
 * Service provider for {@link IRegistryDiscoverer} implementations.
 *
 * @author Torsten Oltmanns
 *
 */
public class RegistryDiscovererService {
	private static ServiceLoader<IRegistryDiscoverer> loader = ServiceLoader.load(IRegistryDiscoverer.class, RegistryDiscovererService.class.getClassLoader());


	private RegistryDiscovererService() {
	}


	public static List<RegistryDescriptor> requestRegistries() {
		List<RegistryDescriptor> registries = null;

		try {
			final Iterator<IRegistryDiscoverer> registryDiscoverers = loader.iterator();

			while (registries == null && registryDiscoverers.hasNext()) {
				final IRegistryDiscoverer registryDiscoverer = registryDiscoverers.next();
				registries = registryDiscoverer.requestRegistries();
			}
		} catch (final ServiceConfigurationError serviceError) {
			registries = null;
			serviceError.printStackTrace();

		}
		return registries;
	}
}
