/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 service
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;

/**
 * @author Marcel Lutz
 * @version 1.0 24.03.2014
 */

public class Service extends TapestryElement {

	/**
	 *
	 */
	private static final long serialVersionUID = 8451777898150008428L;

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			Service.class);

	final TapestryModule parent;
	String className;
	String interfaceName;
	String name;

	public Service(TapestryModule parent, String name,
			String interfaceName) {
		super();

		this.interfaceName= interfaceName;
		this.name= name;
		this.parent= parent;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getPath() {
		StringBuilder sb= new StringBuilder();
		sb.append(parent.getModuleId());
		if (sb.length() > 0) {
			sb.append(".");
		}
		sb.append(name);
		return sb.toString();
	}

	@Override
	public ElementType getType() {
		return ElementType.SERVICE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
		if (!(obj instanceof Service)) {
			return false;
		}
		Service other = (Service) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		return true;
	}
}
