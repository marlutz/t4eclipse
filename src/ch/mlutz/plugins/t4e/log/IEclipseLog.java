/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - IEclipseLog interface
 ******************************************************************************/
package ch.mlutz.plugins.t4e.log;

import org.eclipse.core.runtime.ILog;

public interface IEclipseLog {

	public ILog getLog();

	public void info(String s);

	public void info(String s, Throwable exception);

	public void warn(String s);

	public void warn(String s, Throwable exception);

	public void error(String s);

	public void error(String s, Throwable exception);
}
