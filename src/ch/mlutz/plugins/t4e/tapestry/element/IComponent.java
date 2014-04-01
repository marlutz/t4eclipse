/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 component interface
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

import java.util.List;

/**
 * TapestryComponent
 *
 * <p>
 * Describe TapestryComponent here...
 * </p>
 *
 *
 * @author mlutz
 *
 */
public interface IComponent {

	public abstract String getName();

	public abstract String getPath();

	public abstract List<Parameter> getParameters();

}
