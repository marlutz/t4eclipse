/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - T4E Classpath container
 ******************************************************************************/
package ch.mlutz.plugins.t4e.container;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * T4eClasspathContainer
 *
 * @author Marcel Lutz
 */
public class T4eClasspathContainer implements IClasspathContainer {
	private final IClasspathEntry[] entries;
	private final IPath path;

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			T4eClasspathContainerInitializer.class);

	public T4eClasspathContainer() {
		this.path=		new Path(Constants.CONTAINER_ID);
		this.entries=	new IClasspathEntry[]{};
	}

	public T4eClasspathContainer(IPath path, IClasspathEntry[] entries) {
		this.path = path;
		IClasspathEntry[] e = new IClasspathEntry[entries.length];
		System.arraycopy(entries, 0, e, 0, entries.length);
		Arrays.sort( e, new Comparator() {
			public int compare( Object o1, Object o2) {
				return o1.toString().compareTo( o2.toString());
			}
		} );
		this.entries = e;
	}

	public T4eClasspathContainer(IPath path, Set entrySet) {
		this(path, (IClasspathEntry[])
				entrySet.toArray(new IClasspathEntry[entrySet.size()]));
	}

	public synchronized IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	public String getDescription() {
		return "Maven Recursive Dependencies";  // TODO move to properties
	}

	public int getKind() {
		// IClasspathContainer.K_SYSTEM
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return path;
	}

	public static String getJavaDocUrl(String fileName) {
		try {
			URL fileUrl = new File(fileName).toURL();
			return "jar:"+fileUrl.toExternalForm()+"!/"+T4eClasspathContainer.getJavaDocPathInArchive(fileName);
		} catch(MalformedURLException ex) {
			return null;
		}
	}

	private static String getJavaDocPathInArchive(String name) {
		long l1 = System.currentTimeMillis();
		ZipFile jarFile = null;
		try {
			jarFile = new ZipFile(name);
			String marker = "package-list";
			for(Enumeration<? extends ZipEntry> en = jarFile.entries(); en.hasMoreElements();) {
				ZipEntry entry = en.nextElement();
				String entryName = entry.getName();
				if(entryName.endsWith(marker)) {
					return entry.getName().substring(0, entryName.length()-marker.length());
				}
			}
		} catch(IOException ex) {
			// ignore
		} finally {
			long l2 = System.currentTimeMillis();
			log.info("Scanned javadoc " + name + " " + (l2-l1)/1000f);
			try {
				if(jarFile!=null) jarFile.close();
			} catch(IOException ex) {
				//
			}
		}

		return "";
	}

}

