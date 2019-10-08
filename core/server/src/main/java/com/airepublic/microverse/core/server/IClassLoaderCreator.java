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
package com.airepublic.microverse.core.server;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Interface to create a custom {@link URLClassLoader}.
 *
 * @author Torsten Oltmanns
 *
 */
public interface IClassLoaderCreator {

	/**
	 * Create a custom {@link URLClassLoader} to use for service-container creation.
	 *
	 * @param classpathURLs the URLs representing the classpath
	 * @return the {@link URLClassLoader}
	 */
	URLClassLoader createClassLoader(URL... classpathURLs);
}
