/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - interface IStringListFilter
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools.stringfilter;

import java.util.List;

/**
 * Interface for filtering lists of strings.
 *
 * @author mlutz
 *
 */
public interface IStringListFilter {

	/**
	 * Filters and modifies the input stringList by the string filter.
	 *
	 * @param stringList the input <code>List&lt;String&gt;</code> to be
	 * 		filtered
	 * @param filter the string used to filter the list
	 */
	void filterStringList(List<String> stringList, String filterString);

	/**
	 * Filters and modifies the input stringList by the string filter. Returns
	 * a list of strings that have been removed from stringList.
	 *
	 * @param stringList the input <code>List&lt;String&gt;</code> to be
	 * 		filtered
	 * @param filter
	 * @return the list of strings that have been removed from
	 * 		<code>stringList</code>
	 */
	List<String> filterStringListAndReturnFiltered(List<String> stringList,
			String filterString);
}
