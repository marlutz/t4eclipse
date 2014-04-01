/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Collection tools
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionTools {
	/**
	 * @param c
	 * @param <T>
	 * @return
	 */
	public static
	<T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
	  List<T> list = new ArrayList<T>(c);
	  java.util.Collections.sort(list);
	  return list;
	}
}
