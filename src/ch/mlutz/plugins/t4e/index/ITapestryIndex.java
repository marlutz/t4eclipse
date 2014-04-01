/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 index interface
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index;

import org.eclipse.core.resources.IFile;

/**
 * @author Marcel Lutz
 * @version 1.0 12.02.2014
 */

public interface ITapestryIndex {

	/**
	 * Register a link between a Tapestry 4 file (html, component or page
	 * specification) and a java source file
	 *
	 * @param tapestryFile the Tapestry 4 source to be linked
	 * @param javaSourceFile the target of the link
	 */
	public abstract void registerFileLink(
			IFile tapestryFile, IFile javaSourceFile);

	/**
	 * Unregister the file link by its Tapestry 4 file, if it exists.
	 *
	 * @param tapestryFile the Tapestry 4 source to remove the link of
	 */
	public abstract void unregisterFileLink(IFile tapestryFile);

}
