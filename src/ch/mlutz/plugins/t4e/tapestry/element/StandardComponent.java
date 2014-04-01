/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 standard component
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

/**
 * StandardComponent
 *
 * Models standard Tapestry components with no need for a backing HTML file
 *
 * @author mlutz
 *
 */
public class StandardComponent extends TapestryElement
	implements IComponent {

	/**
	 *
	 */
	private static final long serialVersionUID = 2152442305693958086L;

	private final String name;

	private List<Parameter> parameters= new ArrayList<Parameter>();

	public StandardComponent(String name) {
		this.name= name;
	}

	public void addParameter(Parameter parameter) {
		parameters.add(parameter);
	}

	public List<Parameter> getParameters() {
		@SuppressWarnings("unchecked")
		List<Parameter> unmodifiableList = Collections.unmodifiableList(parameters);
		return unmodifiableList;
	}

	public void removeParameter(Parameter parameter) {
		parameters.remove(parameter);
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return name;
	}

	/* (non-Javadoc)
	 * @see ch.mlutz.plugins.t4e.tapestry.element.TapestryElement#getType()
	 */
	@Override
	public ElementType getType() {
	   return ElementType.COMPONENT;
	}
}
