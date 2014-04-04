/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - ResourceChangeListener
 ******************************************************************************/
package ch.mlutz.plugins.t4e.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IStartup;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tools.TapestryTools;

/**
 * Listener handling resource changes
 */
public class ResourceChangeListener implements IResourceChangeListener {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			ResourceChangeListener.class);

	/**
	 * The constructor.
	 */
	public ResourceChangeListener() {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		boolean logFileChanges= true;
		boolean logProjectChanges= false;
		boolean logOtherChanges= false;

		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			List<IFile> files = getFiles(event.getDelta(), IResourceDelta.CHANGED);

			TapestryIndex tapestryIndex= getTapestryIndex();
			synchronized(tapestryIndex) {
				for (IFile file: files) {
					if (TapestryTools.isComponentSpecification(file)) {
						// tapestryIndex.handleComponentSpecification(file);
					} else if (TapestryTools.isPageSpecification(file)) {
						// tapestryIndex.handlePageSpecification(file);
					} else if (TapestryTools.isAppSpecification(file)) {
						// tapestryIndex.handleAppSpecification(file);
					}
				}

				files = getFiles(event.getDelta(), IResourceDelta.ADDED);
				for (IFile file: files) {
					try {
						if (TapestryTools.isHtmlFile(file)) {
							tapestryIndex.handleHtmlFile(file);
						}
					} catch (CoreException e) {
						log.error("Error on resourceChanged", e);
					}
				}
			}

			files = getFiles(event.getDelta(), IResourceDelta.REMOVED);
			if (files.size() > 0 && logFileChanges) {
				log.info("Removed: " + files.size() + " ");
				// do something with new projects
				for (IFile file: files) {
					log.info(file.getName() + ", ");
				}
			}

			List<IProject> projects = getProjects(event.getDelta(), IResourceDelta.OPEN);
			if (projects.size() > 0 && logProjectChanges) {
				log.info("Opened P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (project.isOpen()) {
						log.info(project.getName() + ", ");
					}
				}
			}

			if (projects.size() > 0 && logProjectChanges) {
				log.info("Closed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (!project.isOpen()) {
						log.info(project.getName() + ", ");
					}
				}
			}

			projects = getProjects(event.getDelta(), IResourceDelta.CHANGED);
			if (projects.size() > 0 && logProjectChanges) {
				log.info("Changed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					log.info(project.getName() + ", ");
				}
			}

			projects = getProjects(event.getDelta(), IResourceDelta.ADDED);
			if (projects.size() > 0 && logProjectChanges) {
				log.info("Added P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					log.info(project.getName() + ", ");
				}
			}

			projects = getProjects(event.getDelta(), IResourceDelta.REMOVED);
			if (projects.size() > 0 && logProjectChanges) {
				log.info("Removed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					log.info(project.getName() + ", ");
				}
			}

		} else if (event.getType() == IResourceChangeEvent.PRE_REFRESH) {
			log.info("ResourceChanged: PRE_REFRESH");

			List<IProject> projects = getProjects(event.getDelta(), IResourceDelta.OPEN);
			if (projects.size() > 0 && logOtherChanges) {
				log.info("Opened P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (project.isOpen()) {
						log.info(project.getName() + ", ");
					}
				}
			}

			if (projects.size() > 0 && logOtherChanges) {
				log.info("Closed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (!project.isOpen()) {
						log.info(project.getName() + ", ");
					}
				}
			}

		} else if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			if (logOtherChanges) {
				System.out.println("Pre-Close event!" + " " + event.getResource().getName());
			}

		} else {
			log.info("ResourceChanged: " + event.getType());

			List<IProject> projects = getProjects(event.getDelta(), IResourceDelta.OPEN);
			if (projects.size() > 0 && logOtherChanges) {
				log.info("Opened P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (project.isOpen()) {
						log.info(project.getName() + ", ");
					}
				}
			}

			if (projects.size() > 0 && logOtherChanges) {
				log.info("Closed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (!project.isOpen()) {
						log.info(project.getName() + ", ");
					}
				}
			}
		}
		// System.out.println("Something changed!" + arg0.getType() + " " + arg0.toString() + " " + arg0.getResource());
	}

	private List<IProject> getProjects(IResourceDelta delta, final int changeTypeMask) {
		final List<IProject> projects = new ArrayList<IProject>();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					if (((delta.getKind() & changeTypeMask) != 0 || changeTypeMask == 0) &&
					  delta.getResource().getType() == IResource.PROJECT) {
						IProject project = (IProject) delta.getResource();
						if (project.isAccessible()) {
							projects.add(project);
						}
					}
					// only continue for the workspace root
					return delta.getResource().getType() == IResource.ROOT;
				}
			});
		} catch (CoreException e) {
			// handle error
		}
		return projects;
	}

	private List<IFile> getFiles(IResourceDelta delta, final int changeTypeMask) {
		final List<IFile> files = new ArrayList<IFile>();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					if (((delta.getKind() & changeTypeMask) != 0 || changeTypeMask == 0) &&
					  delta.getResource().getType() == IResource.FILE) {
						IFile file = (IFile) delta.getResource();
						if (file.isAccessible() || changeTypeMask == IResourceDelta.REMOVED) {
							files.add(file);
						}
					}
					// only continue for the workspace root
					// return delta.getResource().getType() == IResource.ROOT;
					return true;
				}
			});
		} catch (CoreException e) {
			// handle error
		}
		return files;
	}

	public TapestryIndex getTapestryIndex() {
		return Activator.getDefault().getTapestryIndex();
	}

	public TapestryIndexer getTapestryIndexer() {
		return Activator.getDefault().getTapestryIndexer();
	}
}
