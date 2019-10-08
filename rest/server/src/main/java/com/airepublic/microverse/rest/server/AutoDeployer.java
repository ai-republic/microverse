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
package com.airepublic.microverse.rest.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.ServiceCreateDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceCreateDescriptorList;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.microverse.rest.client.Deployer;

@WebListener
public class AutoDeployer implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(AutoDeployer.class);


	@Override
	public void contextInitialized(final ServletContextEvent event) {
		new Thread() {

			@Override
			public void run() {
				try {
					Thread.sleep(Configuration.getAutoDeployStartDelay());
					LOG.info("Loading existing services...");
					loadConfiguredServices();
				} catch (final Exception e) {
					LOG.error("Could not load configured services!", e);
				}
			}
		}.start();
	}


	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
	}


	/**
	 * Loads configured services from a directory found by the system property
	 * {@link Configuration#SERVICE_DEPLOY_DIR}. The directory must contain a file
	 * {@link Configuration#AUTODEPLOY_DEPLOYMENT_CONFIG_FILE_NAME} containing a serialized
	 * (json)list of {@link ServiceCreateDescriptor}. All configured service-bundles must be
	 * specified relatively to the config directory.
	 *
	 * @throws ServiceException
	 */
	protected void loadConfiguredServices() throws ServiceException {
		try {
			final String configDirStr = Configuration.getServiceDeployDir();

			if (configDirStr != null) {
				final java.nio.file.Path configDir = Paths.get(configDirStr);

				if (!Files.exists(configDir)) {
					LOG.warn("Could not auto-load services due to invalid service directory configured in '" + Configuration.SERVICE_DEPLOY_DIR + "': " + configDir);
					return;
				}

				final java.nio.file.Path configFile = configDir.resolve(Configuration.getAutoDeployDeploymentConfigFilename());

				if (!Files.exists(configFile)) {
					LOG.warn("Could not auto-load services due to missing configuration file '" + Configuration.AUTODEPLOY_DEPLOYMENT_CONFIG_FILE_NAME + "' in: " + configDir);
					return;
				}

				final byte[] configBytes = Files.readAllBytes(configFile);
				final ServiceCreateDescriptorList descriptors = MarshallerFactory.get("application/json").deserialize(configBytes, ServiceCreateDescriptorList.class);

				deployConfiguredServices(configDir, descriptors);
			}
		} catch (final Exception e) {
			throw new ServiceException(e);
		}
	}


	/**
	 * Loads configured services from a directory found by the system property
	 * 'microverse.service.config.dir'. The directory must contain a file 'microverse.config'
	 * containing a serialized (json)list of {@link ServiceCreateDescriptor}. All configured
	 * service-bundles must be specified relatively to the config directory.
	 *
	 * @throws ServiceException
	 */
	protected void deployConfiguredServices(final Path configDir, final ServiceCreateDescriptorList descriptors) throws ServiceException {
		try {
			for (final ServiceCreateDescriptor descriptor : descriptors.getDescriptors()) {
				LOG.info("Deploying service: " + descriptor);
				Deployer.deploy(descriptors.getHost(), descriptors.getPort(), descriptors.getUseSSL(), descriptors.getContextRoot(), configDir.resolve(descriptor.getBundle()));
			}
		} catch (final Exception e) {
			throw new ServiceException(e);
		}
	}
}
