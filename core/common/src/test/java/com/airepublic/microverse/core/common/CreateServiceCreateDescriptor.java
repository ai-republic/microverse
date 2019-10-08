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
package com.airepublic.microverse.core.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;

import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.ServiceCreateDescriptor;
import com.airepublic.microverse.core.descriptor.ServiceCreateDescriptorList;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * Test for writing microverse configurations.
 *
 * @author Torsten Oltmanns
 *
 */
public class CreateServiceCreateDescriptor {

	@Test
	public void testCreateServiceStartupConfiguration() throws IllegalArgumentException, ServiceException, IOException {
		final ServiceCreateDescriptorList list = ServiceCreateDescriptorList.create("localhost", 8081, false, "microverse-rest-server");
		list.add(ServiceCreateDescriptor.create("test1-bundle.zip"));

		Files.write(Paths.get("./microverse.config"), MarshallerFactory.get("application/json").serialize(list), StandardOpenOption.CREATE);
	}
}
