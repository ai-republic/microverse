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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.airepublic.microverse.core.client.ServiceLookup;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.rest.client.RestServiceServerUtil;
import com.airepublic.microverse.test.interfaces.ITestService;

/**
 * Utilities to create a service Server and service client.
 *
 * @author Torsten Oltmanns
 *
 */
public class RestServiceServer_IntTest {
	public static final int PORT = 8085;
	private ServiceLookup lookup;


	@Before
	public void setup() {
		// System.setProperty("javax.persistence.jdbc.driver",
		// "org.apache.derby.jdbc.EmbeddedDriver");
		// System.setProperty("javax.persistence.jdbc.url",
		// "jdbc:derby:memory:sampleDB;create=true");

		System.setProperty("javax.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
		System.setProperty("javax.persistence.jdbc.url", "jdbc:oracle:thin:@//localhost:1521/XE");
		System.setProperty("javax.persistence.jdbc.user", "MICROVERSE");
		System.setProperty("javax.persistence.jdbc.password", "MICROVERSE_PWD");

	}


	@Test
	public void runRestServiceTest() throws Exception {
		lookup = ServiceLookup.create();
		// lookup.unregisterAllServices();

		assertThat(lookup).isNotNull();

		// create service server
		final RestServiceServerUtil util = RestServiceServerUtil.create("localhost", 8085, false, "/microverse-rest-server").addService(Paths.get(getClass().getClassLoader().getResource("test1-bundle.zip").toURI())).addService(Paths.get(getClass().getClassLoader().getResource("test2-bundle.zip").toURI()));

		// get service
		final ServiceDescriptor sd1 = lookup.getServiceDescriptor("TestClass1", "1.0");
		final ITestService service1 = lookup.getServiceClient(sd1);
		final String result1 = service1.sayHello("Foo");

		final ServiceDescriptor sd2 = lookup.getServiceDescriptor("TestClass2", "1.0");
		final ITestService service2 = lookup.getServiceClient(sd2);
		final String result2 = service2.sayHello("Foo");

		util.removeService("TestClass1", "1.0");
		util.removeService("TestClass2", "1.0");

		assertThat(result1).isEqualTo("Hello Foo");
		assertThat(result2).isEqualTo("Bye-bye Foo");

	}

}
