/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 module change listener
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index;

import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryElement;

/**
 * @author Marcel Lutz
 * @version 1.0 12.02.2014
 */

public interface ITapestryModuleChangeListener {
	/**
	 * Called after a tapestry element is added to the module.
	 *
	 * @param tapestryModule
	 * @param tapestryElement
	 */
	public abstract void elementAdded(
			TapestryModule tapestryModule, TapestryElement tapestryElement);

	/**
	 * Called before a tapestry element is removed from the module
	 *
	 * @param tapestryModule
	 * @param tapestryElement
	 */
	public abstract void elementRemoved (
			TapestryModule tapestryModule, TapestryElement tapestryElement);
}
