/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 page and component specification parser
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SpecificationParser extends DefaultHandler {

	// which tag to parse (input)
	private String specificationTagName= null;

	// target class found parsing specification (output)
	private String targetClass= null;

	// ctor
	public SpecificationParser(String specificationTagName) {
		super();
		this.specificationTagName = specificationTagName;
	}

	public String getTargetClass() {
		return targetClass;
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
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// if current element is book , create new book
		// clear tmpValue on start of element

		if (qName.equalsIgnoreCase(specificationTagName) && attributes != null) {
			targetClass= attributes.getValue("class");
		}
	}

	/**
	 * Reset this object's values e.g. before parsing a new specification
	 */
	public void clear() {
		targetClass= null;
	}

	public String getSpecificationTagName() {
		return specificationTagName;
	}

	public void setSpecificationTagName(String specificationTagName) {
		this.specificationTagName = specificationTagName;
	}
}