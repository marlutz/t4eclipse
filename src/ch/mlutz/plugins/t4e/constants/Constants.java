/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - general constants
 ******************************************************************************/
package ch.mlutz.plugins.t4e.constants;

import ch.mlutz.plugins.t4e.i18n.Messages;

public interface Constants {

	// The plug-in ID
	public static final String PLUGIN_ID = "ch.mlutz.plugins.t4e"; //$NON-NLS-1$

	// the plugin display name e.g. for message box titles
	public static final String PLUGIN_DISPLAYNAME= Messages.getString("plugin.displayname"); //$NON-NLS-1$

	// the classpath container id
	public static final String CONTAINER_ID= PLUGIN_ID + ".container.T4E_CLASSPATH_CONTAINER"; //$NON-NLS-1$

	// the tapestry editor id
	public static final String TAPESTRY_EDITOR_ID= PLUGIN_ID + ".editors.tapestryEditor"; //$NON-NLS-1$

	public static final int MAX_PROPOSAL_LOOKBEHIND_LENGTH= 255;
}
