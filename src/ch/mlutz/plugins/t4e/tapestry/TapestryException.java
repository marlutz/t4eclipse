/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry exception
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry;

/**
 * @author Marcel Lutz
 * @version 1.0 25.03.2014
 */

public class TapestryException extends Exception {

	public TapestryException() {
		super();
	}

	public TapestryException(String message, Throwable cause) {
		super(message, cause);
	}

	public TapestryException(String message) {
		super(message);
	}

	public TapestryException(Throwable cause) {
		super(cause);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -8367933938424115714L;
}
