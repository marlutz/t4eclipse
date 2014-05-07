/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 parameter type
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

/**
 * Models the direction of data flow that a parameter can have (in, in/out,
 * out).
 *
 * @author mlutz
 *
 */
public enum ParameterDirection {
	IN("in"), IN_OUT("in/out"), OUT("out");

	private String value;

	private ParameterDirection(String value) {
		this.value= value;
	}

	public String getValue() {
		return value;
	}
}
