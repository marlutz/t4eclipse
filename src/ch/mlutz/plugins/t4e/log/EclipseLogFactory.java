/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - EclipseLogFactory implementation
 ******************************************************************************/
package ch.mlutz.plugins.t4e.log;

import org.eclipse.core.runtime.ILog;

/**
 *
 * @author Marcel Lutz
 *
 */
public class EclipseLogFactory {

	private static ILog log= null;

	public static IEclipseLog create(Class<?> logSourceClass) {
		return new EclipseLog(logSourceClass, log);
	}

	// getters and setters
	public static ILog getLog() {
		return log;
	}

	public static void setLog(ILog log) {
		EclipseLogFactory.log = log;
	}
}

