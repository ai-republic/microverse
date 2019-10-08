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
package com.airepublic.microverse.core.descriptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServiceCreateDescriptorList implements Serializable {
	private static final long serialVersionUID = -3864986145704227335L;
	private final List<ServiceCreateDescriptor> descriptors = new ArrayList<>();
	private String host;
	private int port;
	private boolean useSSL;
	private String contextRoot;


	public static ServiceCreateDescriptorList create(String host, int port, boolean useSSL, String contextRoot) {
		final ServiceCreateDescriptorList d = new ServiceCreateDescriptorList();
		d.host = host;
		d.port = port;
		d.useSSL = useSSL;
		d.contextRoot = contextRoot;

		return d;
	}


	public ServiceCreateDescriptorList add(ServiceCreateDescriptor descriptor) {
		descriptors.add(descriptor);

		return this;
	}


	public ServiceCreateDescriptorList remove(ServiceCreateDescriptor descriptor) {
		descriptors.remove(descriptor);

		return this;
	}


	public List<ServiceCreateDescriptor> getDescriptors() {
		return descriptors;
	}


	public String getHost() {
		return host;
	}


	public void setHost(String host) {
		this.host = host;
	}


	public int getPort() {
		return port;
	}


	public void setPort(int port) {
		this.port = port;
	}


	public boolean getUseSSL() {
		return useSSL;
	}


	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}


	public String getContextRoot() {
		return contextRoot;
	}


	public void setContextRoot(String contextRoot) {
		this.contextRoot = contextRoot;
	}
}
