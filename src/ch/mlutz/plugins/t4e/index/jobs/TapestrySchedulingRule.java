/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - TapestrySchedulingRule
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index.jobs;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * Scheduling rule for ordering of Tapestry jobs to avoid conflicts between
 * them.
 *
 * @author Marcel Lutz
 */
public class TapestrySchedulingRule implements ISchedulingRule {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			TapestrySchedulingRule.class);

	/**
	 * the resource, mostly a project, to avoid conflicts of
	 */
	private final IResource resource;

	/**
	 * Creates a new TapestrySchedulingRule.
	 *
	 * @param resource the resource to which this scheduling rule refers. The
	 * 		value null represents the whole Tapestry Index.
	 */
	public TapestrySchedulingRule(IResource resource) {
		super();
		this.resource = resource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 *
	 * The rules for ISchedulingRule:
	 * contains method must return false when given an unknown rule
	 * isConflicting method must be reflexive
	 * isConflicting method must return false when given an unknown rule
	 *
	 */
	@Override
	public boolean contains(ISchedulingRule other) {
		return this.equals(other)
				|| (this.getResource() == null
				&& other instanceof	TapestrySchedulingRule);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean isConflicting(ISchedulingRule other) {
		return this.equals(other) || this.contains(other)
				|| (other instanceof TapestrySchedulingRule
						&& ((TapestrySchedulingRule) other).contains(this));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((resource == null) ? 0 : resource.getFullPath().hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TapestrySchedulingRule)) {
			return false;
		}
		TapestrySchedulingRule other = (TapestrySchedulingRule) obj;
		if (resource == null) {
			if (other.resource != null) {
				return false;
			}
		} else if (!resource.equals(other.resource)) {
			return false;
		}
		return true;
	}

	public IResource getResource() {
		return resource;
	}
}
