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
 * Models a Tapestry parameter used for pages, components, services, etc.
 *
 * @author mlutz
 *
 */
public class Parameter {

	private String name;

	private ParameterType type;

	private ParameterDirection direction;

	private boolean required;

	private Object defaultValue;

	private IComponent parent;

	public Parameter(String name, ParameterType type) {
		super();
		this.name = name;
		this.type = type;
	}

	public Parameter(String name, ParameterType type,
			ParameterDirection direction, boolean required,
			Object defaultValue) {
		super();
		this.name = name;
		this.type = type;
		this.direction = direction;
		this.required = required;
		this.defaultValue = defaultValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (required ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Parameter)) {
			return false;
		}
		Parameter other = (Parameter) obj;
		if (defaultValue == null) {
			if (other.defaultValue != null) {
				return false;
			}
		} else if (!defaultValue.equals(other.defaultValue)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (required != other.required) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	// getters and setters

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParameterType getType() {
		return type;
	}

	public void setType(ParameterType type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public ParameterDirection getDirection() {
		return direction;
	}

	public void setDirection(ParameterDirection direction) {
		this.direction = direction;
	}

	public IComponent getParent() {
		return parent;
	}

	public void setParent(IComponent parent) {
		this.parent = parent;
	}
}
