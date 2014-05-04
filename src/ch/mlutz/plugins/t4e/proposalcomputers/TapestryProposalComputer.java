/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry proposal computer
 ******************************************************************************/
package ch.mlutz.plugins.t4e.proposalcomputers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * @author Marcel Lutz
 * @version 1.0 24.03.2014
 */
public class TapestryProposalComputer implements IJavaCompletionProposalComputer {

	private static final IEclipseLog log=
			EclipseLogFactory.create(TapestryProposalComputer.class);

	protected static final boolean LOG_VERBOSE= false;

	// the minimum prefix length for proposals
	private static final int MINIMUM_PREFIX_LENGTH= 1;

	private static final String[] proposalStrings= new String[] {
		"@Parameter",
		"@InitialValue",
		"@InjectObject",
		"@InjectPage",
		"@InjectState",
		"@InjectComponent",
		"@InjectAsset",
		"@InjectMeta",
		"@InjectScript"
	};

	private static final int maxProposalStringLength;

	static {
		int maxLength= 0;
		for (String proposalString: proposalStrings) {
			if (proposalString.length() > maxLength) {
				maxLength= proposalString.length();
			}
		}
		maxProposalStringLength= maxLength;
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(
			ContentAssistInvocationContext arg0, IProgressMonitor arg1) {
		final int completionProposalOffset= arg0.getInvocationOffset();
		IDocument document= arg0.getDocument();
		// final String lineStart;

		String prefix= "";
		List<ICompletionProposal> result= new ArrayList<ICompletionProposal>();

		try {
			int lineIndex= document.getLineOfOffset(completionProposalOffset);
			int lineStartOffset= document.getLineOffset(lineIndex);
			// int length= completionProposalOffset - lineStartOffset + 1;

			/*
			lineStart= document.get(lineStartOffset, length);
			*/

			// compute the prefix for the proposal: collect characters going
			// backward till first @, line break or maximum proposal length
			int prefixStart= Math.max(lineStartOffset,
					completionProposalOffset - maxProposalStringLength);
			prefix= document.get(prefixStart, completionProposalOffset
					- prefixStart);
			int lastIndexOfAt= prefix.lastIndexOf('@');
			if (lastIndexOfAt > 0) {
				prefix= prefix.substring(lastIndexOfAt);
			}
		} catch (BadLocationException e) {
			log.warn("Could not compute completion proposals: ", e);

			// return empty result
			return result;
		}

		int prefixLength= prefix.length();
		if (prefixLength >= MINIMUM_PREFIX_LENGTH) {
			for (String proposalString: proposalStrings) {
				if (proposalString.startsWith(prefix) &&
						prefixLength < proposalString.length()) {
					result.add(
						/*
						new TapestryCompletionProposal(proposalString,
								"Tapestry 4 Annotation", true, prefixLength,
								completionProposalOffset)
						*/
						new CompletionProposal(proposalString + "(\"\")",
								completionProposalOffset - prefixLength,
								prefixLength,
								proposalString.length() + 2,
								Activator.getImage("icons/t4e.png"),
								proposalString,
								new IContextInformation() {

									@Override
									public String getInformationDisplayString() {
										// tool tip text shown after apply
										return null;
									}

									@Override
									public Image getImage() {
										return Activator.getImage("icons/t4e.png");
									}

									@Override
									public String getContextDisplayString() {
										return "Tapestry 4 Annotation 1";
									}
								},
								"Tapestry 4 Annotation " + proposalString
						)
					);
				}
			}
		}

		return result;
	}

	@Override
	public List<IContextInformation> computeContextInformation(
			ContentAssistInvocationContext arg0, IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return new ArrayList<IContextInformation>();
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
		if (LOG_VERBOSE) {
			log.info("TapestryProposalComputer.sessionEnded()");
		}
	}

	@Override
	public void sessionStarted() {
		if (LOG_VERBOSE) {
			log.info("TapestryProposalComputer.sessionStarted()");
		}
	}
}

/*
 * __dftl_partition_content_type
 * __java_singleline_comment
 * __java_multiline_comment
 * __java_javadoc
 * __java_string
 * __java_character)
 */