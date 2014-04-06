/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 element
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IFile;

/**
 * @author Marcel Lutz
 * @version 1.0 23.03.2014
 */

public abstract class TapestryElement implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1842031515843959101L;

	public abstract String getName();

	public abstract ElementType getType();

	public List<Pair<IFile, Object>> getRelations() {
		return null;
	}
}
