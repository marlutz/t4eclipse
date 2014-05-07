/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - String tools
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTools {
	/**
	 * Joins a collection of strings to a big string, using glue between them
	 *
	 * @param strings the collection of strings to join
	 * @param glue the glue used between the strings
	 * @return the strings joined with glue
	 */
	public static String join(Iterable<String> strings, String glue) {
		StringBuilder sb= new StringBuilder();
		boolean first= true;
		for (String s: strings) {
			if (first) {
				first= false;
			} else {
				sb.append('.');
			}
			sb.append(s);
		}
		return sb.toString();
	}

	public static String getShortestSubstringStartingFrom(String s, char ch) {
		String result= null;
		int lastIndexOfDoubleQuotes= s.lastIndexOf(ch);
		if (lastIndexOfDoubleQuotes >= 0) {
			result= s.substring(lastIndexOfDoubleQuotes + 1);
		}
		return result;
	}

	public static String stripRedundantTagWhitespace(String tagString) {
		String result= tagString.replaceAll("\\s*=\\s*\"", "=\"");

		result= result.replaceAll("\\s+>", ">");

		result= result.replaceAll("[\\n\\r]", " ");

		String newResult;
		while (true) {
			newResult= result.replaceAll("^([^\"]*(?:\"[^\"]*\")[^\"]*\\s)\\s+", "$1");
			if (newResult.equals(result)) break;
			result= newResult;
		}

		return result;
	}

	public static Map<String, String> extractAttributeMap(String tagString) {
		Map<String, String> result= new HashMap<String, String>();
		tagString= stripRedundantTagWhitespace(tagString);

		Matcher attributeMatcher= Pattern
				.compile("\\s([^\\s=\"]+)(?:=\"([^\"]*)\")").matcher(tagString);

		while (attributeMatcher.find()) {
			result.put(attributeMatcher.group(1), attributeMatcher.group(2));
		}

		return result;
	}
}
