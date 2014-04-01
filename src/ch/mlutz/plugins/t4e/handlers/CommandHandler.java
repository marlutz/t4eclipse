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
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isComponentSpecification;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isHtmlFile;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isJavaFile;
import static ch.mlutz.plugins.t4e.tools.TapestryTools.isPageSpecification;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.text.Document;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.StatusTextEditor;

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
	public CommandHandler() {
		Activator.getDefault().setHandler(this);
	}

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

				final ITextEditor textEditor= (ITextEditor) part;
				final ISelection sel= textEditor.getSelectionProvider().getSelection();
				final IFile currentFile= (IFile) part.getEditorInput().getAdapter(IFile.class);

				if (sel instanceof ITextSelection) {
					ITextSelection textSelection= (ITextSelection) sel;

					final IDocumentProvider prov= textEditor.getDocumentProvider();
					final IDocument doc= prov.getDocument(textEditor.getEditorInput());
					// final String currentText= doc.get();

					/*
					String currentContentType= doc.getContentType(
						textSelection.getOffset());

					log.info("Current content type: " + currentContentType);
					*/
				}

				final TextSelection textSel= (TextSelection)sel;
				if (textSel.getText() == null || "".equals(textSel.getText())) {
					logMessage("Tapestry Plugin: No text selected.");

					switchToComplementFileCommand(currentFile, activePage);
					return null;
				}

				final IWorkspace workspace = ResourcesPlugin.getWorkspace();
				final IWorkspaceRoot root = workspace.getRoot();

				String[] pathComponents= textSel.getText().split("/");

				int pathStartIndex= textSel.getText().charAt(0) == '/' ? 1 : 0;
				String projectName= pathComponents[pathStartIndex];
				IProject project= root.getProject(projectName);

				if (!project.exists()) {
					MessageDialog.openInformation(
							window.getShell(),
							Constants.PLUGIN_DISPLAYNAME,
							"Project '" + projectName + "' does not exist.");
					return null;
				}

				if (!project.isOpen()) {
					project.open(null);
				}



				/*
				MessageDialog.openInformation(
						window.getShell(),
						Constants.PLUGIN_DISPLAYNAME,
						"Text selected: " + textSel.getText() + "; number of Path components: "
								+ pathComponents.length + "; Project: " + project.exists());
				*/

				try {
					IFile file= null;
					if (pathStartIndex + 1 < pathComponents.length-1) {
						// there is at least one intermediate folder between project and file
						IFolder folder= project.getFolder(pathComponents[pathStartIndex+1]);
						if (!folder.exists()) {
							folder.create(true, true, null);
						}

						for (int i= pathStartIndex + 2; i < pathComponents.length-1; i++) {
							folder= folder.getFolder(pathComponents[i]);
							if (!folder.exists()) {
								folder.create(true, true, null);
							}
						}

						file= folder.getFile(pathComponents[pathComponents.length-1]);
					} else if (pathStartIndex + 1 == pathComponents.length-1) {
						// no intermediate folder between project and file
						file= project.getFile(pathComponents[pathComponents.length-1]);
					} else {
						MessageDialog.openInformation(
								window.getShell(),
								Constants.PLUGIN_DISPLAYNAME,
								"Invalid path is unusable for file creation.");
						return null;
					}

					// NOTE: file mustn't be null here
					if (!file.exists()) {
						ByteArrayInputStream bais= new ByteArrayInputStream("Hello World from new file!".getBytes());
						file.create(bais, true, null);
					}
					IEditorDescriptor desc = PlatformUI.getWorkbench().
							getEditorRegistry().getDefaultEditor(file.getName());
					if (activePage != null) {
						activePage.openEditor(new FileEditorInput(file), desc.getId());
					}
				} catch (CoreException e) {
					e.printStackTrace();
					return null;
				} catch (OperationCanceledException e) {
					MessageDialog.openInformation(
							window.getShell(),
							Constants.PLUGIN_DISPLAYNAME,
							"Operation canceled by user.");
					return null;
				}

				// IFile newFile= root.getFile()

				/*
				MessageDialog.openInformation(
						window.getShell(),
						Constants.PLUGIN_DISPLAYNAME,
						"Text selected: " + textSel.getText());
				return null;
				*/

				/*
					String filePath= "; filePath: ";


					if (currentFile != null) {
						filePath+= "Fullpath: " + currentFile.getFullPath()
								+ "; Name: " + currentFile.getName() + "; Project: " + currentFile.getProject();
					}

					String newText = "/*" + textSel.getText() + "*" + "; DocumentLength: " + doc.get().length() + filePath;
					MessageDialog.openInformation(
							window.getShell(),
							"ML Plugin",
							newText);
					doc.replace( textSel.getOffset(), textSel.getLength(), newText );
				}
				*/

				/*
				MessageDialog.openInformation(
						window.getShell(),
						"HelloPlugin",
						"Hello, Eclipse world");
				*/

				/*
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IProject project  = root.getProject("asdf");
				// IFolder folder = project.getFolder("Folder1");
				IFile file = project.getFile("openme.txt");
				//at this point, no resources have been created

				if (!project.exists()) project.create(null);
				if (!project.isOpen()) project.open(null);
				if (file.exists()) {
					IEditorDescriptor desc = PlatformUI.getWorkbench().
							getEditorRegistry().getDefaultEditor(file.getName());
					if (activePage != null) {
						activePage.openEditor(new FileEditorInput(file), desc.getId());
					}
				}
				*/

				/*
				if (!project.exists()) project.create(null);
				if (!project.isOpen()) project.open(null);
				if (!folder.exists())
					folder.create(IResource.NONE, true, null);
				if (!file.exists()) {
					byte[] bytes = "File contents".getBytes();
					InputStream source = new ByteArrayInputStream(bytes);
					file.create(source, IResource.NONE, null);
				*/

				/*
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put(IMarker.LINE_NUMBER, new Integer(5));
				map.put(IWorkbenchPage.EDITOR_ID_ATTR,
				   "org.eclipse.ui.DefaultTextEditor");
				IMarker marker = file.createMarker(IMarker.TEXT);
				marker.setAttributes(map);
				//page.openEditor(marker); //2.1 API
				IDE.openEditor(marker); //3.0 API
				marker.delete();
				*/
			} catch (ExecutionException e) {
				// one of the workbench objects could not be retrieved
				return null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (event.getCommand().getId().equals("ch.mlutz.plugins.t4e.commands.refresh")) {
			System.out.println("refresh has been invoked");

			ISelection selection= HandlerUtil.getCurrentSelection(event);

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
		/*
		Set<String> componentClassPackages= module.getAppSpecification()
				.getComponentClassPackages();
		Set<String> pageClassPackages= 		module.getAppSpecification()
				.getPageClassPackages();
		*/

		/*

		if (componentClassPackages == null || pageClassPackages == null) {
			Job job = new Job("Update Tapestry Index") {

				@Override
				public IStatus run(IProgressMonitor monitor) {
					try {
						synchronized(tapestryIndex) {
							getTapestryIndexer().update(project, monitor);
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									try {
										switchToComplementFile(finalCurrentFile,
												finalActivePage, true);
									} catch (CoreException e) {
										e.printStackTrace();
									}
								}
							});
						}
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return Status.OK_STATUS;
				}
			};
			job.setUser(true);
			job.setPriority(Job.SHORT);
			job.schedule(); // start as soon as possible


		} else {
			synchronized(tapestryIndex) {
				switchToComplementFile(currentFile, activePage,
						false);
			}
		}
		*/
	}

	private void switchToComplementFile(IFile currentFile,
			IWorkbenchPage activePage, boolean updated) throws CoreException {

		IFile targetFile= null;
		if (isHtmlFile(currentFile)) {
			logMessage("Current file is html.");
			targetFile= switchToComplementFileFromHtml(currentFile, updated);
		} else if (isPageSpecification(currentFile) || isComponentSpecification(currentFile)) {
			logMessage("Current file is page/component specification.");
			targetFile= switchToComplementFileFromSpecification(currentFile,
					updated);
		} else if (isJavaFile(currentFile)) {
			logMessage("Current file is java file.");
			targetFile= switchToComplementFileFromJava(currentFile,
					updated);
		}

		if (targetFile != null) {
			openFileInEditor(targetFile, activePage);
			logMessage("File opened in editor.");
		}
		return;


		// 2nd step: check if we have a connected file for currentFile. If not:
		//   for HTML file: look for corresponding page or jwc file and reindex those first;
		//     otherwise, look for corresponding java file by path
		//   for jwc/page file: parse file and look for java file
		//   for java file: update whole project (there's no other way since page/jwc files
		//     might refer to it)
		// and try again; need to show dialog box if there are multiple possibilities;
		// put file matching by name first, then the ones by jwc/page file
		// have a look here: org.eclipse.debug.internal.ui.launchConfigurations.SaveScopeResourcesHandler

		/*
		tapestryIndex.clear();

		// in course of update, get app.application descriptor
		// TODO: need to maintain this project info
		tapestryIndex.update(currentFile.getProject());

		IResource connectedResource= tapestryIndex.getConnectedFile(currentFile);

		if (connectedResource instanceof IFile) {
			IFile connectedFile= (IFile) connectedResource;
			IEditorDescriptor desc = PlatformUI.getWorkbench().
				getEditorRegistry().getDefaultEditor(connectedResource.getName());
			if (activePage != null) {
				activePage.openEditor(new FileEditorInput(connectedFile), desc.getId());
			}
		}
		*/

		// projectFiles.printStatistic();


		// TODO: create a mapping cache of all .page and .jwc files; need to iterate over whole project

		// get path of currentfile

		// determine type of file (java, html or other (jwc, page)

		// java: get package name of file and search for html file of same name
	}

	private IFile switchToComplementFileFromSpecification(IFile currentFile,
			boolean updated) {
		IFile targetFile;

		TapestryIndex tapestryIndex= getTapestryIndex();
		synchronized(tapestryIndex) {
			targetFile= tapestryIndex.getJavaForSpecificationFile(currentFile);
			if (targetFile == null && !updated) {
				System.out.print("Handling component specification...");
				tapestryIndex.handleComponentSpecification(currentFile);
				System.out.println("handled.");
				targetFile= tapestryIndex.getJavaForSpecificationFile(currentFile);
			}
		}
		return targetFile;
	}

	private IFile switchToComplementFileFromHtml(IFile htmlFile,
			boolean updated) {
		// 1st substep: try to get Java file via specification file
		IFile targetFile= null;

		// first try to get page specification ...
		IFile specificationFile= TapestryTools.findPageSpecificationforHtmlFile(htmlFile);

		// then try to get component specification ...
		if (specificationFile == null) {
			specificationFile= TapestryTools.findComponentSpecificationforHtmlFile(htmlFile);
		}

		if (specificationFile != null) {
			return switchToComplementFileFromSpecification(
						specificationFile, updated);
		}

		TapestryIndex tapestryIndex= getTapestryIndex();
		synchronized(tapestryIndex) {
			/*
			 *  finding the java file via specification didn't work ==> try direct
			 *  lookup via package/name; index first, then find
			 */
			targetFile= tapestryIndex.getJavaForHtmlFile(htmlFile);
			if (targetFile == null) {
				try {
					targetFile= tapestryIndex.findJavaForHtmlFile(htmlFile);
				} catch (CoreException e) {
					logError("Error on findJavaForHtmlFile", e);
				}

				if (targetFile != null) {
					tapestryIndex.addHtmlToJavaEntry(htmlFile, targetFile);
				}
			}
		}

		return targetFile;
	}

	private IFile switchToComplementFileFromJava(IFile currentFile,
			boolean updated) {

		TapestryIndex tapestryIndex= getTapestryIndex();
		synchronized(tapestryIndex) {
			IFile specificationFile= tapestryIndex.getSpecificationForJavaFile(currentFile);

			if (specificationFile != null) {
				return TapestryTools.findHtmlForSpecificationFile(specificationFile);
			}

			return tapestryIndex.getHtmlForJavaFile(currentFile);
		}
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute2(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		/*
		MessageDialog.openInformation(
				window.getShell(),
				"HelloPlugin",
				"Hello, Eclipse world");
		*/

		Object selectedPage= null;
		IWorkbenchPage activePage= null;
		try {
			IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			activePage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			if(part instanceof MultiPageEditorPart){
				System.out.println("is of type MultiPageEditorPart editor >> trying to go to child");

				final MultiPageEditorPart editor = (MultiPageEditorPart)part;

				selectedPage= editor.getSelectedPage();

				if (selectedPage instanceof  IEditorPart) {
					System.out.println("selectedPage is of type IEditorPart");
					part= (IEditorPart) selectedPage;
				}
			}

			if(part instanceof IReusableEditor){

					System.out.println("is of type reusable editor");

				final IReusableEditor editor = (IReusableEditor)part;

				if(editor instanceof AbstractTextEditor){
					System.out.println("abstract");
				}
				if(editor instanceof StatusTextEditor){
					System.out.println("status");
				}
				if(editor instanceof TextEditor){
					System.out.println("text");
				}
				if(editor instanceof AbstractDecoratedTextEditor){
					System.out.println("abs dec");
				}
				if(editor instanceof CommonSourceNotFoundEditor){
					System.out.println("comm");
				}

			}

			if ( part instanceof ITextEditor ) {
				final ITextEditor editor = (ITextEditor)part;
				IDocumentProvider prov = editor.getDocumentProvider();
				IDocument doc = prov.getDocument( editor.getEditorInput() );
				ISelection sel = editor.getSelectionProvider().getSelection();
				if ( sel instanceof TextSelection ) {
					final TextSelection textSel = (TextSelection)sel;

					String filePath= "; filePath: ";
					IFile file = (IFile) part.getEditorInput().getAdapter(IFile.class);
					if (file != null) {
						filePath+= file.getFullPath() + " " + file.getName();
					}

					String newText = "/*" + textSel.getText() + "*/" + "; DocumentLength: " + doc.get().length() + filePath;
					MessageDialog.openInformation(
							window.getShell(),
							"AnirudhPlugin",
							newText);
					doc.replace( textSel.getOffset(), textSel.getLength(), newText );
				}
			}else{
				MessageDialog.openInformation(
						window.getShell(),
						"AnirudhPlugin",
						"Not ITextEditor");
			}

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			IProject project  = root.getProject("asdf");
			// IFolder folder = project.getFolder("Folder1");
			IFile file = project.getFile("openme.txt");
			//at this point, no resources have been created

			if (!project.exists()) project.create(null);
			if (!project.isOpen()) project.open(null);
			if (file.exists()) {
				IEditorDescriptor desc = PlatformUI.getWorkbench().
						getEditorRegistry().getDefaultEditor(file.getName());
				if (activePage != null) {
					activePage.openEditor(new FileEditorInput(file), desc.getId());
				}
			}

			/*
			if (!project.exists()) project.create(null);
			if (!project.isOpen()) project.open(null);
			if (!folder.exists())
				folder.create(IResource.NONE, true, null);
			if (!file.exists()) {
				byte[] bytes = "File contents".getBytes();
				InputStream source = new ByteArrayInputStream(bytes);
				file.create(source, IResource.NONE, null);
			*/

			/*
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put(IMarker.LINE_NUMBER, new Integer(5));
			map.put(IWorkbenchPage.EDITOR_ID_ATTR,
			   "org.eclipse.ui.DefaultTextEditor");
			IMarker marker = file.createMarker(IMarker.TEXT);
			marker.setAttributes(map);
			//page.openEditor(marker); //2.1 API
			IDE.openEditor(marker); //3.0 API
			marker.delete();
			*/
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}

		return null;
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
						tapestryIndex.handleComponentSpecification(file);
					} else if (TapestryTools.isPageSpecification(file)) {
						tapestryIndex.handlePageSpecification(file);
					} else if (TapestryTools.isAppSpecification(file)) {
						tapestryIndex.handleAppSpecification(file);
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

	/*
	 *
	 *

The selection service knows the current selection of the active part or of a part with a particular id:

	ISelection getSelection()

	 *     if (selection instanceof IStructuredSelection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		Object obj = ssel.getFirstElement();
		IFile file = (IFile) Platform.getAdapterManager().getAdapter(obj,
				IFile.class);
		if (file == null) {
			if (obj instanceof IAdaptable) {
				file = (IFile) ((IAdaptable) obj).getAdapter(IFile.class);
			}
		}
		if (file != null) {
			// do something
		}
	}
	*/

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
