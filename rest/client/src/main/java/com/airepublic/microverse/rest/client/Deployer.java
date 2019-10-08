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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The {@link Deployer} is used to deploy microservices on a RestServiceServer.<br/>
 * USAGE:<br/>
 * Deployer -host=<host> -port=<port> -useSSL=<boolean> -contextRoot=<server-contextRoot> -bundle=
 * <path-to-service-bundle-zip>
 *
 * @author Torsten Oltmanns
 *
 */
public class Deployer {
	static class Arguments {
		String host;
		Integer port;
		Boolean useSSL;
		String contextRoot;
		String bundle;


		@Override
		public String toString() {
			return (useSSL ? "https" : "http") + "://" + host + ":" + port + contextRoot + "  -> " + bundle;
		}
	}


	protected static Arguments checkArguments(final String[] args) throws IOException {
		final Arguments arguments = new Arguments();

		for (final String arg : args) {
			if (arg.startsWith("-host=")) {
				arguments.host = arg.substring("-host=".length()).trim();
			} else if (arg.startsWith("-port=")) {
				arguments.port = Integer.parseInt(arg.substring("-port=".length()).trim());
			} else if (arg.startsWith("-useSSL=")) {
				arguments.useSSL = Boolean.parseBoolean(arg.substring("-useSSL=".length()).trim());
			} else if (arg.startsWith("-contextRoot=")) {
				arguments.contextRoot = arg.substring("-contextRoot=".length()).trim();
			} else if (arg.startsWith("-bundle=")) {
				arguments.bundle = arg.substring("-bundle=".length()).trim();
			}
		}

		if (arguments.host == null) {
			arguments.host = readInput("Host: ");
		}

		if (arguments.port == null) {
			arguments.port = Integer.parseInt(readInput("Port: "));
		}

		if (arguments.useSSL == null) {
			arguments.useSSL = Boolean.parseBoolean(readInput("Use SSL: "));
		}

		if (arguments.contextRoot == null) {
			arguments.contextRoot = readInput("Context-Root: ");

			if (arguments.contextRoot != null && arguments.contextRoot.length() > 0 && arguments.contextRoot.charAt(0) != '/') {
				arguments.contextRoot = "/" + arguments.contextRoot;
			}
		}

		if (arguments.bundle == null) {
			arguments.bundle = readInput("Path to bundle-zip: ");
		}

		return arguments;
	}


	private static String readInput(final String text) throws IOException {
		System.out.print(text);
		final StringBuffer buf = new StringBuffer();
		int chr = System.in.read();

		while (chr != 13) {
			buf.append((char) chr);
			chr = System.in.read();
		}

		return buf.toString().trim();
	}


	public static void deploy(final String host, final int port, final boolean useSSL, final String contextRoot, final Path serviceBundlePath) {
		System.out.println("Trying to deploy service:\n");
		System.out.println("ServiceServer-URL: " + (useSSL ? "https" : "http") + "://" + host + ":" + port + contextRoot);
		System.out.println("Service-bundle: " + serviceBundlePath.toAbsolutePath());
		System.out.print("\nStarting deployment...");

		try {
			RestServiceServerUtil.create(host, port, useSSL, contextRoot).addService(serviceBundlePath);
			System.out.println("done.");
		} catch (final Exception e) {
			System.out.println("failed: ");
			e.printStackTrace();
		}
	}


	public static void undeploy(final String host, final int port, final boolean useSSL, final String contextRoot, final String serviceId, final String serviceVersion) {
		System.out.println("Trying to undeploy service:\n");
		System.out.println("ServiceServer-URL: " + (useSSL ? "https" : "http") + "://" + host + ":" + port + contextRoot);
		System.out.println("Service-id: " + serviceId);
		System.out.println("Service-Version: " + serviceVersion);
		System.out.print("\nStarting deployment...");

		try {
			RestServiceServerUtil.create(host, port, useSSL, contextRoot).removeService(serviceId, serviceVersion);
			System.out.println("done.");
		} catch (final Exception e) {
			System.out.println("failed: ");
			e.printStackTrace();
		}
	}


	public static void main(final String[] args) throws Exception {
		try {
			System.out.println("Checking deployment arguments...");

			final Arguments arguments = checkArguments(args);
			System.out.println("Checking deployment arguments - done!\n");
			deploy(arguments.host, arguments.port, arguments.useSSL, arguments.contextRoot, Paths.get(arguments.bundle));
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println("USAGE:\n\tDeployer -host=<host> -port=<port> -useSSL=<boolean> -contextRoot=<server-contextRoot> -bundle=<path-to-service-bundle-zip>");
			System.exit(-1);

		}
	}
}
