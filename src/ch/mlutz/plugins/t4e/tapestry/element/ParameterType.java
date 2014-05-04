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
	STRING, OBJECT, BOOLEAN, LISTENER, COLLECTION, INTEGER, DATE, FORMAT,
	VALIDATOR, TRANSLATOR, ASSET, COMPONENT, RENDER, EXCEPTIONDESCRIPTION,
	PRIMARYKEYCONVERTER, VALIDATIONDELEGATE, SUBMITTYPE, POINT, INSERTTEXTMODE,
	NAMESPACE, STRING_OR_ASSET, BLOCK, MAP, STRING_OR_COLLECTION, UPLOADFILE;
}
