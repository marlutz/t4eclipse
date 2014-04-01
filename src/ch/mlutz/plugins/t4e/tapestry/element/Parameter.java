/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 parameter (for page, components, services, ...)
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;


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
public class Parameter {

	private String name;

	private ParameterType type;

	public Parameter(String name, ParameterType type)
	{
		super();
		this.name = name;
		this.type = type;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Parameter other = (Parameter) obj;
		if(name == null)
		{
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		if(type != other.type)
			return false;
		return true;
	}
}
