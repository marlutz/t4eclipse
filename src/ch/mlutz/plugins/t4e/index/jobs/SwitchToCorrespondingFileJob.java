/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - job for switching to a corresponding file
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index.jobs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;

import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;

/**
 * A Eclipse API job that gets the corresponding file for a base file and opens
 * it in a new editor window.
 *
 * @author Marcel Lutz
 */
public class SwitchToCorrespondingFileJob extends Job {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			SwitchToCorrespondingFileJob.class);

	protected static final String TASK_NAME_SWITCH_TO_CORRESPONDING_FILE=
			"Switching to corresponding file for ";

	protected static final String TASK_NAME_SWITCH_FILES=
			"Switch between Tapestry files";

	private final IFile baseFile;

	private final IWorkbenchPage targetPage;

	private final TapestryIndexer tapestryIndexer;

	/**
	 * @param baseFile the file to get and open the corresponding file for
	 * @param targetPage the Eclipse Workbench page to open the new editor in
	 * @param tapestryIndexer the TapestryIndexer instance to be used for
	 * 		switching
	 */
	public SwitchToCorrespondingFileJob(IFile baseFile,
			IWorkbenchPage targetPage,
			TapestryIndexer tapestryIndexer) {
		super(TASK_NAME_SWITCH_FILES);
		this.baseFile= baseFile;
		this.targetPage= targetPage;
		this.tapestryIndexer= tapestryIndexer;

		setRule(new TapestrySchedulingRule(baseFile.getProject()));
		setUser(true);
		setPriority(Job.SHORT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) {
		// use a NullProgressMonitor as stub if needed
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}

		monitor.beginTask(TASK_NAME_SWITCH_TO_CORRESPONDING_FILE
				+ baseFile.getName(), 1000);

		try {
			// check if file is already in index ==> also catches Java files
			IFile toFile= tapestryIndexer.getRelatedFile(baseFile);
			if (toFile == null) {
				// try to get TapestryModule
				TapestryModule module= tapestryIndexer.getTapestryIndex()
						.getModuleForResource(baseFile);

				if (module != null) {
					toFile= module.findRelatedFile(baseFile);
				}
			}

			if (toFile != null) {
				final IFile finalToFile= toFile;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						TapestryIndexer.openFileInSpecificEditor(finalToFile,
								targetPage);
					}
				});
			}

			monitor.worked(1000);
		} catch (CoreException e) {
			log.error("Could not switch to corresponding file: ", e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}
