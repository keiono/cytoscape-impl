package org.cytoscape.io.internal.read;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.work.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericReaderManager<T extends InputStreamTaskFactory, R extends Task> {

	private static final Logger logger = LoggerFactory.getLogger(GenericReaderManager.class);

	// This is a HACK! We need re-design this filtering mechanism.
	private static final String DEFAULT_READER_FACTORY_CLASS = "org.cytoscape.tableimport.internal.ImportNetworkTableReaderFactory";
	
	protected final DataCategory category;
	protected final StreamUtil streamUtil;

	protected final Set<T> factories;

	public GenericReaderManager(final DataCategory category, final StreamUtil streamUtil) {
		this.category = category;
		this.streamUtil = streamUtil;

		factories = new HashSet<T>();
	}

	/**
	 * Listener for OSGi service
	 * 
	 * @param factory
	 * @param props
	 */
	public void addInputStreamTaskFactory(T factory, @SuppressWarnings("rawtypes") Map props) {
		if (factory == null)
			logger.warn("Specified factory is null.");
		else if (factory.getFileFilter().getDataCategory() == category) {
			logger.debug("adding IO taskFactory (factory = " + factory + ", category = " + category + ")");
			factories.add(factory);
		}
	}

	/**
	 * Listener for OSGi service
	 * 
	 * @param factory
	 * @param props
	 */
	public void removeInputStreamTaskFactory(T factory, @SuppressWarnings("rawtypes") Map props) {
		factories.remove(factory);
	}

	/**
	 * Gets the GraphReader that is capable of reading the specified file.
	 * 
	 * @param uri
	 *            URI of file to be read.
	 * @return GraphReader capable of reading the specified file. Null if file
	 *         cannot be read.
	 */
	public R getReader(final URI uri, final String inputName) {

		// Data location is always required.
		if (uri == null) {
			throw new NullPointerException("Data source URI is null");
		}

		// This is the default reader, which accepts files with no extension.
		// Usually, this is ImportNetworkTableReaderFactory (Manual table
		// import)
		T defaultFactory = null;

		final List<T> factoryList = new ArrayList<T>();
		final Map<String, T> factoryTable = new HashMap<String, T>();

		// Pick compatible reader factories.
		for (final T factory : factories) {
			final CyFileFilter cff = factory.getFileFilter();
			// Got "Accepted" flag. Need to check it's default or not.
			if (cff.accepts(uri, category)) {
				logger.info("Filter returns Accepted.  Need to check priority: " + factory);
				if(factory.getClass().toString().contains(DEFAULT_READER_FACTORY_CLASS)) {
					defaultFactory = factory;
				} else {
					factoryList.add(factory);
					for (final String extension : cff.getExtensions()) {
						factoryTable.put(extension, factory);
					}
				}
			}
		}

		T chosenFactory = null;

		// No compatible factory is available.
		if (factoryTable.isEmpty() && defaultFactory == null) {
			logger.warn("No reader found for uri: " + uri.toString());
			throw new NullPointerException("Could not find reader.");
		} else if(factoryList.size() == 1) {
			// There is only one compatible reader factory.  Use it:
			chosenFactory = factoryList.get(0);
		} else {
			if(factoryList.isEmpty() && defaultFactory != null) {
				// There is only one factory
				chosenFactory = defaultFactory;
			} else {
				// Well, we cannot decide which one is correct.  Try to use ext...
				String extension = getExtension(uri.toString());
				if (factoryTable.containsKey(extension))
					chosenFactory = factoryTable.get(extension);
				else {
					if (factoryTable.containsKey(""))
						chosenFactory = factoryTable.get("");
					else
						throw new NullPointerException("Could not find reader factory.");
				}
			}
		}

		try {
			logger.info("Successfully found compatible ReaderFactory " + chosenFactory);
			// This returns strean using proxy if it exists.
			InputStream stream = streamUtil.getInputStream(uri.toURL());
			if (!stream.markSupported()) {
				stream = new BufferedInputStream(stream);
			}
			return (R) chosenFactory.createTaskIterator(stream, inputName).next();

		} catch (IOException e) {
			logger.warn("Error opening stream to URI: " + uri.toString(), e);
			throw new IllegalStateException("Could not open stream for reader.", e);
		}
	}

	private final String getExtension(String filename) {
		if (filename != null) {
			int i = filename.lastIndexOf('.');

			if ((i > 0) && (i < (filename.length() - 1))) {
				return filename.substring(i + 1).toLowerCase();
			}
			if (i == -1)
				return "";
		}
		return null;
	}

	public R getReader(InputStream stream, String inputName) {
		try {
			if (!stream.markSupported()) {
				stream = new BufferedInputStream(stream);
				stream.mark(1025);
			}

			for (T factory : factories) {
				CyFileFilter cff = factory.getFileFilter();
				logger.debug("trying READER: " + factory + " with filter: " + cff);

				// Because we don't know who will provide the file filter or
				// what they might do with the InputStream, we provide a copy
				// of the first 2KB rather than the stream itself.
				if (cff.accepts(CopyInputStream.copyKBytes(stream, 1), category)) {
					logger.debug("successfully matched READER " + factory);
					return (R) factory.createTaskIterator(stream, inputName).next();
				}
			}
		} catch (IOException ioe) {
			logger.warn("Error setting input stream", ioe);
		}

		logger.warn("No reader found for input stream");
		return null;
	}
}
