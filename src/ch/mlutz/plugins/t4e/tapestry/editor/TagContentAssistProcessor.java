/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - content assist processor for tag content type
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tapestry.element.IComponent;
import ch.mlutz.plugins.t4e.tapestry.element.ParameterType;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryHtmlElement;
import ch.mlutz.plugins.t4e.tools.CollectionTools;
import ch.mlutz.plugins.t4e.tools.StringTools;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagContentAssistProcessor implements IContentAssistProcessor {

	private static final IEclipseLog log=
			EclipseLogFactory.create(TagContentAssistProcessor.class);

	// private static final String SIGNATURE_PATTERN_STRING= "Q(.+?)(?:(<)(.+)(>))?;";

	private static final String COMMA_PATTERN_STRING= ";Q";

	private static final String COMMA_PATTERN_REPLACEMENT= ";, Q";

	private static final String GENERICS_PATTERN_STRING= "(.*)<([^<>]*)>(.*)";

	private static final Pattern genericsPattern=
			Pattern.compile(GENERICS_PATTERN_STRING);

	// private static final String GENERICS_PATTERN_REPLACEMENT= ";, Q";

	private static final String SIGNATURE_PATTERN_STRING= "Q([^;]+?);";

	private static final String SIGNATURE_PATTERN_REPLACEMENT= "$1";



	private static final Pattern signaturePattern=
			Pattern.compile(SIGNATURE_PATTERN_STRING);

	// ctor
	public TagContentAssistProcessor(TapestryTagScanner xmlTagScanner) {}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int caretOffset) {
		IDocument document= viewer.getDocument();
		String attributePrefix= "";
		List<ICompletionProposal> result= new ArrayList<ICompletionProposal>();

		try {
			int lineIndex= document.getLineOfOffset(caretOffset);
			int lineStartOffset= document.getLineOffset(lineIndex);

			// compute the prefix for the proposal: collect characters going
			// backward till first @, line break or maximum proposal length
			int attributePrefixStart= Math.max(lineStartOffset,
					caretOffset - Constants.MAX_PROPOSAL_LOOKBEHIND_LENGTH);
			attributePrefix= document.get(attributePrefixStart, caretOffset
					- attributePrefixStart);
		} catch (BadLocationException e) {
			log.warn("Could not compute completion proposals: ", e);

			// return empty result
			return getEmptyResult();
		}

		int attributeStartIndex= 0;
		for (char ch: new char[] {' ', '\t', '\n', '\r', '<', '>'}) {
			int lastIndex= attributePrefix.lastIndexOf(ch);
			if (lastIndex >= attributeStartIndex) {
				attributeStartIndex= lastIndex+1;
			}
		}

		if (attributeStartIndex > 0) {
			attributePrefix= attributePrefix.substring(attributeStartIndex);
		}

		String attributeValue= StringTools.getShortestSubstringStartingFrom(
				attributePrefix, '"');

		if (attributeValue == null) {
			// return empty result
			return getEmptyResult();
		}

		IFile documentFile= Activator.getDefault().getTapestryIndex()
			.getDocumentToFileMapping(document);

		if (attributePrefix.startsWith("jwcid=\"")) {
			String jwcidPrefix= StringTools.getShortestSubstringStartingFrom(
					attributeValue, '@');
			if (jwcidPrefix != null) {
				return computeJwcidCompletionProposals(caretOffset, jwcidPrefix,
					document, documentFile);
			} else {
				return getEmptyResult();
			}
		} else if (attributeValue.startsWith("ognl:")) {
			String ognlPrefix= StringTools.getShortestSubstringStartingFrom(
					attributeValue, ':');
			if (ognlPrefix != null) {
				return computeOgnlCompletionProposals(caretOffset, ognlPrefix,
					document, documentFile);
			} else {
				return getEmptyResult();
			}
		} else {
			// get jwcid via regex
			// get sensible attributes for this jwcid
		}

		return getEmptyResult();
	}

	protected ICompletionProposal[] computeJwcidCompletionProposals(
			int offset,
			String jwcIdPrefix,
			IDocument document,
			IFile documentFile) {
		List<ICompletionProposal> result= new ArrayList<ICompletionProposal>();
		int jwcIdPrefixLength= jwcIdPrefix.length();

		if (documentFile != null) {
			String[] jwcIds= getJwcIds(documentFile);
			for (String jwcId: jwcIds) {
				if (jwcId.startsWith(jwcIdPrefix)) {
					result.add(
							new CompletionProposal(jwcId + "\"",
									offset - jwcIdPrefixLength,
									jwcIdPrefixLength,
									jwcId.length() + 1,
									Activator.getImage("icons/t4e.png"),
									"@" + jwcId,
									getContextInformation("", ""),
									"@" + jwcId
							)
					);
				}
			}
		}

		try
		{
			ITypedRegion typedRegion= document.getPartition(offset);

			result.add(
				new CompletionProposal("",
						offset,
						0,
						0,
						Activator.getImage("icons/t4e.png"),
						typedRegion.getType() + " / "
							+ typedRegion.getOffset() + " / "
							+ typedRegion.getLength()
							+ "'" + document.get(typedRegion.getOffset(),
								typedRegion.getLength()) + "'",
						getContextInformation("", ""),
						"Partition information"
				)
			);
		}
		catch(BadLocationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result.toArray(new ICompletionProposal[result.size()]);
	}

	protected ICompletionProposal[] computeOgnlCompletionProposals(
			int offset,
			String ognlPrefix,
			IDocument document,
			IFile documentFile) {
		List<ICompletionProposal> result= new ArrayList<ICompletionProposal>();
		int ognlPrefixLength= ognlPrefix.length();

		TapestryIndex tapestryIndex= Activator.getDefault().getTapestryIndex();
		ICompilationUnit compilationUnit= tapestryIndex
			.getRelatedCompilationUnit(documentFile);

		if (compilationUnit != null) {
			Map<ICompletionProposal, Integer> proposalScoreMap=
					new HashMap<ICompletionProposal, Integer>();

			String suffix= "\"";
			int additionalCursorOffset= 1;
			try {
				if (offset < document.getLength() - 1 && document.get(offset, 1).equals("\"")) {
					suffix= "";
				}
			} catch (BadLocationException e) {
				log.warn("Could not compute suffix of completion proposals: ", e);
			}

			try {
				IType[] types= compilationUnit.getTypes();

				for (IType type: types) {
					if (type.isClass() && Flags.isPublic(type.getFlags())) {
						IMethod[] methods = type.getMethods();
						for (IMethod method: methods) {
							String completionProposalString=
									method.getElementName() + "()";

							String ognlProposalString= completionProposalString
									.replaceAll("^get(.+)\\(\\)?$", "$1");

							if (!ognlProposalString.equals(
									completionProposalString)) {
								// ognlProposalString is different from
								// original proposal => make sure first
								// character is lower case
								ognlProposalString= Character.toLowerCase(
										ognlProposalString.charAt(0))
										+ ognlProposalString.substring(1);
								completionProposalString= ognlProposalString;
							}

							if (completionProposalString.startsWith(ognlPrefix)) {
								String displayName= completionProposalString;
								String translatedSignature=
										translateSignatureToType(method.getReturnType());
								if (translatedSignature != null) {
									displayName+= " - " + translatedSignature;
								}

								ICompletionProposal completionProposal=
									new CompletionProposal(
										completionProposalString + suffix,
										offset - ognlPrefixLength,
										ognlPrefixLength,
										completionProposalString.length()
											+ additionalCursorOffset,
										Activator.getImage("icons/t4e.png"),
										displayName,
										getContextInformation("", ""),
										completionProposalString
									);

								// compute score for this proposal
								int proposalScore= getProposalScoreForMethod(method,
										ParameterType.STRING);

								proposalScoreMap.put(completionProposal,
										proposalScore);

								result.add(completionProposal);
							}
						}
					}
				}
			}
			catch(JavaModelException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Collections.sort(result, new ScoreComparator<Integer>(proposalScoreMap));
		}

		return result.toArray(new ICompletionProposal[result.size()]);
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return ":@ ".toCharArray();
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	protected ICompletionProposal[] getEmptyResult() {
		return new ICompletionProposal[0];
	}

	protected String[] getJwcIds(IFile tapestryHtmlFile) {
		List<String> jwcIdList= new ArrayList<String>();
		TapestryIndex tapestryIndex= Activator.getDefault().getTapestryIndex();

		// add standard components
		for (IComponent component: tapestryIndex.getStandardComponents()) {
			jwcIdList.add(component.getPath());
		}

		// add components of module
		TapestryModule module= tapestryIndex.getModuleForResource(tapestryHtmlFile);
		Set<TapestryHtmlElement> components= module.getComponents();
		List<TapestryHtmlElement> componentsSorted=
			CollectionTools.asSortedList(components);

		for (TapestryHtmlElement component: componentsSorted) {
			jwcIdList.add(component.getPath());
		}
		return jwcIdList.toArray(new String[jwcIdList.size()]);
	}

	protected IContextInformation getContextInformation(
			String contextDisplayString,
			String informationDisplayString) {
		return new ContextInformation(Activator.getImage("icons/t4e.png"),
				contextDisplayString, informationDisplayString);
	}

	protected int getProposalScoreForMethod(IMethod method,
			ParameterType requirement) {
		int score= 0;
		String returnType;
		try {
			returnType = method.getReturnType();

			if ("QString;".equals(returnType)) {
				score= 300;
			} else if ("B".equals(returnType)	// byte
					|| "C".equals(returnType)	// char
					|| "D".equals(returnType)	// double
					|| "F".equals(returnType)	// float
					|| "I".equals(returnType)	// int
					|| "J".equals(returnType)	// long
					|| "S".equals(returnType)	// short
					|| "Z".equals(returnType)	// boolean
					|| "QInteger;".equals(returnType)	// Integer
			) {
				score= 200;
			} else if ("V".equals(returnType)) {
				// void
				score= 0;
			} else {
				score= 100;
			}
		} catch (JavaModelException e) {
			score= -100;
		}
		return score;
	}

	protected String translateSignatureToType(String signature) {
		if ("V".equals(signature)) {
			return "void";
		} else if ("I".equals(signature)) {
			return "int";
		} else if ("Z".equals(signature)) {
			return "boolean";
		} else if ("D".equals(signature)) {
			return "double";
		} else if ("C".equals(signature)) {
			return "char";
		} else if ("B".equals(signature)) {
			return "byte";
		} else if ("F".equals(signature)) {
			return "float";
		} else if ("J".equals(signature)) {
			return "long";
		} else if ("S".equals(signature)) {
			return "short";
		}

		String result= signature;
		result= result.replaceAll(COMMA_PATTERN_STRING, COMMA_PATTERN_REPLACEMENT);

		Matcher genericsMatcher= genericsPattern.matcher(result);
		while (genericsMatcher.find()) {
			result= genericsMatcher.group(1)
					+ "%" + translateSignatureToType(genericsMatcher.group(2))
					+ "&" + genericsMatcher.group(3);
			genericsMatcher= genericsPattern.matcher(result);
		}

		result= result.replaceAll(SIGNATURE_PATTERN_STRING, SIGNATURE_PATTERN_REPLACEMENT);
		result= result.replace('%', '<').replace('&', '>');
		return result;
	}

	protected boolean isPrimitiveTypeSignature(String signature) {
		return signature.length() == 1 && signature.charAt(0) != 'V';
	}

	protected static class ScoreComparator<T extends Comparable<T>> implements Comparator<Object> {

		private final Map<?, T> scoreMap;

		public ScoreComparator(Map<ICompletionProposal, T> proposalScoreMap) {
			this.scoreMap= proposalScoreMap;
		}

		@Override
		public int compare(Object o1, Object o2) {
			T score1= scoreMap.get(o1);
			T score2= scoreMap.get(o2);

			if (score1 == null || score2 == null) {
				return 0;
			} else {
				return score2.compareTo(score1);
			}
		}

	}
}