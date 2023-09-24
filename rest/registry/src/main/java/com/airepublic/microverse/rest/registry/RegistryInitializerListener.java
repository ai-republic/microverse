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
package com.airepublic.microverse.rest.registry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;

@WebListener
public class RegistryInitializerListener implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(RegistryInitializerListener.class);


	@Override
	public void contextInitialized(final ServletContextEvent event) {
		final String host = Configuration.getRegistryHost();
		final int port = Configuration.getRegistryPort();
		final boolean useSSL = Configuration.getRegistryUseSSL();

		final RegistryDescriptor registryDescriptor = RegistryDescriptor.create(host, port, useSSL, event.getServletContext().getContextPath(), MarshallerFactory.getSupportedMimeTypes());

		// call heartbeat of registry to initialize REST context - must run
		// asynchronously otherwise it will block container startup
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000L);
					LOG.info("Trying to initialize Service-Registry...");
					ServiceUtils.executeRequest(registryDescriptor.getHost(), registryDescriptor.getPort(), registryDescriptor.isUseSSL(), registryDescriptor.getHeartbeatUri(), MarshallerFactory.getSupportedMimeTypes(), null);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}


	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
	}
}
