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
package com.airepublic.microverse.core.discovery;

import java.io.Closeable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import com.airepublic.microverse.core.descriptor.RegistryDescriptor;

/**
 * Cache the registry-servers sorted by its latency.
 *
 * @author Torsten Oltmanns
 */
@Singleton
public class RegistryCache implements Serializable, Closeable {
	private static final long serialVersionUID = -6397067639386945344L;
	public final static long DEFAULT_LATENCY = 300000L;
	private Map<RegistryDescriptor, Metadata> cache = new ConcurrentHashMap<>();

	public static class Metadata implements Comparable<Metadata> {
		public LocalDateTime lastUpdate;
		public long latency;


		public static Metadata create(final Long latency) {
			final Metadata metadata = new Metadata();
			metadata.lastUpdate = LocalDateTime.now();
			metadata.latency = latency;
			return metadata;
		}


		@Override
		public int compareTo(final Metadata o) {
			return Long.compare(latency, o.latency);
		}
	}


	@Override
	@PreDestroy
	public void close() {
		cache.clear();
	}


	public Collection<RegistryDescriptor> getAll() {
		return cache.keySet();
	}


	public void add(final RegistryDescriptor registryDescriptor, final long latency) {
		final Metadata md = Metadata.create(latency);
		cache.put(registryDescriptor, md);
		cache = sortByValue(cache);
	}


	protected <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
		final Map<K, V> result = new LinkedHashMap<>();
		final Stream<Entry<K, V>> st = map.entrySet().stream();

		st.sorted(Comparator.comparing(e -> e.getValue())).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));

		return result;
	}


	public boolean contains(final RegistryDescriptor registryDescriptor) {
		return cache.containsKey(registryDescriptor);
	}


	public void remove(final RegistryDescriptor registryDescriptor) {
		cache.remove(registryDescriptor);
	}


	public void addAll(final List<RegistryDescriptor> registryDescriptors) {
		for (final RegistryDescriptor registryDescriptor : registryDescriptors) {
			add(registryDescriptor, DEFAULT_LATENCY);
		}
	}


	public void setMetadata(final RegistryDescriptor registryDescriptor, final Metadata metadata) {
		cache.put(registryDescriptor, metadata);
	}

}
