/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - main handler for UI commands
 ******************************************************************************/
package ch.mlutz.plugins.t4e.handlers;

import static ch.mlutz.plugins.t4e.tools.EclipseTools.openFileInEditor;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isHtmlFile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Command;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tools.TapestryTools;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CommandHandler extends AbstractHandler implements IStartup, IExecutionListener,
	IResourceChangeListener
{
	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			CommandHandler.class);

	/**
	 * The constructor.
	 */
	public CommandHandler() {}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (event.getCommand().getId().equals(Command.SWITCH_TO_COMPLEMENT_FILE)) {
			try {
				final IWorkbenchWindow window= HandlerUtil.getActiveWorkbenchWindowChecked(event);
				final IWorkbenchPage activePage= window.getActivePage();

				if (activePage == null) {
					MessageDialog.openInformation(
							window.getShell(),
							Constants.PLUGIN_DISPLAYNAME,
							"No active page found.");
					return null;
				}

				IEditorPart part= activePage.getActiveEditor();

				if (part != null && !(part instanceof ITextEditor)) {
					part= (IEditorPart) part.getAdapter(ITextEditor.class);
				}

				if (!(part instanceof ITextEditor)) {
					MessageDialog.openInformation(
							window.getShell(),
							Constants.PLUGIN_DISPLAYNAME,
							"No file open in text editor.");
					return null;
				}

				final IFile currentFile= (IFile) part.getEditorInput().getAdapter(IFile.class);

				switchToComplementFileCommand(currentFile, activePage);

			} catch (ExecutionException e) {
				// one of the workbench objects could not be retrieved
				return null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private void switchToComplementFileCommand(IFile currentFile,
		IWorkbenchPage activePage) throws CoreException {

		// 1st step: check if we have page- and component class packages
		// for currentFile.getProject(). If not, update this project.
		final IProject project= currentFile.getProject();

		final TapestryIndex tapestryIndex= getTapestryIndex();
		final TapestryIndexer tapestryIndexer= getTapestryIndexer();

		// check and add project to TapestryIndex if necessary
		if (!tapestryIndex.contains(project)) {
			tapestryIndexer.addProjectToIndex(project, currentFile, activePage);
			log.info("Added project " + project.toString() + ".");
		} else {
			// check if file is already in index ==> also catches Java files
			IFile toFile= tapestryIndexer.getRelatedFile(currentFile);
			if (toFile != null) {
				if (isHtmlFile(toFile)) {
					openFileInEditor(toFile, activePage, "ch.mlutz.plugins.t4e.editors.tapestryEditor");
				} else {
					openFileInEditor(toFile, activePage);
				}
				return;
			}

			// try to get TapestryModule
			TapestryModule module= tapestryIndex.getModuleForResource(currentFile);

			if (module != null) {
				toFile= module.findRelatedFile(currentFile);
			}

			if (toFile != null) {
				openFileInEditor(toFile, activePage);
				return;
			}
		}
	}

	// IStartup implementation

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup()
	{
		System.out.println("earlyStartup called...");

		// Add listener to monitor Cut and Copy commands
		ICommandService commandService = (ICommandService) PlatformUI
			.getWorkbench().getAdapter(ICommandService.class);
		if (commandService != null) {
			commandService.addExecutionListener(this);
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(this);
	}

	// IExecutionListener implementation
	@Override
	public void notHandled(String commandId,
		NotHandledException exception) {
	}

	@Override
	public void postExecuteFailure(String commandId,
		ExecutionException exception) {
	}

	@Override
	public void postExecuteSuccess(String commandId,
			Object returnValue) {}

	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
		if ("org.eclipse.ui.file.refresh".equals(commandId)) {
			System.out.println("preExecute called on org.eclipse.ui.file.refresh "
				+ event.toString());
			Activator activator = Activator.getDefault();
			IWorkbench workbench = activator.getWorkbench();
			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
			ISelectionService selectionService = workbenchWindow
			.getSelectionService();
			if (selectionService != null) {
				System.out.println("selectionService is NOT NULL!");
				ISelection selection= selectionService.getSelection("org.eclipse.jdt.ui.PackageExplorer");

				if (selection instanceof IStructuredSelection) {

					System.out.println("selection instanceof IStructuredSelection!");

					IStructuredSelection ssel = (IStructuredSelection) selection;

					System.out.println("selection 1");

					Object obj = ssel.getFirstElement();

					System.out.println("selection 2");

					IFile file = (IFile) Platform.getAdapterManager().getAdapter(obj,
							IFile.class);

					System.out.println("selection 3");

					if (file == null) {
						if (obj instanceof IAdaptable) {
							file = (IFile) ((IAdaptable) obj).getAdapter(IFile.class);
						}
					}

					System.out.println("selection 4");

					if (file != null) {
						// do something
						System.out.println("Refreshed file: " + file.getFullPath());
					}
				} else {
					System.out.println("!(selection instanceof IStructuredSelection): " + (selection != null ? selection.getClass() : "null"));
				}
			} else {
				System.out.println("selectionService is NULL!");
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event)
	{
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
						logError("Error on resourceChanged", e);
					}
				}
			}

			files = getFiles(event.getDelta(), IResourceDelta.REMOVED);
			if (files.size() > 0 && logFileChanges) {
				System.out.print("Removed: " + files.size() + " ");
				// do something with new projects
				for (IFile file: files) {
					System.out.print(file.getName() + ", ");
				}
				System.out.println();
			}

			List<IProject> projects = getProjects(event.getDelta(), IResourceDelta.OPEN);
			if (projects.size() > 0 && logProjectChanges) {
				System.out.print("Opened P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (project.isOpen()) {
						System.out.print(project.getName() + ", ");
					}
				}
				System.out.println();
			}
			if (projects.size() > 0 && logProjectChanges) {
				System.out.print("Closed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (!project.isOpen()) {
						System.out.print(project.getName() + ", ");
					}
				}
				System.out.println();
			}

			projects = getProjects(event.getDelta(), IResourceDelta.CHANGED);
			if (projects.size() > 0 && logProjectChanges) {
				System.out.print("Changed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					System.out.print(project.getName() + ", ");
				}
				System.out.println();
			}

			projects = getProjects(event.getDelta(), IResourceDelta.ADDED);
			if (projects.size() > 0 && logProjectChanges) {
				System.out.print("Added P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					System.out.print(project.getName() + ", ");
				}
				System.out.println();
			}

			projects = getProjects(event.getDelta(), IResourceDelta.REMOVED);
			if (projects.size() > 0 && logProjectChanges) {
				System.out.print("Removed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					System.out.print(project.getName() + ", ");
				}
				System.out.println();
			}


		} else if (event.getType() == IResourceChangeEvent.PRE_REFRESH) {
			System.out.print("ResourceChanged: PRE_REFRESH");

			List<IProject> projects = getProjects(event.getDelta(), IResourceDelta.OPEN);
			if (projects.size() > 0 && logOtherChanges) {
				System.out.print("Opened P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (project.isOpen()) {
						System.out.print(project.getName() + ", ");
					}
				}
				System.out.println();
			}
			if (projects.size() > 0 && logOtherChanges) {
				System.out.print("Closed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (!project.isOpen()) {
						System.out.print(project.getName() + ", ");
					}
				}
				System.out.println();
			}
		} else if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			if (logOtherChanges) {
				System.out.println("Pre-Close event!" + " " + event.getResource().getName());
			}
		} else {
			System.out.print("ResourceChanged: " + event.getType());

			List<IProject> projects = getProjects(event.getDelta(), IResourceDelta.OPEN);
			if (projects.size() > 0 && logOtherChanges) {
				System.out.print("Opened P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (project.isOpen()) {
						System.out.print(project.getName() + ", ");
					}
				}
				System.out.println();
			}
			if (projects.size() > 0 && logOtherChanges) {
				System.out.print("Closed P: " + projects.size() + " ");
				// do something with new projects
				for (IProject project: projects) {
					if (!project.isOpen()) {
						System.out.print(project.getName() + ", ");
					}
				}
				System.out.println();
			}
		}
		// System.out.println("Something changed!" + arg0.getType() + " " + arg0.toString() + " " + arg0.getResource());
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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

	/* LogProvider interface implementation */
	public ILog getLog() {
		return Activator.getDefault().getLog();
	}

	public void logMessage(String s) {
		getLog().log(new Status(Status.INFO, Constants.PLUGIN_ID, Status.OK, s, null));
	}

	public void logMessage(String s, Throwable exception) {
		getLog().log(new Status(Status.INFO, Constants.PLUGIN_ID, Status.OK, s, exception));
	}

	public void logWarning(String s) {
		getLog().log(new Status(Status.WARNING, Constants.PLUGIN_ID, Status.OK, s, null));
	}

	public void logWarning(String s, Throwable exception) {
		getLog().log(new Status(Status.WARNING, Constants.PLUGIN_ID, Status.OK, s, exception));
	}

	public void logError(String s) {
		getLog().log(new Status(Status.ERROR, Constants.PLUGIN_ID, Status.OK, s, null));
	}

	public void logError(String s, Throwable exception) {
		getLog().log(new Status(Status.ERROR, Constants.PLUGIN_ID, Status.OK, s, exception));
	}

	public TapestryIndex getTapestryIndex() {
		return Activator.getDefault().getTapestryIndex();
	}

	public TapestryIndexer getTapestryIndexer() {
		return Activator.getDefault().getTapestryIndexer();
	}
}
