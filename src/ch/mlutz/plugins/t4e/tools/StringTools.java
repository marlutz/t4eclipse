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
}
