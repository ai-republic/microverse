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
package com.airepublic.microverse.core.common;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants for the system property configurations and service descriptor name.
 *
 * @author Torsten Oltmanns
 *
 */
@Singleton
public class Configuration {
	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
	public static final String SERVICE_DEPLOY_DIR = "microverse.service.deploy.dir";
	public static final String REGISTRY_HOST = "microverse.registry.host";
	public static final String REGISTRY_PORT = "microverse.registry.port";
	public static final String REGISTRY_USESSL = "microverse.registry.useSSL";
	public static final String REGISTRY_CONTEXTROOT = "microverse.registry.contextroot";
	public static final String AUTODEPLOY_START_DELAY = "microverse.autodeploy.start.delay";
	public static final String AUTODEPLOY_SERVICE_DESCRIPTOR_FILE_NAME = "microservice.txt";
	public static final String AUTODEPLOY_DEPLOYMENT_CONFIG_FILE_NAME = "microverse.config";
	public static final String REGISTRY_HEARTBEAT_INTERVAL = "microverse.registry.hearbeat.interval";
	public static final String SERVER_HEARTBEAT_INTERVAL = "microverse.server.hearbeat.interval";
	public static final String SERVER_PORT = "microverse.server.port";
	public static final String SERVER_USESSL = "microverse.server.useSSL";


	public static String getServiceDeployDir() {
		return getString(SERVICE_DEPLOY_DIR, "./deploy");
	}


	public static String getRegistryHost() {
		return getString(REGISTRY_HOST, "localhost");
	}


	public static int getRegistryPort() {
		return getInt(REGISTRY_PORT, 8080);
	}


	public static boolean getRegistryUseSSL() {
		return getBoolean(REGISTRY_USESSL, false);
	}


	public static String getRegistryContextroot() {
		return getString(REGISTRY_CONTEXTROOT, "microverse-rest-registry");
	}


	public static long getAutoDeployStartDelay() {
		return getLong(AUTODEPLOY_START_DELAY, 0L);
	}


	public static String getAutoDeployServiceDescriptorFilename() {
		return AUTODEPLOY_SERVICE_DESCRIPTOR_FILE_NAME;
	}


	public static String getAutoDeployDeploymentConfigFilename() {
		return AUTODEPLOY_DEPLOYMENT_CONFIG_FILE_NAME;
	}


	public static long getRegistryHeartbeatInterval() {
		return getLong(REGISTRY_HEARTBEAT_INTERVAL, 10000L);
	}


	public static long getServerHeartbeatInterval() {
		return getLong(SERVER_HEARTBEAT_INTERVAL, 10000L);
	}


	public static int getServerPort() {
		return getInt(SERVER_PORT, 8080);
	}


	public static boolean getServerUseSSL() {
		return getBoolean(SERVER_USESSL, false);
	}


	private static String getString(final String key, final String defaultValue) {
		final String value = System.getProperty(key);

		if (value != null) {
			return value;
		}

		return defaultValue;
	}


	private static boolean getBoolean(final String key, final boolean defaultValue) {
		final String value = System.getProperty(key);

		if (StringUtils.isNotEmpty(value)) {
			try {
				return Boolean.parseBoolean(value);
			} catch (final Exception e) {
				LOG.error("System-property '" + key + "' must contain a boolean value!");
			}
		}

		return defaultValue;
	}


	private static int getInt(final String key, final int defaultValue) {
		final String value = System.getProperty(key);

		if (StringUtils.isNotEmpty(value)) {
			try {
				return Integer.parseInt(value);
			} catch (final NumberFormatException e) {
				LOG.error("System-property '" + key + "' must contain an integer value!");
			}
		}

		return defaultValue;
	}


	private static long getLong(final String key, final long defaultValue) {
		final String value = System.getProperty(key);

		if (StringUtils.isNotEmpty(value)) {
			try {
				return Long.parseLong(value);
			} catch (final NumberFormatException e) {
				LOG.error("System-property '" + key + "' must contain a long value!");
			}
		}

		return defaultValue;
	}
}
