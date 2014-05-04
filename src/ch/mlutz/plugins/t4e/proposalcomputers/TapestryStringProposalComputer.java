/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry proposal computer for string environments
 ******************************************************************************/
package ch.mlutz.plugins.t4e.proposalcomputers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
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
public class TapestryStringProposalComputer implements IJavaCompletionProposalComputer {

	private static final IEclipseLog log=
			EclipseLogFactory.create(TapestryProposalComputer.class);

	protected static final boolean LOG_VERBOSE= false;

	// the minimum prefix length for proposals
	private static final int MINIMUM_PREFIX_LENGTH= 1;

	private static final String[] proposalStrings= new String[] {};

	private static final int maxProposalStringLength= 100;

	static {
		int maxLength= 0;
		for (String proposalString: proposalStrings) {
			if (proposalString.length() > maxLength) {
				maxLength= proposalString.length();
			}
		}
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(
			ContentAssistInvocationContext context, IProgressMonitor arg1) {
		final int completionProposalOffset= context.getInvocationOffset();
		IDocument document= context.getDocument();
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

		if (prefix.equals("@InjectPage(\"")) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
			ICompilationUnit unit= javaContext.getCompilationUnit();
			IFile documentFile= null;;
			try {
				documentFile = (IFile) unit.getCorrespondingResource();
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			IFile file= null;

			if (documentFile != null) {
				file= Activator.getDefault().getTapestryIndexer()
					.getRelatedFile(documentFile);
			}

			if (file != null) {
				String proposalString= file.getFullPath().toString();
				result.add(
					/*
					new TapestryCompletionProposal(proposalString,
							"Tapestry 4 Annotation", true, prefixLength,
							completionProposalOffset)
					*/
					new CompletionProposal(proposalString,
							0, 0, 0,
							Activator.getImage("icons/t4e.png"),
							file.getName(),
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

				result.add(
						/*
						new TapestryCompletionProposal(proposalString,
								"Tapestry 4 Annotation", true, prefixLength,
								completionProposalOffset)
						*/
						new CompletionProposal("12345",
								0, 0, 0,
								Activator.getImage("icons/t4e.png"),
								"12345",
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
			log.info("TapestryStringProposalComputer.sessionEnded()");
		}
	}

	@Override
	public void sessionStarted() {
		if (LOG_VERBOSE) {
			log.info("TapestryStringProposalComputer.sessionStarted()");
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