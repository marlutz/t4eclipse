/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - PomParser
 ******************************************************************************/
package ch.mlutz.plugins.t4e.pom.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.mlutz.plugins.t4e.pom.Dependency;
import ch.mlutz.plugins.t4e.pom.property.PomProperties;

public class PomParser extends DefaultHandler {

	private static final String DEPENDENCY_TAG_NAME=	"dependency";
	private static final String DEPENDENCIES_TAG_NAME=	"dependencies";
	private static final String DEPENDENCY_MANAGEMENT_TAG_NAME= "dependencyManagement";

	private static final String GROUPID_TAG_NAME=		"groupId";
	private static final String ARTIFACTID_TAG_NAME=	"artifactId";
	private static final String VERSION_TAG_NAME=		"version";

	private List<Dependency> dependencies= new ArrayList<Dependency>();
	private List<Dependency> dependencyManagement= new ArrayList<Dependency>();

	private final PomProperties pomProperties;

	// the following variables are used during XML traversal
	// current element "call stack"
	private Dependency currentDependency= null;
	private List<String> elementHierarchy= new ArrayList<String>();
	/*
	private Map<String, String> projectProperties= new HashMap<String, String>();
	private Map<String, String> properties= new HashMap<String, String>();
	*/
	private StringBuilder stringBuilder= null;

	public PomParser() {
		pomProperties= new PomProperties();
	}

	public boolean isChildOfDependencies() {
		int elementHierarchySize= elementHierarchy.size();
		return (elementHierarchySize >= 2) &&
			DEPENDENCIES_TAG_NAME.equals(
				elementHierarchy.get(elementHierarchySize - 2)
			);
	}

	public boolean isGrandChildOfDependencyManagement() {
		int elementHierarchySize= elementHierarchy.size();
		return (elementHierarchySize >= 3) &&
			DEPENDENCY_MANAGEMENT_TAG_NAME.equals(
				elementHierarchy.get(elementHierarchySize - 3)
			);
	}

	public String getCurrentElementName() {
		int elementHierarchySize= elementHierarchy.size();
		if (elementHierarchySize > 0) {
			return elementHierarchy.get(elementHierarchySize - 1);
		} else {
			return null;
		}
	}

	public void parse(InputStream specificationContent)
		throws UnsupportedEncodingException {

		// parse
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser parser = factory.newSAXParser();
			parser.parse(specificationContent, this);
		} catch (ParserConfigurationException e) {
			System.out.println("ParserConfig error");
		} catch (SAXException e) {
			System.out.println("SAXException : xml not well formed");
		} catch (IOException e) {
			System.out.println("IO error");
		}
	}

	@Override
	public void startElement(String namespaceURI, String localName,
		String elementName, Attributes attributes)
			throws SAXException {
		elementHierarchy.add(elementName);

		if (currentDependency == null) {
			if (DEPENDENCY_TAG_NAME.equals(elementName)) {

				// start of dependency element
				if (isChildOfDependencies()) {
					currentDependency= new Dependency(pomProperties);
					if (isGrandChildOfDependencyManagement()) {
						dependencyManagement.add(currentDependency);
					} else {
						dependencies.add(currentDependency);
					}
				}
			} else if (currentElementMatches("/project/properties/*")
				|| currentElementMatches("/project/groupId")
				|| currentElementMatches("/project/artifactId")
				|| currentElementMatches("/project/name")
				|| currentElementMatches("/project/version")
				|| currentElementMatches("/project/packaging")
			) {
				// record properties
				stringBuilder= new StringBuilder();
			}
		} else if (GROUPID_TAG_NAME.equals(elementName)
			|| ARTIFACTID_TAG_NAME.equals(elementName)
			|| VERSION_TAG_NAME.equals(elementName)) {

			// currentDependency != null
			// create stringBuilder for dependency sub-element
			stringBuilder= new StringBuilder();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String content = new String(ch, start, length);
		if (!content.trim().equals("")) {
			if (stringBuilder != null) {
				stringBuilder.append(content);
			}
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName,
			String element) throws SAXException {

		if (currentDependency != null) {
			String currentElementName= getCurrentElementName();

			if (stringBuilder != null) {
				if (GROUPID_TAG_NAME.equals(currentElementName)) {
					currentDependency.setGroupId(stringBuilder.toString());
				} else if (ARTIFACTID_TAG_NAME.equals(currentElementName)) {
					currentDependency.setArtifactId(stringBuilder.toString());
				} else if (VERSION_TAG_NAME.equals(currentElementName)) {
					currentDependency.setVersion(stringBuilder.toString());
				}
				stringBuilder= null;
			} else if (DEPENDENCY_TAG_NAME.equals(currentElementName)) {
				// end of dependency element
				currentDependency= null;
			} else {
				// seems to be some other element in current dependency ==> ignore
			}
	   } else {
			// currentDependency is null
			if (stringBuilder != null) {
				if (currentElementMatches("/project/properties/*")) {
					// user defined pom property
					pomProperties.addProperty(getCurrentElementName(),
						stringBuilder.toString());
				} else if (currentElementMatches("/project/*")) {
					// most probably one of groupId, artifactId, name, version,
					// packaging
					pomProperties.addProjectProperty(getCurrentElementName(),
						stringBuilder.toString());
				}

				stringBuilder= null;
			}
		}

		elementHierarchy.remove(elementHierarchy.size() - 1);
	}

	/**
	 * Reset this objects values e.g. before parsing a new specifications
	 */
	public void clear() {
		dependencies.clear();
		dependencyManagement.clear();
		pomProperties.clear();
	}

	// getters and setters
	public List<Dependency> getDependencies()
	{
		return dependencies;
	}

	public List<Dependency> getDependencyManagement()
	{
		return dependencyManagement;
	}

	/**
	 * The asterisk '*' is a wildcard that matches any tag.
	 *
	 * @param path the path to match consisting of segments separated by
	 *      slashes, e.g /foo/bar/baz; a leading / will let the search start
	 *      at the root; otherwise, search will be backwards starting at the
	 *      current element and match the ancestors
	 * @return
	 */
	public boolean currentElementMatches(String path) {
		// trim and remove trailing slashes
		String p= path.trim().replaceAll("[/]+$", "");

		if ("".equals(p)) {
			// don't match empty string
			return false;
		}

		// p is not empty here

		if (".".equals(p)) {
			// match the "."
			return true;
		}

		if (p.charAt(0) == '/') {
			// match forward from root
			String[] pathArray= p.substring(1).split("/");

			if (elementHierarchy.size() != pathArray.length) {
				// exit quickly if sizes don't match
				return false;
			}

			for (int i= 0; i < pathArray.length; ++i) {
				if (!elementHierarchy.get(i).equals(pathArray[i])
					&& !"*".equals(pathArray[i])) {
					return false;
				}
			}
			return true;
		} else {
			// match backward from element
			String[] pathArray= p.split("/");

			if (elementHierarchy.size() < pathArray.length) {
				// exit quickly element hierarchy is not as deep as path
				return false;
			}

			int elementHierarchyOffset= elementHierarchy.size() - pathArray.length;

			for (int i= 0; i < pathArray.length; ++i) {
				if (!elementHierarchy.get(i + elementHierarchyOffset)
					.equals(pathArray[i]) && !"*".equals(pathArray[i])) {
					return false;
				}
			}
			return true;
		}
	}

	// getters and setters

	public Map<String, String> getProjectProperties() {
		return pomProperties.getProjectProperties();
	}

	public Map<String, String> getProperties() {
		return pomProperties.getProperties();
	}
}