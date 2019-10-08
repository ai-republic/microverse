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
package com.airepublic.microverse.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.airepublic.microverse.core.client.ServiceLookup;
import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.microverse.test.interfaces.ITestService;

public class DeployerTest {
	public static int PORT = 8085;
	private ServiceLookup lookup;


	@Before
	public void setup() throws ServiceException {
		lookup = ServiceLookup.create();
	}


	@After
	public void tearDown() throws ServiceException {
		Deployer.undeploy("localhost", PORT, false, "/microverse-rest-server", "TestClass1", "1.0");
	}


	@Test
	public void testDeployViaMain() throws Exception {
		Deployer.main(new String[] { "-host=localhost", "-port=" + PORT, "-useSSL=false", "-contextRoot=/microverse-rest-server", "-bundle=src/test/resources/test1-bundle.zip" });

		final ServiceDescriptor serviceDescriptor = lookup.getServiceDescriptor("TestClass1", "1.0");
		assertThat(serviceDescriptor).isNotNull();

		final ITestService client = lookup.getServiceClient(serviceDescriptor);
		assertThat(client).isNotNull();

		final String result = client.sayHello("Test");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Hello Test");
	}


	@Test
	public void testDeployDirect() throws Exception {
		Deployer.deploy("localhost", PORT, false, "/microverse-rest-server", Paths.get("src/test/resources/test1-bundle.zip"));
		final ServiceLookup lookup = ServiceLookup.create();

		final ServiceDescriptor serviceDescriptor = lookup.getServiceDescriptor("TestClass1", "1.0");
		assertThat(serviceDescriptor).isNotNull();

		final ITestService client = lookup.getServiceClient(serviceDescriptor);
		assertThat(client).isNotNull();

		final String result = client.sayHello("Test");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Hello Test");
	}


	@Test
	public void testRedeploy() throws Exception {
		Deployer.deploy("localhost", PORT, false, "/microverse-rest-server", Paths.get(Deployer.class.getClassLoader().getResource("test1-bundle.zip").toURI()));

		ServiceDescriptor serviceDescriptor = lookup.getServiceDescriptor("TestClass1", "1.0");
		assertThat(serviceDescriptor).isNotNull();

		ITestService client = lookup.getServiceClient(serviceDescriptor);
		assertThat(client).isNotNull();

		String result = client.sayHello("Foo");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Hello Foo");

		// re-deploy
		Deployer.deploy("localhost", PORT, false, "/microverse-rest-server", Paths.get(Deployer.class.getClassLoader().getResource("test1-bundle.zip").toURI()));

		serviceDescriptor = lookup.getServiceDescriptor("TestClass1", "1.0");
		assertThat(serviceDescriptor).isNotNull();

		client = lookup.getServiceClient(serviceDescriptor);
		assertThat(client).isNotNull();

		result = client.sayHello("Bar");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Hello Bar");

	}

}
