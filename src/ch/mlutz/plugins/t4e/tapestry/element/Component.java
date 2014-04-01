/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 component
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;

import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import java.util.Collections;

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
public class Component extends TapestryHtmlElement implements IComponent {

	/**
	 *
	 */
	private static final long serialVersionUID = -8067998249912120016L;

	private List<Parameter> parameters= new ArrayList<Parameter>();

	public Component(TapestryModule parent, ElementType type,
			IFile htmlFile) {
		super(parent, type, htmlFile);
	}

	public Component(TapestryModule parent, ElementType type,
			IFile htmlFile, IFile specification,
			ICompilationUnit javaCompilationUnit) {
		super(parent, type, htmlFile, specification, javaCompilationUnit);
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
}
