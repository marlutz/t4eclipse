package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.jface.text.rules.*;

public class TapestryPartitionScanner extends RuleBasedPartitionScanner {
	public final static String XML_COMMENT = "__xml_comment";
	public final static String XML_TAG = "__xml_tag";
	public final static String XML_PI = "__xml_pi";
	public final static String XML_START_TAG = "__xml_starttag";
	public final static String XML_END_TAG = "__xml_endtag";
	public final static String XML_DOCTYPE = "__xml_d";
	public final static String XML_TEXT = "__xml_text";
	public final static String XML_CDATA = "__xml_cdata";

	/*
	public XMLPartitionScanner()
	{

		IToken xmlComment = new Token(XML_COMMENT);
		IToken xmlPI = new Token(XML_PI);
		IToken startTag = new Token(XML_START_TAG);
		IToken endTag = new Token(XML_END_TAG);
		IToken docType = new Token(XML_DOCTYPE);
		IToken text = new Token(XML_TEXT);

		IPredicateRule[] rules = new IPredicateRule[6];

		// rules[0] = new NonMatchingRule();
		rules[0] = new MultiLineRule("<!--", "-->", xmlComment);
		rules[1] = new MultiLineRule("<?", "?>", xmlPI);
		rules[2] = new MultiLineRule("</", ">", endTag);
		rules[3] = new StartTagRule(startTag);
		rules[4] = new MultiLineRule("<!DOCTYPE", ">", docType);
		rules[5] = new XMLTextPredicateRule(text);

		setPredicateRules(rules);
	}
	*/

	public TapestryPartitionScanner() {

		IToken xmlComment = new Token(XML_COMMENT);
		IToken tag = new Token(XML_TAG);

		IPredicateRule[] rules = new IPredicateRule[2];

		rules[0] = new MultiLineRule("<!--", "-->", xmlComment);
		rules[1] = new TagRule(tag);

		setPredicateRules(rules);
	}
}
