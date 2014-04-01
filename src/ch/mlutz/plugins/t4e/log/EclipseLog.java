/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - EclipseLog implementation
 ******************************************************************************/
package ch.mlutz.plugins.t4e.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Implementation of LogProvider; forwards incoming messages to the logProvider
 * set with its getter
 *
 * @author Marcel Lutz
 *
 */
public class EclipseLog implements IEclipseLog {

	private Class<?> logSourceClass;
	private ILog log;

	public EclipseLog(Class<?> logSourceClass, ILog log) {
		this.logSourceClass= logSourceClass;
		this.log= log;
	}

	public void info(String s) {
		status(IStatus.INFO, s);
	}

	public void info(String s, Throwable exception) {
		status(IStatus.INFO, s, exception);
	}

	public void warn(String s) {
		status(IStatus.WARNING, s);
	}

	public void warn(String s, Throwable exception) {
		status(IStatus.WARNING, s, exception);
	}

	public void error(String s) {
		status(IStatus.ERROR, s);
	}

	public void error(String s, Throwable exception) {
		status(IStatus.ERROR, s, exception);
	}

	public void status(int statusCode, String s) {
		log.log(new Status(statusCode,
				logSourceClass.getName(),
				s));
	}

	public void status(int statusCode, String s, Throwable exception) {
		String exceptionString= null;
		if (exception != null) {
			StringWriter sw= new StringWriter();
			PrintWriter pw= new PrintWriter(sw);
			exception.printStackTrace(pw);
			exceptionString= sw.toString();
		}

		log.log(new Status(statusCode,
				logSourceClass.getName(),
				s + "\n" + exceptionString));
	}

	// getters and setters
	public ILog getLog() {
		return log;
	}

	public void setLog(ILog log) {
		this.log= log;
	}
}

