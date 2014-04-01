/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 element type
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

/**
 * @author Marcel Lutz
 * @version 1.0 23.03.2014
 */

public enum ElementType {
	PAGE("page-specification"),
	COMPONENT("component-specification"),
	SERVICE("service-point"),
	ASSET("asset"),
	STATE("state")
	;

	private final String specificationTagName;

	private ElementType(String specificationTagName) {
		this.specificationTagName= specificationTagName;
	}

	public String getSpecificationTagName() {
		return specificationTagName;
	}
}
