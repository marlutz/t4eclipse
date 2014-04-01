/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Classpath container handler
 ******************************************************************************/
package ch.mlutz.plugins.t4e.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import ch.mlutz.plugins.t4e.constants.Command;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tools.MavenTools;

/**
 * This handler handles adding the t4e classpath container.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class ClasspathContainerHandler extends AbstractHandler {

	public static final IPath CONTAINER_PATH= new Path(Constants.CONTAINER_ID);

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			ClasspathContainerHandler.class);

	/**
	 * This execute handles refresh and refresh all commands
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (event.getCommand().getId().equals(Command.ADD_CLASSPATH_CONTAINER)) {
			return onAddClasspathContainer(event);
		} else if (event.getCommand().getId().equals(Command.REMOVE_CLASSPATH_CONTAINER)) {
			return onRemoveClasspathContainer(event);
		}

		return null;
	}

	private Object onAddClasspathContainer(ExecutionEvent event) throws ExecutionException {

		// boolean oldValue = HandlerUtil.toggleCommandState(event.getCommand());

		String localRepositoryDir= MavenTools.getMavenLocalRepoPath();
		log.info("LocalRepositoryDir: " + localRepositoryDir);

		/*
			  try {
				JavaCore.setClasspathContainer(containerSuggestion.getPath(), new IJavaProject[] {project},
					new IClasspathContainer[] {new Maven2ClasspathContainer(containerPath, bundleUpdater.newEntries)}, null);
			  } catch(JavaModelException ex) {
				Maven2Plugin.getDefault().getConsole().logError(ex.getMessage());
			  }
		 */

		// get workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		// set selection service
		ISelectionService service = window.getSelectionService();
		// set structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();

		//check if it is an IFile
		Object el= structured.getFirstElement();

		if (el instanceof IJavaProject) {

			IJavaProject javaProject= (IJavaProject) el;

			// Create new Classpath Container Entry
			IClasspathEntry containerEntry = JavaCore.newContainerEntry(
					CONTAINER_PATH);

			// Initialize Classpath Container
			ClasspathContainerInitializer containerInit =
					JavaCore.getClasspathContainerInitializer(Constants.CONTAINER_ID);
			try {
				containerInit.initialize(new Path(Constants.CONTAINER_ID), javaProject);

				// Set Classpath of Java Project
				List<IClasspathEntry> projectClassPath = new ArrayList<IClasspathEntry>(
						Arrays.asList(javaProject.getRawClasspath()));
				projectClassPath.add(containerEntry);
				javaProject.setRawClasspath(projectClassPath.toArray(new IClasspathEntry[projectClassPath.size()]), null);
			} catch (CoreException e) {
				log.error("Could not add Classpath container: ", e);
			}

			/*
			IClasspathEntry varEntry = JavaCore.newContainerEntry(
					new Path("JDKLIB/default"), // container 'JDKLIB' + hint 'default'
					false); //not exported

			try {
				JavaCore.setClasspathContainer(
						new Path("JDKLIB/default"),
						new IJavaProject[]{ (IJavaProject) el }, // value for 'myProject'
						new IClasspathContainer[] {
							new IClasspathContainer() {
								public IClasspathEntry[] getClasspathEntries() {
									return new IClasspathEntry[]{
											JavaCore.newLibraryEntry(new Path("d:/rt.jar"), null, null, false)
									};
								}
								public String getDescription() { return "Basic JDK library container"; }
								public int getKind() { return IClasspathContainer.K_SYSTEM; }
								public IPath getPath() { return new Path("JDKLIB/basic"); }
							}
						},
						null);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
		return null;
	}


	private Object onRemoveClasspathContainer(ExecutionEvent event) throws ExecutionException {
		// get workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		// set selection service
		ISelectionService service = window.getSelectionService();
		// set structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();

		//check if it is an IFile
		Object el= structured.getFirstElement();

		if (el instanceof IJavaProject) {

			IJavaProject javaProject= (IJavaProject) el;

			try {
				// Set Classpath of Java Project
				List<IClasspathEntry> projectClassPath = new ArrayList<IClasspathEntry>(
						Arrays.asList(javaProject.getRawClasspath()));

				IClasspathEntry entry;
				for (int i= projectClassPath.size()-1; i >= 0; --i) {
					entry= projectClassPath.get(i);
					if (entry.getPath().equals(CONTAINER_PATH)) {
						projectClassPath.remove(i);
					}
				}

				javaProject.setRawClasspath(projectClassPath.toArray(new IClasspathEntry[projectClassPath.size()]), null);
			} catch (CoreException e) {
				log.error("Could not remove Classpath container: ", e);
			}
		}
		return null;
	}
}
