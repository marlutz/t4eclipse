/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - PrefixStringListFilter
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools.stringfilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of IStringListFilter that filters by interpretating the filter
 * string as a prefix that is matched against the strings in the list.
 *
 * @author mlutz
 *
 */
public class PrefixStringListFilter implements IStringListFilter {

	@Override
	public void filterStringList(List<String> stringList, String filterString) {
		for (Iterator<String> it= stringList.iterator(); it.hasNext();) {
			String element= it.next();

			// check if element has to be removed
			if (element == null || !element.startsWith(filterString)) {
				it.remove();
			}
		}
	}

	@Override
	public List<String> filterStringListAndReturnFiltered(
			List<String> stringList, String filterString) {
		List<String> result= new ArrayList<String>();
		for (Iterator<String> it= stringList.iterator(); it.hasNext();) {
			String element= it.next();

			// check if element has to be removed
			if (element == null || !element.startsWith(filterString)) {
				result.add(element);
				it.remove();
			}
		}
		return result;
	}

}
