/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - constants for commands
 ******************************************************************************/
package ch.mlutz.plugins.t4e.constants;

public interface Command {
	String SWITCH_TO_COMPLEMENT_FILE = "ch.mlutz.plugins.t4e.commands.switchToComplementFile";
	String REFRESH = "ch.mlutz.plugins.t4e.commands.refresh";
	String REFRESH_ALL = "ch.mlutz.plugins.t4e.commands.refreshAll";
	String ANALYZE = "ch.mlutz.plugins.t4e.commands.analyzeJavaSourceFile";
	String ADD_CLASSPATH_CONTAINER = "ch.mlutz.plugins.t4e.commands.addT4eClasspathContainer";
	String REMOVE_CLASSPATH_CONTAINER = "ch.mlutz.plugins.t4e.commands.removeT4eClasspathContainer";
	String CLEAR_TAPESTRY_INDEX = "ch.mlutz.plugins.t4e.commands.clearTapestryIndex";
}
