/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - HyperlinkDetectorDelegate
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.editor.hyperlink;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Interface definition for hyperlink detector delegates that can be used
 * together with DelegatingHyperlinkDetector.
 *
 * @author Marcel Lutz <marcel@mlutz.ch>
 */
public interface IHyperlinkDetectorDelegate {

	/**
	 * Method called on delegate to detect hyperlinks. Should return
	 * a list of SourceRangeHyperlink instances.
	 *
	 * @param document the document for which hyperlinks should be detected
	 * @param content the whole content of the partition where hyperlinks should
	 * 		be detected in
	 * @param region the position relative to content based on which hyperlinks
	 * 		are to be detected, usually the position of the caret / mouse
	 * 		pointer
	 * @return a list of detected SourceRangeHyperlink instances, or
	 * 		<code>null</code>
	 */
	List<SourceRangeHyperlink> detectHyperlinks(IDocument document,
			String content, IRegion region);
}
