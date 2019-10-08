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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;
import com.airepublic.microverse.core.discovery.IRegistryDiscoverer;

/**
 * An implementation of {@link IRegistryDiscoverer} which request all known registries from
 * single/multiple registries
 *
 * @author Torsten Oltmanns
 *
 */
public class ConfiguredRegistryDiscoverer implements IRegistryDiscoverer {
	private final static Logger LOG = LoggerFactory.getLogger(ConfiguredRegistryDiscoverer.class);


	/**
	 * Creates a {@link ConfiguredRegistryDiscoverer} which requests a list of registries from the
	 * specified database configured on the system-properties.
	 *
	 * @return the {@link ConfiguredRegistryDiscoverer}
	 */
	public static ConfiguredRegistryDiscoverer create() {
		return new ConfiguredRegistryDiscoverer();
	}


	/**
	 * Constructor. Creates a {@link ConfiguredRegistryDiscoverer} which requests a list of
	 * registries from the specified registries. One-by-one the registries are contacted until one
	 * answers.
	 *
	 * @param requestRegistryDescriptors the list of registries to request the list from
	 */
	public ConfiguredRegistryDiscoverer() {
		// for (final RegistryDescriptor registryDescriptor : requestRegistryDescriptors) {
		// try {
		// final HttpGet request = new HttpGet(ServiceUtils.buildURL(registryDescriptor.getHost(),
		// registryDescriptor.getPort(), registryDescriptor.isUseSSL(),
		// registryDescriptor.getConfigurationUri().getUri()));
		// final HttpResponse response = ServiceUtils.executeRequest(request,
		// registryDescriptor.isUseSSL());
		// // get marshaller for answer
		// final IMarshaller marshaller =
		// MarshallerFactory.get(response.getFirstHeader("Content-Type").getValue());
		//
		// // try to deserialize the list of RegistryDescriptors
		// registryDescriptors = marshaller.deserialize(response.getEntity().getContent(),
		// ArrayList.class);
		//
		// // ok, got a reply so finish
		// break;
		// } catch (final Exception e) {
		// LOG.warn("Could not retrieve registries from registry: " + registryDescriptor, e);
		// }
		// }

	}


	@Override
	public List<RegistryDescriptor> requestRegistries() {
		final Map<String, String> properties = new HashMap<>();
		properties.put("javax.persistence.jdbc.driver", System.getProperty("javax.persistence.jdbc.driver"));
		properties.put("javax.persistence.jdbc.url", System.getProperty("javax.persistence.jdbc.url"));
		properties.put("javax.persistence.jdbc.user", System.getProperty("javax.persistence.jdbc.user"));
		properties.put("javax.persistence.jdbc.password", System.getProperty("javax.persistence.jdbc.password"));

		final EntityManager entityManager = Persistence.createEntityManagerFactory("microverse-pu", properties).createEntityManager();

		final CriteriaQuery<RegistryEntry> query = entityManager.getCriteriaBuilder().createQuery(RegistryEntry.class);
		final Root<RegistryEntry> root = query.from(RegistryEntry.class);
		final CriteriaQuery<RegistryEntry> all = query.select(root);
		final TypedQuery<RegistryEntry> allQuery = entityManager.createQuery(all);
		final List<RegistryEntry> registryEntries = allQuery.getResultList();

		LOG.info("Found registries:\n" + registryEntries.stream().map(RegistryEntry::toString).collect(Collectors.joining("\n\t", "\t", "")));

		final List<RegistryDescriptor> registryDescriptors = new ArrayList<>();

		for (final RegistryEntry registryEntry : registryEntries) {
			// check if the context-root starts with a '/' - if not prepend it
			final String contextRoot = registryEntry.getContextRoot().startsWith("/") ? registryEntry.getContextRoot() : ("/" + registryEntry.getContextRoot());
			registryDescriptors.add(RegistryDescriptor.create(registryEntry.getHost(), registryEntry.getPort(), registryEntry.isUseSSL(), contextRoot, MarshallerFactory.getSupportedMimeTypes()));
		}

		entityManager.close();

		return registryDescriptors;
	}


	@Override
	public void close() {
	}

}
