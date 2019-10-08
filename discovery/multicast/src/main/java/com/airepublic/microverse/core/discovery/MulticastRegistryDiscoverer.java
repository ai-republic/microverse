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
package com.airepublic.microverse.core.discovery;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.common.ServiceUtils;
import com.airepublic.microverse.core.common.marshaller.IMarshaller;
import com.airepublic.microverse.core.common.marshaller.MarshallerFactory;
import com.airepublic.microverse.core.descriptor.RegistryDescriptor;
import com.airepublic.microverse.core.discovery.IRegistryDiscoverer;
import com.airepublic.microverse.core.discovery.RegistryCache;
import com.airepublic.microverse.core.exception.ServiceException;
import com.airepublic.multicast.IMulticastMessageHandler;
import com.airepublic.multicast.MulticastReceiver;
import com.airepublic.multicast.MulticastSender;

/**
 * Discovers service-registries by making a multicast request.
 *
 * @author Torsten Oltmanns
 *
 */
@Singleton
public class MulticastRegistryDiscoverer implements IRegistryDiscoverer, IMulticastMessageHandler, Closeable {
	private final static Logger LOG = LoggerFactory.getLogger(MulticastRegistryDiscoverer.class);
	public final static String REQUEST_REGISTRY_ALL = "REQUEST_REGISTRY_ALL";
	public final static String ANNOUNCE_REGISTRY_ALL = "REGISTRY_ALL";
	private InetAddress multicastGroup;
	private int multicastPort;
	private final IMarshaller marshaller = MarshallerFactory.get("application/octet-stream");
	private MulticastReceiver multicastReceiver;
	private final Object sync = new Object();
	private final ArrayList<RegistryDescriptor> registries = new ArrayList<>();
	private final Timer timer = new Timer();


	/**
	 * Creates a new instance by defining the {@link RegistryCache}.<br/>
	 * NOTE: This is for standalone use when not in a CDI context.
	 *
	 * @param registryCache
	 * @return
	 */
	public static MulticastRegistryDiscoverer create() {
		return new MulticastRegistryDiscoverer();
	}


	/**
	 * Constructor.
	 */
	private MulticastRegistryDiscoverer() {
		// initialize multicast socket
		try {
			multicastGroup = InetAddress.getByName(ServiceUtils.MULTICAST_GROUP_ADDRESS);
			multicastPort = ServiceUtils.MULTICAST_PORT;
			multicastReceiver = MulticastReceiver.create(multicastGroup, multicastPort, 1024000).addMessageHandler(this).start();

			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					sendMulticast();
				}
			}, 0, 60000);
		} catch (final Exception e) {
			LOG.error("Error initializing multicast registry receiver: " + e);
			throw new RuntimeException("Error initializing multicast registry receiver!", e);
		}
	}


	@Override
	public List<RegistryDescriptor> requestRegistries() {
		return registries;
	}


	void sendMulticast() {
		// send request to get all registries
		try {
			synchronized (sync) {
				send(REQUEST_REGISTRY_ALL, null);

				try {
					sync.wait(1000);
				} catch (final InterruptedException e) {
				}
			}

		} catch (final ServiceException e) {
			LOG.error("Could not request all registries on service-discovery startup!", e);
			throw new RuntimeException("Could not request all registries on service-discovery startup!", e);
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public void handle(final InetAddress fromHost, final int fromPort, final byte[] data) {
		// first find the type which is separated by a 0 byte value from the
		// serialized list of RegistryDescriptors
		int i = 0;

		while (i < data.length && data[i] != 0) {
			i++;
		}

		final byte[] typeBytes = new byte[i];
		System.arraycopy(data, 0, typeBytes, 0, i);
		final String type = new String(typeBytes).trim();

		// now read the bytes of the serialized RegistryDescriptors
		final byte[] values = new byte[data.length - i - 1];
		System.arraycopy(data, i + 1, values, 0, values.length);

		try {
			switch (type) {
				case ANNOUNCE_REGISTRY_ALL:
					synchronized (sync) {
						registries.addAll(marshaller.deserialize(values, List.class));
						sync.notify();
					}
					break;
				case REQUEST_REGISTRY_ALL:
					send(ANNOUNCE_REGISTRY_ALL, marshaller.serialize(registries));
					break;
			}
		} catch (final Exception e) {
			LOG.error("Error processing service-discovery message '" + type + "' and sending response!", e);
		}
	}


	/**
	 * Sends the message of the specified type to the multicast socket.
	 *
	 * @param type the type of message
	 * @param message the message data
	 * @throws ServiceException
	 */
	protected void send(final String type, final byte[] message) throws ServiceException {
		try {
			final byte[] typeBytes = (type == null) ? new byte[0] : type.getBytes();
			final byte[] messageBytes = (message == null) ? new byte[0] : message;
			final byte[] data = new byte[typeBytes.length + messageBytes.length + 1];
			System.arraycopy(typeBytes, 0, data, 0, typeBytes.length);
			data[typeBytes.length] = 0;
			System.arraycopy(messageBytes, 0, data, typeBytes.length + 1, messageBytes.length);

			MulticastSender.send(multicastGroup, multicastPort, data);
		} catch (final Exception e) {
			LOG.error("Error sending multicast message '" + type + ((message == null || message.length == 0) ? "" : (" " + message)) + "'", e);
			throw new ServiceException("Error sending multicast message '" + type + ((message == null || message.length == 0) ? "" : (" " + message)) + "'", e);
		}
	}


	@PreDestroy
	@Override
	public void close() {
		try {
			multicastReceiver.close();
		} catch (final IOException e) {
			LOG.error("Could not close multicast service-discovery!", e);
		}
	}
}
