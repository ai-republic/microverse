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
package com.airepublic.microverse.rest.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * REST Services Application registry.
 *
 * @author Torsten Oltmanns
 *
 */
@ApplicationPath("/")
public class ServiceServerApplication extends Application {
	private final Set<Object> singletons = new HashSet<>();
	private final Set<Class<?>> classes = new HashSet<>();


	public ServiceServerApplication() {
		classes.add(RestServiceServer.class);
	}


	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}


	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
