/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 app description parser
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AppSpecificationParser extends DefaultHandler {
	private Map<String, String> metaMap= new HashMap<String, String>();

	private MetaTag metaTmp;

	public String getMeta(String key) {
		return metaMap.get(key);
	}

	public String[] getPageClassPackages() {
		String value= metaMap.get("org.apache.tapestry.page-class-packages");
		if (value == null) {
			return null;
		}
		return value.split(",");
	}

	public String[] getComponentClassPackages() {
		String value= metaMap.get("org.apache.tapestry.component-class-packages");
		if (value == null) {
			return null;
		}
		return value.split(",");
	}

	public String getMessagesEncoding() {
		return metaMap.get("org.apache.tapestry.messages-encoding");
	}

	public String[] getAcceptedLocales() {
		String value= metaMap.get("org.apache.tapestry.accepted-locales");
		if (value == null) {
			return null;
		}
		return value.split(",");
	}

	public void parse(InputStream specificationContent) throws UnsupportedEncodingException {

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
	public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
		// if current element is book , create new book
		// clear tmpValue on start of element

		if (elementName.equalsIgnoreCase("meta")) {
			metaTmp = new MetaTag();
			metaTmp.parseAttributes(attributes);
		}
	}
	@Override
	public void endElement(String s, String s1, String element) throws SAXException {
		if (element.equals("meta")) {
			 metaMap.put(metaTmp.getKey(), metaTmp.getValue());
		}
	}

	class MetaTag {
		private Map<String, String> attributeMap= new HashMap<String, String>();

		public String getKey() {
			return attributeMap.get("key");
		}

		public String getValue() {
			return attributeMap.get("value");
		}

		public void parseAttributes(Attributes attributes) {
			attributeMap.put("key", attributes.getValue("key"));
			attributeMap.put("value", attributes.getValue("value"));
		}
	}

	/**
	 * Reset this objects values e.g. before parsing a new specifications
	 */
	public void clear() {
		metaMap.clear();
	}
}