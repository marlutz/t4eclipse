/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Pom Dependency
 ******************************************************************************/
package ch.mlutz.plugins.t4e.pom;

import ch.mlutz.plugins.t4e.pom.property.PomProperties;

/**
 * Models a dependency in POM file.
 *
 * @author mlutz
 */
public class Dependency
{
	private boolean resolveVariables= true;

	private String groupId;
	private String artifactId;
	private String version;
	private String scope;

	private PomProperties pomProperties;

	public Dependency() {
		super();
		this.groupId=		null;
		this.artifactId=	null;
		this.version=		null;
		this.scope=			null;
		this.pomProperties= null;
	}

	public Dependency(PomProperties pomProperties) {
		super();
		this.groupId=       null;
		this.artifactId=    null;
		this.version=       null;
		this.scope=         null;
		this.pomProperties= pomProperties;
	}

	public Dependency(String groupId, String artifactId) {
		super();
		this.groupId=		groupId;
		this.artifactId=	artifactId;
		this.version=		null;
		this.scope=			null;
		this.pomProperties= null;
	}

	public String getGroupId() {
		if (resolveVariables && pomProperties != null && groupId != null) {
			return pomProperties.resolveVariablesInString(
				groupId, true);
		} else {
			return groupId;
		}
	}

	public void setGroupId(String groupId)
	{
		this.groupId = groupId;
	}

	public String getArtifactId() {
		if (resolveVariables && pomProperties != null && artifactId != null) {
			return pomProperties.resolveVariablesInString(
				artifactId, true);
		} else {
			return artifactId;
		}
	}

	public void setArtifactId(String artifactId)
	{
		this.artifactId = artifactId;
	}

	public PomProperties getPomProperties()
	{
		return pomProperties;
	}

	public void setPomProperties(PomProperties pomProperties)
	{
		this.pomProperties = pomProperties;
	}

	public boolean isResolveVariables()
	{
		return resolveVariables;
	}

	public void setResolveVariables(boolean resolveVariables)
	{
		this.resolveVariables = resolveVariables;
	}

	public String getVersion() {
		if (resolveVariables && pomProperties != null && version != null) {
			return pomProperties.resolveVariablesInString(
				version, true);
		} else {
			return version;
		}
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getScope() {
		if (resolveVariables && pomProperties != null && scope != null) {
			return pomProperties.resolveVariablesInString(
				scope, true);
		} else {
			return scope;
		}
	}

	public void setScope(String scope)
	{
		this.scope = scope;
	}
}
