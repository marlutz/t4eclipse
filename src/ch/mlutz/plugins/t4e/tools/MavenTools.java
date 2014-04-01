/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Maven tools
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools;

/**
 * @author Marcel Lutz
 * @version 1.0 25.02.2014
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

public class MavenTools {
	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
		MavenTools.class);

	public static String getMavenLocalRepoPath() {
		IMaven maven= MavenPlugin.getMaven();

		Object artifactRepository= null;
		try {
			artifactRepository= maven.getLocalRepository();
		} catch(CoreException e) {
		   log.error("Could not get artifactRepository. ", e);
		   return null;
		}

		if (artifactRepository == null) {
			log.warn("maven.getLocalRepository() returned null.");
			return null;
		}

		Method getBasedirMethod= null;
		try {
			getBasedirMethod = artifactRepository.getClass()
				.getMethod("getBasedir");
		} catch (SecurityException e) {
			log.error("Could not retrieve method ArtifactRepository.getBasedir()", e);
			return null;
		} catch (NoSuchMethodException e) {
			log.error("Could not retrieve method ArtifactRepository.getBasedir()", e);
			return null;
		}

		if (getBasedirMethod == null) {
			log.warn("Class.getMethod(\"getBasedir\") returned null.");
			return null;
		}

		Object localRepositoryDir= null;
		try {
			localRepositoryDir= getBasedirMethod.invoke(artifactRepository);
		} catch (IllegalArgumentException e) {
			log.error("Could not invoke method artifactRepository.getBasedir()", e);
			return null;
		} catch (IllegalAccessException e) {
			log.error("Could not invoke method artifactRepository.getBasedir()", e);
			return null;
		} catch (InvocationTargetException e) {
			log.error("Could not invoke method artifactRepository.getBasedir()", e);
			return null;
		}

		if (!(localRepositoryDir instanceof String)) {
			log.error("Invocation of method artifactRepository.getBasedir() did not return a String");
			return null;
		}

		// success
		return (String) localRepositoryDir;
	}
}
