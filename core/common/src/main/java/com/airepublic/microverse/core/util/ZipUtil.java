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
package com.airepublic.microverse.core.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to perform operations on a Zip file.
 *
 * @author Torsten Oltmanns
 *
 */
public class ZipUtil {
	private static final Logger LOG = LoggerFactory.getLogger(ZipUtil.class);


	/**
	 * Unzips the specified zip file to the specified destination directory. Replaces any files in the destination, if they already exist.
	 *
	 * @param zipFilename the name of the zip file to extract
	 * @param destFilename the directory to unzip to
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static List<URL> unzip(final URI zipFile, final Path targetPath) throws IOException, URISyntaxException {
		// if the destination doesn't exist, create it
		LOG.debug("Unzipping: " + zipFile.toString());
		if (Files.notExists(targetPath)) {
			LOG.debug(targetPath + " does not exist. Creating...");
			Files.createDirectories(targetPath);
		}

		try (FileSystem zipFileSystem = createZipFileSystem(zipFile, false)) {
			final List<URL> urls = new ArrayList<>();
			final Path root = zipFileSystem.getPath("/");

			// walk the zip file tree and copy files to the destination
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					final Path destFile = Paths.get(targetPath.toString(), file.toString());
					urls.add(destFile.toUri().toURL());
					LOG.debug("Extracting file " + file + " to " + destFile.toAbsolutePath());
					Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.CONTINUE;
				}


				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
					final Path dirToCreate = Paths.get(targetPath.toString(), dir.toString());
					if (Files.notExists(dirToCreate)) {
						LOG.debug("Creating directory " + dirToCreate.toAbsolutePath());
						Files.createDirectory(dirToCreate);
					}
					return FileVisitResult.CONTINUE;
				}
			});

			return urls;
		}
	}


	/**
	 * Returns a zip file system
	 *
	 * @param zipFile to construct the file system from
	 * @param create true if the zip file should be created
	 * @return a zip file system
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private static FileSystem createZipFileSystem(final URI zipFile, final boolean create) throws IOException, URISyntaxException {
		// convert the filename to a URI
		System.out.println("Create ZipFS: " + zipFile + " -> " + zipFile.getPath());
		final String zipPath = zipFile.getPath();

		URI uri = null;

		if (zipPath == null) {
			uri = zipFile;
		} else {
			uri = URI.create("jar:file:" + zipPath);
		}

		System.out.println("Create ZipFS: using " + uri);

		final Map<String, String> env = new HashMap<>();

		if (create) {
			env.put("create", "true");
		}
		return FileSystems.newFileSystem(uri, env);
	}

}
