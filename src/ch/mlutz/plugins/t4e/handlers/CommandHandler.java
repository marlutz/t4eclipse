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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Command;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.index.jobs.AddProjectToIndexJob;
import ch.mlutz.plugins.t4e.index.jobs.SwitchToCorrespondingFileJob;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CommandHandler extends AbstractHandler implements
	IExecutionListener {

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
				log.error("Couldn't switch to corresponding file: ", e);
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
			Job job= new AddProjectToIndexJob(project, tapestryIndexer);
			job.schedule();

			job= new SwitchToCorrespondingFileJob(currentFile, activePage, tapestryIndexer);
			job.schedule();
			// tapestryIndexer.addProjectToIndex(project, currentFile, activePage, true);

			log.info("Adding project scheduled: " + project.toString() + ".");
		} else {
			// check if file is already in index ==> also catches Java files
			IFile toFile= tapestryIndexer.getRelatedFile(currentFile);
			if (toFile != null) {
				TapestryIndexer.openFileInSpecificEditor(toFile, activePage);
				return;
			}

			// try to get TapestryModule
			TapestryModule module= tapestryIndex.getModuleForResource(currentFile);

			if (module != null) {
				toFile= module.findRelatedFile(currentFile);
			}

			if (toFile != null) {
				TapestryIndexer.openFileInSpecificEditor(toFile, activePage);
				return;
			}
		}
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

	/* LogProvider interface implementation */
	public ILog getLog() {
		return Activator.getDefault().getLog();
	}

	public TapestryIndex getTapestryIndex() {
		return Activator.getDefault().getTapestryIndex();
	}

	public TapestryIndexer getTapestryIndexer() {
		return Activator.getDefault().getTapestryIndexer();
	}
}
