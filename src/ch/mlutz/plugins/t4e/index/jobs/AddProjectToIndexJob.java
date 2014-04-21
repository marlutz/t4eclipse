/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - job for adding a project to the index
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index.jobs;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryException;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;

/**
 * An Eclipse API job for adding a project to the TapestryIndex.
 *
 * @author Marcel Lutz
 */
public class AddProjectToIndexJob extends Job {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			AddProjectToIndexJob.class);

	protected static final String TASK_NAME_ADD_PROJECT=
			"Adding project ";

	protected static final String TASK_NAME_CREATING_MODULES=
			"Creating modules for project ";

	protected static final String TASK_NAME_UPDATING_MODULES=
			"Updating modules for project ";

	protected static final String TASK_NAME_UPDATE_TAPESTRY_INDEX=
			"Update Tapestry index";

	private final IProject projectToAdd;

	private final TapestryIndexer tapestryIndexer;

	private final TapestryIndex tapestryIndex;

	private final boolean waitForRefreshesAndBuilds;

	/**
	 * @param projectToAdd the project to be added to the TapestryIndex
	 * @param tapestryIndexer the TapestryIndexer instance to be used to add the
	 * 		project
	 */
	public AddProjectToIndexJob(IProject projectToAdd,
			TapestryIndexer tapestryIndexer,
			boolean waitForRefreshesAndBuilds) {
		super(TASK_NAME_UPDATE_TAPESTRY_INDEX);
		this.projectToAdd= projectToAdd;
		this.tapestryIndexer= tapestryIndexer;
		this.waitForRefreshesAndBuilds= waitForRefreshesAndBuilds;

		this.tapestryIndex= tapestryIndexer.getTapestryIndex();

		ISchedulingRule tapestrySchedulingRule= new TapestrySchedulingRule(
				projectToAdd);

		setRule(tapestrySchedulingRule);
		setUser(false);
		setPriority(Job.LONG);

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

		monitor.beginTask(TASK_NAME_ADD_PROJECT + projectToAdd.getName(), 1000);

		if (tapestryIndex.contains(projectToAdd)) {
			// early out
			monitor.worked(1000);
			monitor.done();
			return Status.OK_STATUS;
		}

		try {
			tapestryIndex.add(projectToAdd);

			monitor.worked(100);
			monitor.setTaskName(TASK_NAME_CREATING_MODULES + projectToAdd.getName());

			if (waitForRefreshesAndBuilds) {
				waitForFamily(ResourcesPlugin.FAMILY_AUTO_REFRESH);
				waitForFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
			}

			List<TapestryModule> modulesToAdd= tapestryIndexer
					.createModulesForProject(projectToAdd);
				log.info("Found " + modulesToAdd.size() + " modules to add to " + projectToAdd.getName());

			if (monitor.isCanceled()) {
				 return Status.CANCEL_STATUS;
			}

			monitor.worked(300);
			monitor.setTaskName(TASK_NAME_UPDATING_MODULES + projectToAdd.getName());

			for (TapestryModule module: modulesToAdd) {
				// totalWork+= module.getScanAndUpdateWork();
				tapestryIndex.add(module);
				log.info("Scanning and updating module " + module.getAppSpecification().getAppSpecificationFile().getName());
				module.scanAndUpdateIndex(monitor);
			}

			monitor.worked(600);
		} catch(TapestryException e) {
			log.error("Could not add project " + projectToAdd.getName(), e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	protected static void waitForFamily(Object family) {
	    boolean wasInterrupted = false;
	    do {
	      try {
	        Job.getJobManager().join(family, null);
	        wasInterrupted = false;
	      }
	      catch (OperationCanceledException e) {
	        e.printStackTrace();
	      }
	      catch (InterruptedException e) {
	        wasInterrupted = true;
	      }
	    }
	    while (wasInterrupted);
	  }
}
