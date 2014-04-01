/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 application specification logic
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry;

/**
 * @author Marcel Lutz
 * @version 1.0 12.02.2014
 */

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.serializer.EclipseSerializer;
import ch.mlutz.plugins.t4e.tapestry.parsers.AppSpecificationParser;

public class AppSpecification implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -8635671905282053186L;

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			AppSpecification.class);

	private Set<String> componentClassPackages= new HashSet<String>();

	private Set<String> pageClassPackages= new HashSet<String>();

	private transient IFile appSpecificationFile;

	private transient AppSpecificationParser appSpecificationParser=
			new AppSpecificationParser();

	public AppSpecification(IFile appSpecificationFile) {
		this.appSpecificationFile= appSpecificationFile;
		parseSpecification();
	}

	public Set<String> getComponentClassPackages() {
		return componentClassPackages;
	}

	public Set<String> getPageClassPackages() {
		return pageClassPackages;
	}

	/**
	 *
	 * @return true if app specification has been parsed successfully; false
	 *     otherwise
	 */
	private boolean parseSpecification() {

		// parse app specification and extract data
		try {
			appSpecificationParser.clear();
			appSpecificationParser.parse(appSpecificationFile.getContents());
		} catch(UnsupportedEncodingException e) {
			// log warning, ignore file, don't apply any changes
			log.warn("Could not parse app specification.", e);
			return false;
		} catch(CoreException e)  {
			// log warning, ignore file, don't apply any changes
			log.warn("Could not parse app specification.", e);
			return false;
		}

		componentClassPackages= new HashSet<String>(Arrays.asList(
				appSpecificationParser.getComponentClassPackages()));

		pageClassPackages= new HashSet<String>(Arrays.asList(
				appSpecificationParser.getPageClassPackages()));

		return true;
	}

	public boolean update() {
		return parseSpecification();
	}

	public IFile getAppSpecificationFile() {
		return appSpecificationFile;
	}

	public void setAppSpecificationFile(IFile appSpecificationFile) {
		this.appSpecificationFile= appSpecificationFile;
	}

	// serialization
	private void writeObject(java.io.ObjectOutputStream stream)
			throws IOException, JavaModelException {
		stream.defaultWriteObject();
		EclipseSerializer.serializeResource(stream, appSpecificationFile);
	}

	private void readObject(java.io.ObjectInputStream stream)
			throws IOException, ClassNotFoundException {
		stream.defaultReadObject();

		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		EclipseSerializer.deserializeResource(stream, workspaceRoot,
				IFile.class);

		appSpecificationParser= new AppSpecificationParser();
	}
}
