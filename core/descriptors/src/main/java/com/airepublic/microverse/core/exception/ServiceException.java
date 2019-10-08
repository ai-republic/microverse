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
package com.airepublic.microverse.core.exception;

/**
 * General exception-class for the service infrastructure.
 *
 * @author Torsten Oltmanns
 *
 */
public class ServiceException extends Exception {
	private static final long serialVersionUID = 8712900295490840685L;


	/**
	 * Constructor.
	 *
	 * @param msg the message
	 */
	public ServiceException(final String msg) {
		super(msg);
	}

	/**
	 * Constructor.
	 *
	 * @param cause the cause
	 */
	public ServiceException(final Throwable cause) {
		super(cause);
	}


	/**
	 * Constructor.
	 *
	 * @param msg the message
	 * @param cause the cause
	 */
	public ServiceException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

}
