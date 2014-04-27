/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - DelegatingHyperlinkDetector
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.editor.hyperlink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * HyperlinkDetector that forwards detectHyperlinks requests to its delegates.
 *
 * @author Marcel Lutz
 */
public class DelegatingHyperlinkDetector extends AbstractHyperlinkDetector {

	/**
	 * the list of accepted partition types; the partition that surrounds the
	 * region that hyperlinks should be generated for must have one of these
	 * types, otherwise the request will be ignored.
	 */
	private Set<String> acceptedPartitionTypes= null;

	/**
	 * the list of delegates that detectHyperlinks requests will be forwarded to
	 */
	private List<IHyperlinkDetectorDelegate> delegateList= null;

	/**
	 * No-Arg constructor
	 */
	public DelegatingHyperlinkDetector() {}

	/**
	 * Constructor with a parameter for initializing the accepted partition
	 * types.
	 *
	 * @param acceptedPartitionTypes the initial value
	 */
	public DelegatingHyperlinkDetector(Set<String> acceptedPartitionTypes) {
		super();
		this.acceptedPartitionTypes = acceptedPartitionTypes;
	}

	/**
	 * the log
	 */
	private static final IEclipseLog log=
		EclipseLogFactory.create(DelegatingHyperlinkDetector.class);

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region,
			boolean canHandleMultipleLinks) {

		IDocument document= viewer.getDocument();

		// determine the partition that surrounds the region given
		ITypedRegion partition= null;
		try {
			partition= document.getPartition(region.getOffset());
		} catch(BadLocationException e) {
			log.warn("Could not detect hyperlinks: ", e);
		}

		if (partition == null || (acceptedPartitionTypes != null
				&& !acceptedPartitionTypes.contains(partition.getType()))) {
			// either partition could not be determined
			// or its type is not accepted
			return null;
		}

		// get the partition's content
		String partitionContent;
		try {
			partitionContent= document.get(partition.getOffset(),
					partition.getLength());
		} catch (BadLocationException e) {
			log.warn("Could not detect hyperlinks and get partition content:",
					e);
			return null;
		}

		List<SourceRangeHyperlink> resultList= new ArrayList<SourceRangeHyperlink>();
		List<SourceRangeHyperlink> partialResultList;

		Region transformedRegion= new Region(region.getOffset() - partition.getOffset(), region.getLength());

		// loop through delegates
		if (delegateList != null)  {
			for (IHyperlinkDetectorDelegate delegate: delegateList) {
				partialResultList= delegate.detectHyperlinks(document,
						partitionContent, transformedRegion);
				if (partialResultList != null) {
					resultList.addAll(partialResultList);
				}
			}
		}

		// transform the list to an array for returning it
		if (resultList.size() > 0) {
			for (SourceRangeHyperlink hyperlink: resultList) {
				hyperlink.setPartitionOffset(partition.getOffset());
			}
			return resultList.toArray(new IHyperlink[resultList.size()]);
		} else {
			return null;
		}
	}

	/**
	 * Adds a delegate to the list of delegates.
	 *
	 * @param delegate the delegate to add
	 */
	public void addDelegate(IHyperlinkDetectorDelegate delegate) {
		if (delegateList == null) {
			delegateList= new ArrayList<IHyperlinkDetectorDelegate>();
		}
		delegateList.add(delegate);
	}

	/**
	 * Removes a delegate from the list of delegates.
	 *
	 * @param delegate the delegate to remove
	 */
	public void removeDelegate(IHyperlinkDetectorDelegate delegate) {
		if (delegateList != null) {
			delegateList.remove(delegate);
		}
	}

	// getters and setters

	public Set<String> getAcceptedPartitionTypes() {
		return acceptedPartitionTypes;
	}

	public void setAcceptedPartitionTypes(Set<String> acceptedPartitionTypes) {
		this.acceptedPartitionTypes= acceptedPartitionTypes;
	}

	public List<IHyperlinkDetectorDelegate> getDelegateList() {
		return delegateList;
	}

	public void setDelegateList(List<IHyperlinkDetectorDelegate> delegateList) {
		this.delegateList= delegateList;
	}
}
