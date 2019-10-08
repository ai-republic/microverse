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
package com.airepublic.microverse.standalone.server;

import org.jboss.weld.environment.se.Weld;

import com.airepublic.microverse.PackageClass;

/**
 * Bootstrap class to start a {@link StandaloneServiceServer} from the fat-jar via command-line. The
 * command-line needs the following system-parameters set: <br/>
 * <code>
 * -Dmicroverse.service.deploy.dir=&lt;path to deployment directory&gt;<br/>
 * -Dmicroverse.autodeploy.start.delay=&lt;delay after when to start the deployment in millis&gt;
 * <br/>
 * -Dmicroverse.registry.heartbeat.interval=&lt;intervall of heartbeat requests in millis&gt;<br/>
 * -Dmicroverse.server.port=&lt;port&gt;<br/>
 * -Dmicroverse.server.useSSL=&lt;flag whether to use SSL&gt;<br/>
 * -Djavax.persistence.jdbc.driver=&lt;DB driver&gt;<br/>
 * -Djavax.persistence.jdbc.url=&lt;DB connection URL&gt;<br/>
 * -Djavax.persistence.jdbc.user=&lt;DB user&gt;<br/>
 * -Djavax.persistence.jdbc.password=&lt;DB password&gt;<br/>
 * -Dhibernate.dialect=&lt;Hibernate DB dialect class&gt;<br/>
 * </code>
 *
 * @author Torsten Oltmanns
 *
 */
public class StandaloneServiceServerBootstrap {
	public static void main(final String[] args) {
		StandaloneServiceServer bootServer = null;

		try {
			final Weld weld = new Weld("Standalone-Service-Server");
			bootServer = weld.disableDiscovery().addPackage(true, PackageClass.class).initialize().select(StandaloneServiceServer.class).get();
			final StandaloneServiceServer server = bootServer;

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					// to shutdown cleanly
					server.shutDown();
				}
			});
		} catch (final Exception e) {
			// to shutdown cleanly
			if (bootServer != null) {
				bootServer.shutDown();
			}
		}

	}

}
