/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 indexing logic
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index;

import static ch.mlutz.plugins.t4e.tools.EclipseTools.openFileInEditor;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isAppSpecification;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isComponentSpecification;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isHtmlFile;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isJavaFile;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isPageSpecification;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;

import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryException;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tools.EclipseTools;

/**
 * Contains the logic to index Tapestry4 projects and stores file relations
 * into the associated index. Should not contain any storage of any relations.
 *
 * @author Marcel Lutz
 */
public class TapestryIndexer {

	private static final int PROGRESS_MONITOR_MULTIPLIER= 100;
	private static final int DEBUG_SLEEP_MS= 0;

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			TapestryIndexer.class);

	/**
	 * the Tapestry index storing the file relations
	 */
	private TapestryIndex tapestryIndex;

	public TapestryIndexer(TapestryIndex tapestryIndex) {
		this.tapestryIndex= tapestryIndex;
	}


	private void checkForProgressMonitorCancel() {
		/* check for abort update by user
		if (progressMonitor != null && progressMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		*/
	}

	/**
	 * Takes a resource and updates all lists and maps for the enclosing
	 * project.
	 *
	 * @param enclosedResource a random resource whose whole project tree
	 * 			will be updated
	 * @throws CoreException
	 */
	public void update(IResource enclosedResource) throws CoreException {
		checkForProgressMonitorCancel();
		if (enclosedResource instanceof IFile) {
			update((IFile) enclosedResource);
		} else if (enclosedResource instanceof IContainer) {
			update((IContainer) enclosedResource);
		} else if (enclosedResource instanceof IWorkspaceRoot) {
			update((IWorkspaceRoot) enclosedResource);
		} else {
			// unknown instance/interface
			/* logMessage("Unknown instance with interface IResource: "
					+ enclosedResource.getClass()); */
		}
	}

	private void update(IWorkspaceRoot workspaceRoot) throws CoreException {
		checkForProgressMonitorCancel();
		IProject[] projects= workspaceRoot.getProjects();
		for (IProject p: projects) {
			update(p);
		}
	}

	private void update(IContainer container) throws CoreException {
		checkForProgressMonitorCancel();
		IResource[] members= container.members();

		// omit folder named "target"
		// TODO: have to solve this more elegantly, e.g. by use of priorities
		if ("target".equals(container.getName())
				&& container.getType() == IFile.FOLDER
				&& container.getParent().getType() == IFile.PROJECT) {
			return;
		}

		if (container.getType() == IFile.PROJECT) {
			System.out.println("Updating project: " + container.getName());
		}

		for (IResource member: members) {
			update(member);
		}
	}

	public void update(IContainer project, IProgressMonitor progressMonitor)
			throws CoreException {

		assert progressMonitor != null : "progressMonitor can't be null";

		try {
			checkForProgressMonitorCancel();
			IResource[] members= project.members();

			// omit folder named "target"
			// TODO: have to solve this more elegantly, e.g. by use of priorities
			if ("target".equals(project.getName()) && project.getType() == IFile.FOLDER
					&& project.getParent().getType() == IFile.PROJECT) {
				return;
			}

			if (project.getType() == IFile.PROJECT) {
				System.out.println("Updating project: " + project.getName());
			}

			progressMonitor.beginTask("Update Tapestry Index",
					members.length * PROGRESS_MONITOR_MULTIPLIER);

			for (IResource member: members) {
				try {
					Thread.sleep(DEBUG_SLEEP_MS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				update(member);
				progressMonitor.worked(PROGRESS_MONITOR_MULTIPLIER);
			}
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Handle updating a file.
	 *
	 * @param file the file to update
	 */
	private void update(IFile file) {
		checkForProgressMonitorCancel();

		EclipseTools.logMessage("Updating file: " + file.getName());

		try {
			if (isHtmlFile(file)) {
				tapestryIndex.handleHtmlFile(file);
			} else if (isComponentSpecification(file)) {
				tapestryIndex.handleComponentSpecification(file);
			} else if (isPageSpecification(file)) {
				tapestryIndex.handlePageSpecification(file);
			} else if (isAppSpecification(file)) {
				tapestryIndex.handleAppSpecification(file);
			}
			/* DEBUG
			else if (TapestryTools.isJavaFile(file)) {
				if ("Home2.java".equals(file.getName())) {
					EclipseTools.logMessage("Home2.java found.");
					try
					{
						file.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
						createMarkerForResource(file, IMarker.TASK, 20, "TODO fix this problem.");
						createMarkerForResource(file, IMarker.PROBLEM, 21, "There is a PROBLEM here.", IMarker.SEVERITY_ERROR);
						createMarkerForResource(file, IMarker.PROBLEM, 22, "There is a WARNING here.", IMarker.SEVERITY_WARNING);
						createMarkerForResource(file, IMarker.PROBLEM, 23, "INFO for source here.", IMarker.SEVERITY_INFO);
						createMarkerForResource(file, IMarker.BOOKMARK, 24, "Just a random BOOKMARK");
						createMarkerForResource(file, "ch.mlutz.plugins.t4e.helloplugin.problemmarker", 25, "custom problemmarker", IMarker.SEVERITY_WARNING);
					}
					catch(CoreException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			*/
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		try {
			if (file.getContentDescription() == null) {
				if (UPDATE_VERBOSE) {
					System.out.println("File " + file.getFullPath() + " " + file.getName() + ": getContentDescription() is null");
				}
				if (isAppSpecification(file)) {
					handleAppSpecification(file);
				}
			} else if (file.getContentDescription().getContentType() == null) {
				if (UPDATE_VERBOSE) {
					System.out.println("File " + file.getFullPath() + " " + file.getName() + ": getContentType() is null");
				}
			} else if (file.getContentDescription().getContentType().getName() == null) {
				if (UPDATE_VERBOSE) {
					System.out.println("File " + file.getFullPath() + " " + file.getName() + ": getName() is null");
				}
			} else {
				if (isHtmlFile(file)) {
					if (UPDATE_VERBOSE) {
						System.out.println("File " + file.getFullPath() + " " + file.getName() + ": HTML");
					}
					handleHtmlFile(file);
				} else if (UPDATE_VERBOSE) {
					System.out.println("File " + file.getFullPath() + " " + file.getName() + ": " + file.getContentDescription().getContentType().getId());
				}
			}
		} catch (CoreException e) {
			System.out.println("CoreException when handling file has occurred: " + e.getMessage());
		}
		*/
	}

	/**
	 * Iterates all folders and looks for *.specification files in a folder
	 * named WEB-INF.
	 *
	 * @param project
	 * @return
	 */
	public List<TapestryModule> createModulesForProject(IProject project) {

		// the queue storing the potential app specification files
		Queue<IFile> appSpecificationFiles= new LinkedList<IFile>();

		/*
		// add all children folders of project
		for (IResource member: project.members()) {
			if (member.getType() == IResource.FOLDER) {
				folderQueue.add((IFolder) member);
			}
		}
		*/

		// the queue used for breadth first search
		Queue<IContainer> containerQueue= new LinkedList<IContainer>();
		containerQueue.add(project);

		IContainer currentContainer;
		while ((currentContainer= containerQueue.poll()) != null) {

			try {
				if (!TapestryModule.WEB_INF_FOLDER_NAME.equals(
						currentContainer.getName())) {

					// add all children folders of project
					for (IResource member: currentContainer.members()) {
						if (member.getType() == IResource.FOLDER) {
							containerQueue.add((IContainer) member);
						}
					}
				} else {
					// add all children folders of project and check files for
					// specification
					for (IResource member: currentContainer.members()) {
						if (member.getType() == IResource.FOLDER) {
							containerQueue.add((IContainer) member);
						} else if (member.getType() == IResource.FILE
								&& isAppSpecification((IFile) member)) {
							appSpecificationFiles.add((IFile) member);
						}
					}

				}
			} catch (CoreException e) {
				log.warn("Couldn't iterate container " + currentContainer.getName(), e);
			}
		}	// while

		List<TapestryModule> result= new ArrayList<TapestryModule>();

		IFile currentFile;
		while ((currentFile= appSpecificationFiles.poll()) != null) {

			try {
				TapestryModule tapestryModule= new TapestryModule(currentFile);
				result.add(tapestryModule);
			} catch (TapestryException e) {
				log.warn("Couldn't validate tapestryModule for app specification"
						+ " file " + currentFile.getName() + " in project "
						+ project.getName() + ": ", e);
			}
		}

		return result;
	}

	/*
	 * NEW INFRASTRUCTURE
	 *
	 */
	public void addProjectToIndex(IProject project) {
		addProjectToIndex(project, null, null);
	}

	public void addProjectToIndex(IProject project, IFile switchToRelatedFile,
			IWorkbenchPage activePage) {
		if (tapestryIndex.contains(project)) {
			return;
		}

		final IProject projectToAdd= project;
		final List<TapestryModule> modulesToAdd= createModulesForProject(project);
		final IFile finalSwitchToRelatedFile= switchToRelatedFile;
		final IWorkbenchPage finalActivePage= activePage;

		Job job = new Job("Add project " + project.getName() + " to Tapestry index") {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {
					int totalWork= 100;
					for (TapestryModule module: modulesToAdd) {
						totalWork+= module.getScanAndUpdateWork();
					}

					monitor.beginTask(this.getName(), totalWork);

					tapestryIndex.add(projectToAdd);

					monitor.worked(100);

					for (TapestryModule module: modulesToAdd) {
						tapestryIndex.add(module);
						module.scanAndUpdateIndex(monitor);
					}

					if (finalSwitchToRelatedFile != null
							&& finalActivePage != null) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								IFile target= getRelatedFile(
									finalSwitchToRelatedFile);
								switchToFile(target, finalActivePage);
							}
						});
					}
				/*
				} catch (CoreException e) {
					log.error("Could not add project " + projectToAdd.getName(), e);
				}
				*/
				} catch(TapestryException e) {
					log.error("Could not add project " + projectToAdd.getName(), e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setPriority(Job.SHORT);
		job.schedule(); // start as soon as possible
	}

	public void removeProjectFromIndex(IProject project) {
		if (!tapestryIndex.contains(project)) {
			return;
		}

		Set<TapestryModule> modules= tapestryIndex.getModules();
		List<TapestryModule> toRemove=
				tapestryIndex.getModulesForProject(project);

		for (TapestryModule module: toRemove) {
			tapestryIndex.remove(module);
		}
	}


	/**
	 * @param target
	 * @param activePage
	 */
	public void switchToFile(IFile target, IWorkbenchPage activePage) {
		try
		{
			if (isHtmlFile(target)) {
				openFileInEditor(target, activePage, Constants.TAPESTRY_EDITOR_ID);
			} else {
				openFileInEditor(target, activePage);
			}
		}
		catch(CoreException e)
		{
			log.error("Couldn't open file " + target.getName() + " in editor.", e);
		}
	}

	/**
	 * ... If multiple targets for file exist, returns just the first one
	 *
	 * @param file
	 * @return
	 */
	public synchronized IFile getRelatedFile(IFile file) {
		IFile target= null;
		Set<IFile> toFiles= tapestryIndex.getRelatedFiles(file);
		if (toFiles.size() > 0) {
			for (IFile toFile: toFiles) {

				// use first file as default
				if (target == null) {
					target= toFile;
				}

				// prefer html/java files
				try {
					if (isHtmlFile(toFile) || isJavaFile(toFile)) {
						target= toFile;
						break;
					}
				} catch(CoreException e) {
					log.warn("Couldn't check toFile if it is Html or Java.", e);
				}
			}
		}
		return target;
	}
}
