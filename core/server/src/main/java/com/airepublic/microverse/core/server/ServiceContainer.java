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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microverse.core.descriptor.ServiceDescriptor;
import com.airepublic.microverse.core.exception.ServiceException;

/**
 * The container to run the service independently from the other classloaders.
 *
 * @author Torsten Oltmanns
 *
 */
public class ServiceContainer implements Closeable {
	private final static Logger LOG = LoggerFactory.getLogger(ServiceContainer.class);

	private URLClassLoader classLoader;
	private ServiceDescriptor serviceDescriptor;
	private Object service;
	private final Path containerDirectory;
	private final Object syncClassloading = new Object();


	/**
	 * Creates a {@link ServiceContainer} for the specified {@link ServiceDescriptor} by decoding
	 * all files in the service-bundle zip and creating the service.
	 *
	 * @param serviceDescriptor the {@link ServiceDescriptor}
	 * @param serviceBundleZip the service-bundle zip
	 * @param serviceDir the root directory where the container directory will be created
	 * @param classLoaderCreator the classloader
	 * @return the {@link ServiceContainer}
	 * @throws ServiceException
	 */
	public static ServiceContainer create(final ServiceDescriptor serviceDescriptor, final byte[] serviceBundleZip, final Path serviceDir, final IClassLoaderCreator classLoaderCreator) throws ServiceException {
		final Path containerDirectory = serviceDir.resolve(serviceDescriptor.getId() + "-" + serviceDescriptor.getVersion() + "_" + System.nanoTime());

		try {
			// create service-container directory
			Files.createDirectories(containerDirectory);
		} catch (final Exception e) {
			throw new ServiceException("Error serializing service-descriptor to service-container directory: " + containerDirectory, e);
		}

		// write the service-libraries to the service-container directory for
		// the service to load it via its own classloader
		final URL[] classpathURLs = writeServiceLibraries(containerDirectory, serviceBundleZip);
		final URLClassLoader classLoader = classLoaderCreator.createClassLoader(classpathURLs);

		return new ServiceContainer(containerDirectory, serviceDescriptor, classLoader);
	}


	/**
	 * Creates the service-lib directory and writes all libraries which are needed by the service.
	 *
	 *
	 * @param libDir the path to the service-lib directory to create
	 * @param serviceBundleZip the bytes of the service-bundle zip
	 * @throws ServiceException
	 */
	protected static URL[] writeServiceLibraries(final Path libDir, final byte[] serviceBundleZip) throws ServiceException {
		// write jars and set lib dir
		final List<URL> urls = new ArrayList<URL>();

		if (serviceBundleZip != null) {
			try {
				// write bundle libs
				ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(serviceBundleZip));
				ZipEntry entry;

				while ((entry = zis.getNextEntry()) != null) {
					final String entryName = entry.getName();
					final Path targetPath = libDir.resolve(entryName);
					final FileOutputStream out = new FileOutputStream(targetPath.toFile());
					final byte[] byteBuff = new byte[4096];
					int bytesRead = 0;

					while ((bytesRead = zis.read(byteBuff)) != -1) {
						out.write(byteBuff, 0, bytesRead);
					}

					out.close();
					zis.closeEntry();

					if (entryName.toLowerCase().endsWith(".jar")) {
						urls.add(targetPath.toUri().toURL());
					}
				}

				zis.close();

				// write servicecontainer jars
				zis = new ZipInputStream(ServiceContainer.class.getClassLoader().getResourceAsStream("servicecontainer.zip"));

				while ((entry = zis.getNextEntry()) != null) {
					final String entryName = entry.getName();
					final Path targetPath = libDir.resolve(entryName);
					final FileOutputStream out = new FileOutputStream(targetPath.toFile());
					final byte[] byteBuff = new byte[4096];
					int bytesRead = 0;

					while ((bytesRead = zis.read(byteBuff)) != -1) {
						out.write(byteBuff, 0, bytesRead);
					}

					out.close();
					zis.closeEntry();

					if (entryName.toLowerCase().endsWith(".jar")) {
						urls.add(targetPath.toUri().toURL());
					}
				}

				zis.close();

			} catch (final IOException e) {
				LOG.error("Error writing servicecontainer JARs to service lib-directory " + libDir, e);
				throw new ServiceException("Error writing servicecontainer JARs to service lib-directory " + libDir, e);
			}
		}

		return urls.toArray(new URL[urls.size()]);
	}


	private ServiceContainer(final Path containerDirectory, final ServiceDescriptor serviceDescriptor, final URLClassLoader classLoader) throws ServiceException {
		this.containerDirectory = containerDirectory;
		this.classLoader = classLoader;

		// create the service via its own classloader
		try {
			synchronized (syncClassloading) {
				final ClassLoader orig = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(classLoader);
				service = classLoader.loadClass("com.airepublic.microverse.core.service.Service").getConstructor(byte[].class).newInstance(SerializationUtils.serialize(serviceDescriptor));
				Thread.currentThread().setContextClassLoader(orig);
			}

			this.serviceDescriptor = SerializationUtils.deserialize((byte[]) service.getClass().getMethod("getSerializedServiceDescriptor", new Class[] {}).invoke(service, new Object[] {}));
		} catch (final Exception e) {
			try {
				// close to release resources and delete container-directory
				close();
			} catch (final Exception e1) {
				// nothing to do
			}

			throw new ServiceException("Error creating service instance: " + serviceDescriptor, e);
		}
	}


	public final ServiceDescriptor getServiceDescriptor() {
		return serviceDescriptor;
	}


	public final void setServiceDescriptor(final ServiceDescriptor serviceDescriptor) {
		this.serviceDescriptor = serviceDescriptor;
	}


	public final Object getService() {
		return service;
	}


	@Override
	public synchronized void close() {
		try {
			if (service != null) {
				service.getClass().getMethod("close", new Class[] {}).invoke(service, new Object[] {});
			}
		} catch (final Exception e) {
			LOG.error("Error closing service: " + serviceDescriptor, e);
		}

		service = null;

		try {
			classLoader.close();
		} catch (final Exception e) {
			LOG.error("Service-ClassLoader could not be closed cleanly!", e);
		}
		classLoader = null;

		System.gc();

		// delete service-container directory
		try {
			deleteContainerDirectory();
		} catch (final Exception e) {
			LOG.error("Could not delete service-container directory: " + containerDirectory, e);
		}
	}


	/**
	 * Deletes the service container-directory.
	 *
	 * @throws IOException
	 */
	private void deleteContainerDirectory() {
		try {
			Files.walkFileTree(containerDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					try {
						Files.delete(file);
						LOG.debug("   deleted: " + file);
					} catch (final Exception e) {
						LOG.error("File '" + file + "' could not be deleted!", e);
					}
					return FileVisitResult.CONTINUE;
				}


				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException {
					if (ex == null) {
						try {
							Files.delete(dir);
							LOG.debug("   deleted: " + dir);
						} catch (final Exception e) {
							LOG.error("File '" + dir + "' could not be deleted!", e);
						}
						return FileVisitResult.CONTINUE;
					} else {
						// directory iteration failed
						throw ex;
					}
				}

			});
		} catch (final Exception e) {
			// TODO: handle exception
		}
	}
}
