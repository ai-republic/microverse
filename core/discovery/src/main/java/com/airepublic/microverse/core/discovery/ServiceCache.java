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

import java.io.Closeable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.descriptor.ServiceDescriptor;

@Singleton
public class ServiceCache implements Serializable, Closeable {
	private static final long serialVersionUID = -1078025534926983036L;
	private final static Logger LOG = LoggerFactory.getLogger(ServiceCache.class);
	private final Map<String, Map<ServiceDescriptor, Metadata>> services = new ConcurrentHashMap<>();

	public static class Metadata implements Comparable<Metadata> {
		public LocalDateTime lastUpdate;
		public Long latency;


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


	public void add(final ServiceDescriptor serviceDescriptor, final Long latency) {
		Map<ServiceDescriptor, Metadata> serviceDescriptors = services.get(serviceDescriptor.getId() + ":" + serviceDescriptor.getVersion());

		if (serviceDescriptors == null) {
			serviceDescriptors = new LinkedHashMap<ServiceDescriptor, Metadata>();
			services.put(serviceDescriptor.getId() + ":" + serviceDescriptor.getVersion(), serviceDescriptors);
		}

		// check if a service with the same service-class is already registered
		if (!serviceDescriptors.containsKey(serviceDescriptor)) {
			serviceDescriptors.put(serviceDescriptor, Metadata.create(latency));
			serviceDescriptors = sortByValue(serviceDescriptors);
			services.put(serviceDescriptor.getId() + ":" + serviceDescriptor.getVersion(), serviceDescriptors);
			LOG.info("Service " + serviceDescriptor + " registered successfully");
		} else {
			LOG.warn("Service " + serviceDescriptor + " is already registered and was not be replaced! You must unregister the old service first!");
		}

		LOG.debug("Using Service cache: " + this);
		LOG.debug("Services available: " + services.keySet().size());
	}


	protected <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
		final Map<K, V> result = new LinkedHashMap<>();
		map.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue())).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));

		return result;
	}


	public Map<ServiceDescriptor, Metadata> get(final String serviceId, final String serviceVersion) {
		return services.get(serviceId + ":" + serviceVersion);
	}


	public boolean contains(final ServiceDescriptor serviceDescriptor) {
		final Map<ServiceDescriptor, Metadata> serviceDescriptors = services.get(serviceDescriptor.getId() + ":" + serviceDescriptor.getVersion());

		if (serviceDescriptors != null && serviceDescriptors.containsKey(serviceDescriptor)) {
			return true;
		}

		return false;
	}


	public void remove(final ServiceDescriptor serviceDescriptor) {
		final Map<ServiceDescriptor, Metadata> serviceDescriptors = services.get(serviceDescriptor.getId() + ":" + serviceDescriptor.getVersion());

		if (serviceDescriptors != null) {
			serviceDescriptors.remove(serviceDescriptor);
		}

		LOG.info("Service " + serviceDescriptor + " unregistered successfully");
	}


	public void removeAll() {
		services.clear();
	}


	public ArrayList<ServiceDescriptor> getAll() {
		return services.values().stream().map(map -> map.keySet()).collect(Collector.of(ArrayList::new, ArrayList::addAll, (result, values) -> {
			result.addAll(values);
			return result;
		}));
	}


	public void setMetadata(final ServiceDescriptor serviceDescriptor, final Metadata metadata) {
		services.get(serviceDescriptor.getId() + ":" + serviceDescriptor.getVersion()).put(serviceDescriptor, metadata);
	}


	@PreDestroy
	@Override
	public void close() {
		services.clear();
	}
}
