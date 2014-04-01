/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - PomProperties
 ******************************************************************************/
package ch.mlutz.plugins.t4e.pom.property;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PomProperties
 *
 * <p>
 * Container for storing properties collected from inside a pom
 * and resolving them for clients.
 * </p>

 * @author mlutz
 */
public class PomProperties {
	private static final String PROJECT_PROPERTIES_PREFIX= "project.";

	private Map<String, String> projectProperties;
	private Map<String, String> properties;

	public PomProperties() {
		projectProperties= new HashMap<String, String>();
		properties= new HashMap<String, String>();
	}

	public void addProjectProperty(String key, String value) {
		projectProperties.put(key, value);
	}

	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

	public void clear() {
		projectProperties.clear();
		properties.clear();
	}

	public String getProperty(String key) {

		// try pom project properties first for keys like project.xyz
		if (key.startsWith(PROJECT_PROPERTIES_PREFIX)) {
			// remove matched prefix
			String projectPropertiesKey= key.substring(
				PROJECT_PROPERTIES_PREFIX.length());
			if (projectProperties.containsKey(projectPropertiesKey)) {
				return projectProperties.get(projectPropertiesKey);
			}
		}

		// try pom user properties next
		if (properties.containsKey(key)) {
			return properties.get(key);
		}

		// at last, try system properties
		return System.getProperty(key);
	}


	public String resolveVariablesInString(String stringWithVariables,
		boolean removeUnmatched) {

		Pattern variablePattern= Pattern.compile("\\$\\{([^\\}]+)\\}");
		Matcher matcher= variablePattern.matcher(stringWithVariables);
		StringBuffer sb = new StringBuffer();
		String variableKey;
		String variableValue;
		while (matcher.find()) {
			variableKey= matcher.group(1);
			variableValue= getProperty(variableKey);

			if (variableValue != null) {
				// variable value found ==> insert
				matcher.appendReplacement(sb, variableValue);
			} else {
				// no value for variable found
				if (removeUnmatched) {
					matcher.appendReplacement(sb, "");
				} else {
					matcher.appendReplacement(sb, matcher.group());
				}
			}
		}
		matcher.appendTail(sb);

		return sb.toString();
	}

	// getters and setters
	public Map<String, String> getProjectProperties() {
		return projectProperties;
	}

	public void setProjectProperties(Map<String, String> projectProperties) {
		this.projectProperties= projectProperties;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties= properties;
	}
}
