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
package com.airepublic.microverse.core.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.environment.se.Weld;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.airepublic.microverse.core.descriptor.ServiceDescriptor;

public class ServiceRegistryTest {
	private final Weld weld = new Weld();
	private ServiceRegistry registry;


	@Before
	public void setup() {
		registry = weld.initialize().select(ServiceRegistry.class).get();
	}


	@Test
	public void testSortingByLatency() {
		// Given:
		// - 2 equal ServiceDescriptors
		// - with different hosts
		// - with different latencies, long latency first
		// are added to the registry
		final ServiceDescriptor descriptor1 = createServiceDescriptor("test1", "1", "classX", "intf1", "host1", 8080);
		final ServiceDescriptor descriptor2 = createServiceDescriptor("test1", "1", "classX", "intf1", "host2", 8080);

		registry.getServiceCache().add(descriptor1, 200L);
		registry.getServiceCache().add(descriptor2, 100L);

		// When: the ServiceDescriptor is queried
		final ServiceDescriptor descriptor = registry.getServiceDescriptor("test1", "1");

		// Then:
		assertThat(descriptor).isEqualTo(descriptor2);
	}


	@After
	public void tearDown() {
		weld.shutdown();
	}


	ServiceDescriptor createServiceDescriptor(final String id, final String version, final String serviceClass, final String serviceInterface, final String host, final int port) {
		final ServiceDescriptor descriptor = ServiceDescriptor.create(id, version, serviceClass, serviceInterface);
		descriptor.setHost(host);
		descriptor.setPort(port);
		return descriptor;
	}
}
