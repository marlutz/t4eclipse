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

import static ch.mlutz.plugins.t4e.tools.EclipseTools.extractFileExtension;
import static ch.mlutz.plugins.t4e.tools.EclipseTools.openFileInEditor;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isAppSpecification;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isComponentSpecification;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isHtmlFileChecked;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isJavaFile;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isPageSpecification;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryException;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryElement;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryHtmlElement;
import ch.mlutz.plugins.t4e.tools.EclipseTools;

/**
 * Contains the logic to index Tapestry4 projects and stores file relations
 * into the associated index. Should not contain any storage of any relations.
 *
 * @author Marcel Lutz
 */
public class TapestryIndexer implements ITapestryModuleChangeListener {

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
    private transient TapestryIndex tapestryIndexStore;

	public TapestryIndexer() {}


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
			update(enclosedResource);
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
				TapestryModule tapestryModule= new TapestryModule(currentFile,
						this);
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
		if (getTapestryIndex().contains(project)) {
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

					getTapestryIndex().add(projectToAdd);

					monitor.worked(100);

					for (TapestryModule module: modulesToAdd) {
						getTapestryIndex().add(module);
						module.scanAndUpdateIndex(monitor);
					}

					if (finalSwitchToRelatedFile != null
							&& finalActivePage != null) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								IFile target= getRelatedFile(
									finalSwitchToRelatedFile);
								if (target != null) {
									switchToFile(target, finalActivePage);
								}
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
		if (!getTapestryIndex().contains(project)) {
			return;
		}

		Set<TapestryModule> modules= getTapestryIndex().getModules();
		List<TapestryModule> toRemove=
				getTapestryIndex().getModulesForProject(project);

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
			if (isHtmlFileChecked(target)) {
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
		Set<Object> toSet= getTapestryIndex().getRelatedObjects(file);
		if (toSet.size() > 0) {
			for (Object toFile: toSet) {

				// prefer html/java files
				boolean isPreferred= false;
				try {
					isPreferred= (toFile instanceof IFile && isHtmlFileChecked((IFile) toFile))
							|| toFile instanceof ICompilationUnit;
				} catch(CoreException e) {
					log.warn("Couldn't check toFile if it is Html or Java.", e);
				}

				// use first file as default
				if (isPreferred || target == null) {
					try {
						if (toFile instanceof IFile) {
							target= (IFile) toFile;
						} else if (toFile instanceof ICompilationUnit) {
							target= (IFile) ((ICompilationUnit) toFile)
									.getCorrespondingResource().getAdapter(IFile.class);
						}

						if (isPreferred) {
							break;
						}
					} catch (JavaModelException e) {
						log.warn("Couldn't get corresponding resource for "
								+ "ICompilationUnit: ", e);
					}
				}
			}
		}
		return target;
	}


	@Override
	public void elementAdded(TapestryModule module,
			TapestryElement element) {
		TapestryHtmlElement htmlElement;
		switch (element.getType()) {
			case COMPONENT:
			case PAGE:
				htmlElement= (TapestryHtmlElement) element;
				for (Pair<IFile, Object> relation: htmlElement.getRelations()) {
					getTapestryIndex().addBidiRelation(relation.getLeft(),
						relation.getRight());
				}
				/*
				getTapestryIndex().addRelationToCompilationUnit(
					htmlElement.getHtmlFile(), htmlElement.getJavaCompilationUnit());
					*/
				break;
			default:
		}
	}


	@Override
	public void elementRemoved(TapestryModule module,
			TapestryElement element) {
		TapestryHtmlElement htmlElement;
	    switch (element.getType()) {
	        case COMPONENT:
	        case PAGE:
	            htmlElement= (TapestryHtmlElement) element;
	            for (Pair<IFile, Object> relation: htmlElement.getRelations()) {
	                tapestryIndex.addBidiRelation(relation.getLeft(),
	                    relation.getRight());
	            }
	            /*
	            tapestryIndex.addRelationToCompilationUnit(
	                htmlElement.getHtmlFile(), htmlElement.getJavaCompilationUnit());
	            */
	            break;
	        default:
	    }
	}

	public static IFile getCorrespondingFileForCompilationUnit(
			ICompilationUnit compilationUnit)  {
		try {
		    return (IFile) compilationUnit
		        .getCorrespondingResource().getAdapter(IFile.class);
		} catch(JavaModelException e) {
		    log.warn("Could not get corresponding resource for compilation"
		        + " unit " + compilationUnit.getElementName(), e);
		}
		return null;
	}

	public static boolean isHtmlFile(IFile file) {
        try {
            return isHtmlFileChecked(file);
        } catch (CoreException e) {
            log.warn("isHtmlFileChecked(" + file.getName() + ") threw "
                + "exception: ", e);
        }
        return false;
	}

	public TapestryIndex getTapestryIndex() {
       if (tapestryIndexStore == null) {
           tapestryIndexStore= Activator.getDefault().getTapestryIndex();
       }
       return tapestryIndexStore;
	}
}
