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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.airepublic.microverse.core.client.ServiceLookup;
import com.airepublic.microverse.core.common.Configuration;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.microverse.test.interfaces.ITestService;

/**
 * Utilities to create a service Server and service client.
 *
 * @author Torsten Oltmanns
 *
 */
public class StandaloneServiceServer_IntTest {
	private ServiceLookup lookup;
	private Weld weld;


	@Before
	public void setup() throws ServiceException {
		System.setProperty(Configuration.SERVER_PORT, "8082");
		System.setProperty(Configuration.SERVER_USESSL, "false");
		System.setProperty(Configuration.SERVICE_DEPLOY_DIR, "D:/TEMP/services");

		System.setProperty("javax.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
		System.setProperty("javax.persistence.jdbc.url", "jdbc:oracle:thin:@//localhost:1521/XE");
		System.setProperty("javax.persistence.jdbc.user", "MICROVERSE");
		System.setProperty("javax.persistence.jdbc.password", "MICROVERSE_PWD");

		weld = new Weld("Standalone-ServiceServer-IntTest");
	}


	@After
	public void tearDown() {
		weld.shutdown();
	}


	@Test
	public void testStandalone() throws Exception {
		lookup = ServiceLookup.create();
		// lookup.unregisterAllServices();

		assertThat(lookup).isNotNull();

		String result1 = null;
		String result2 = null;

		// create service server
		final WeldContainer weldContainer = weld.initialize();
		final StandaloneServiceServer serviceServer = weldContainer.select(StandaloneServiceServer.class).get();

		// add services
		serviceServer.addService(Paths.get(getClass().getClassLoader().getResource("test1-bundle.zip").toURI()));
		serviceServer.addService(Paths.get(getClass().getClassLoader().getResource("test2-bundle.zip").toURI()));

		// get service
		final ITestService service1 = lookup.getServiceClient(ITestService.class, "TestClass1", "1.0");
		result1 = service1.sayHello("Foo");

		final ITestService service2 = lookup.getServiceClient(ITestService.class, "TestClass2", "1.0");
		result2 = service2.sayHello("Foo");

		serviceServer.shutDown();

		assertThat(result1).isEqualTo("Hello Foo");
		assertThat(result2).isEqualTo("Bye-bye Foo");

	}
}
