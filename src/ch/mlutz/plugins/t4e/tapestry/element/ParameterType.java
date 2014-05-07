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
 * Models the different parameter types that tapestry standard components can
 * have.
 *
 * @author mlutz
 *
 */
// TODO: add type-dependent comparator for completion options
public enum ParameterType {
	STRING("String"), OBJECT("Object"), BOOLEAN("Boolean"),
	LISTENER("Listener"), COLLECTION("Collection"), INTEGER("Integer"),
	DATE("Date"), FORMAT("Format"), VALIDATOR("Validator"),
	TRANSLATOR("Translator"), ASSET("Asset"), COMPONENT("Component"),
	RENDER("Render"), EXCEPTIONDESCRIPTION("ExceptionDescription"),
	PRIMARYKEYCONVERTER("PrimaryConverter"),
	VALIDATIONDELEGATE("ValidationDelegate"), SUBMITTYPE("SubmitType"),
	POINT("Point"), INSERTTEXTMODE("InsertTextMode"), NAMESPACE("Namespace"),
	STRING_OR_ASSET("String or Asset"), BLOCK("Block"), MAP("Map"),
	STRING_OR_COLLECTION("String or Collection"), UPLOADFILE("UploadFile");

	private String value;

	private ParameterType(String value) {
		this.value= value;
	}

	public String getValue() {
		return value;
	}
}
