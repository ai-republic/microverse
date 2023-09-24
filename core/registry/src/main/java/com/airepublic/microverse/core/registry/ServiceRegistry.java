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
package com.airepublic.microverse.core.registry;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.Action;
import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.descriptor.AbstractDescriptor;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.descriptor.WebCall;
import com.airepublic.microverse.core.discovery.RegistryCache;
import com.airepublic.microverse.core.discovery.RegistryDiscovererService;
import com.airepublic.microverse.core.discovery.ServiceCache;
import com.airepublic.microverse.core.discovery.ServiceCache.Metadata;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Service registry for the service platform.
 *
 * @author Torsten Oltmanns <br/>
 */
@Singleton
public class ServiceRegistry implements Closeable {
	private final static Logger LOG = LoggerFactory.getLogger(ServiceRegistry.class);
	private RegistryDescriptor registryDescriptor;
	@Inject
	private ServiceCache serviceCache;
	@Inject
	private RegistryCache registryCache;
	private final Timer timer = new Timer(false);


	/**
	 * Starts this service-registry.
	 *
	 * @param host the host under which the registry is running
	 * @param port the port under which the registry is running
	 * @param useSSL flag, whether to use SSL
	 * @param contextRoot the context under which the registry is running
	 */
	public void start(final RegistryDescriptor registryDescriptor) {
		// try to discover all registries
		registryCache.addAll(RegistryDiscovererService.requestRegistries());
		// remove this registry, since we don't want to propagate anything to ourselves
		registryCache.remove(registryDescriptor);

		// propagate this registry to other registries
		propagateRegistryToOtherRegistries(registryDescriptor, Action.ADD);

		// schedule service heartbeat checks to update latencies
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				for (final ServiceDescriptor serviceDescriptor : serviceCache.getAll()) {
					final Long latency = getLatency(serviceDescriptor);
					serviceCache.setMetadata(serviceDescriptor, ServiceCache.Metadata.create(latency));
				}
			}
		}, 0, Configuration.getServerHeartbeatInterval());

		LOG.info("Service-Registry started successfully on: " + registryDescriptor.getHost() + ":" + registryDescriptor.getPort() + " (SSL=" + registryDescriptor.isUseSSL() + ")");
	}


	/**
	 * Stops the registry and releases its resources.
	 */
	@PreDestroy
	@Override
	public void close() {
		timer.cancel();
		registryCache.close();
		serviceCache.close();
	}


	/**
	 * Registers the specified service and propagates it to other service-registries.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor} of the service
	 * @throws ServiceException
	 */
	public void registerService(final ServiceDescriptor serviceDescriptor) throws ServiceException {
		// if the new service is not know
		if (!serviceCache.contains(serviceDescriptor)) {
			// add the new service with fresh latency
			serviceCache.add(serviceDescriptor, getLatency(serviceDescriptor));

			// propagate the new registry to all other known registries
			propagateServiceToOtherRegistries(serviceDescriptor, Action.ADD);
		}
	}


	/**
	 * Unregisters the specified service and propagates it to other service-registries.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor} of the service
	 * @throws ServiceException
	 */
	public void unregisterService(final ServiceDescriptor serviceDescriptor) throws ServiceException {
		// if the new service is know
		if (serviceCache.contains(serviceDescriptor)) {
			// add the new service with fresh latency
			serviceCache.remove(serviceDescriptor);

			// propagate the new registry to all other known registries
			propagateServiceToOtherRegistries(serviceDescriptor, Action.REMOVE);
		}
	}


	/**
	 * Adds a service-registry to the {@link IRegistrySynchronizationStrategy}.
	 *
	 * @param registryDescriptor
	 * @throws ServiceException
	 */
	public void addRegistry(final RegistryDescriptor registryDescriptor) throws ServiceException {
		// if the new registry is not know
		if (!registryCache.contains(registryDescriptor)) {
			// add the new registry with fresh latency
			registryCache.add(registryDescriptor, getLatency(registryDescriptor));

			// propagate the new registry to all other known registries
			propagateRegistryToOtherRegistries(registryDescriptor, Action.ADD);
		}
	}


	/**
	 * Removes a service-registry from the {@link IRegistrySynchronizationStrategy}.
	 *
	 * @param registryDescriptor
	 * @throws ServiceException
	 */
	public void removeRegistry(final RegistryDescriptor registryDescriptor) throws ServiceException {
		// if the new registry is not already know
		if (registryCache.contains(registryDescriptor)) {
			// remove the registry
			registryCache.remove(registryDescriptor);

			// propagate the removed registry to all other known registries
			propagateRegistryToOtherRegistries(registryDescriptor, Action.REMOVE);
		}
	}


	/**
	 * Propagates the specified registry to all other known registries. The {@link Action} specifies
	 * whether it was added or removed.
	 *
	 * @param propagateRegistryDescriptor the {@link RegistryDescriptor} of the registry
	 * @param action the {@link Action}
	 */
	void propagateRegistryToOtherRegistries(final RegistryDescriptor propagateRegistryDescriptor, final Action action) {
		// propagate the new registry to all other known registries
		for (final RegistryDescriptor otherRegistryDescriptor : registryCache.getAll()) {
			if (!otherRegistryDescriptor.equals(propagateRegistryDescriptor)) {
				try {
					WebCall webCall = null;

					switch (action) {
						case ADD:
							webCall = otherRegistryDescriptor.getAddRegistryUri();
							break;
						case REMOVE:
							webCall = otherRegistryDescriptor.getRemoveRegistryUri();
							break;
					}

					ServiceUtils.executeRequest(otherRegistryDescriptor, webCall, propagateRegistryDescriptor);
				} catch (final Exception e) {
					LOG.warn("Unable to propagate registry (" + propagateRegistryDescriptor + ") to registry: " + registryDescriptor, e);
				}
			}
		}
	}


	/**
	 * Propagates the specified service to all other known registries. The {@link Action} specifies
	 * whether it was added or removed.
	 *
	 * @param propagateServiceDescriptor the {@link ServiceDescriptor} of the registry
	 * @param action the {@link Action}
	 */
	void propagateServiceToOtherRegistries(final ServiceDescriptor propagateServiceDescriptor, final Action action) {
		// propagate the service to all other known registries
		for (final RegistryDescriptor otherRegistryDescriptor : registryCache.getAll()) {
			try {
				WebCall webCall = null;

				switch (action) {
					case ADD:
						webCall = otherRegistryDescriptor.getRegisterUri();
						break;
					case REMOVE:
						webCall = otherRegistryDescriptor.getUnregisterUri();
						break;
				}

				ServiceUtils.executeRequest(otherRegistryDescriptor, webCall, propagateServiceDescriptor);
			} catch (final Exception e) {
				LOG.warn("Unable to propagate service (" + propagateServiceDescriptor + ") to registry: " + registryDescriptor, e);
			}
		}
	}


	/**
	 * Gets the {@link ServiceDescriptor} for the specified service-class.
	 *
	 * @param serviceClass the service-class
	 * @return the {@link ServiceDescriptor}
	 */
	public ServiceDescriptor getServiceDescriptor(final String serviceId, final String serviceVersion) {
		final Map<ServiceDescriptor, Metadata> serviceDescriptors = serviceCache.get(serviceId, serviceVersion);

		if (serviceDescriptors == null) {
			LOG.warn("No service registered for '" + serviceId + ":" + serviceVersion + "'");
			return null;
		}

		// TODO maybe implement a better strategy which service-server will be
		// connected to
		final ServiceDescriptor serviceDescriptor = serviceDescriptors.keySet().iterator().next();

		LOG.info("Retrieved service " + serviceDescriptor + " for '" + serviceId + ":" + serviceVersion + "'");

		return serviceDescriptor;
	}


	/**
	 * Gets all {@link ServiceDescriptor}s known to this {@link ServiceRegistry} .
	 *
	 * @return a list of {@link ServiceDescriptor}
	 */
	public ArrayList<ServiceDescriptor> getAllServiceDescriptors() {
		return serviceCache.getAll();
	}


	long getLatency(final AbstractDescriptor descriptor) {
		long latency = Long.MAX_VALUE;

		try {
			final long startTime = System.currentTimeMillis();
			ServiceUtils.executeRequest(descriptor, descriptor.getHeartbeatUri(), null);
			latency = System.currentTimeMillis() - startTime;
		} catch (final ServiceException e) {
			LOG.warn("Heartbeat to " + descriptor + " failed!");
		}

		return latency;
	}


	ServiceCache getServiceCache() {
		return serviceCache;
	}


	RegistryCache getRegistryCache() {
		return registryCache;
	}
}
