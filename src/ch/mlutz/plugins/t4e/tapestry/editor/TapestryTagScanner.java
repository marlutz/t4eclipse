package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;

public class TapestryTagScanner extends RuleBasedScanner {

	public TapestryTagScanner(ColorManager manager) {
		IToken string =
			new Token(
				new TextAttribute(manager.getColor(IXMLColorConstants.STRING)));

		IToken dqAttr =
				new Token(
					new TextAttribute(manager.getColor(IXMLColorConstants.DQ_ATTR)));

		IToken sqAttr =
				new Token(
					new TextAttribute(manager.getColor(IXMLColorConstants.SQ_ATTR)));

		IToken jwcIdAttr =
				new Token(
					new TextAttribute(manager.getColor(IXMLColorConstants.JWCID_ATTR)));

		IToken ognlExpr =
				new Token(
					new TextAttribute(manager.getColor(IXMLColorConstants.OGNL_EXPR)));

		IToken attributeName =
				new Token(
					new TextAttribute(manager.getColor(IXMLColorConstants.ATTRIBUTE_NAME)));

		// this list is from: http://tapestry.apache.org/tapestry4.1/usersguide/bindings.html
		String[] bindingTypes= new String[] {
				"asset",
				"bean",
				"clientId",
				"component",
				"hivemind",
				"listener",
				"literal",
				"message",
				"meta",
				"ognl",
				"state",
				"translator",
				"validators"};

		IRule[] rules = new IRule[6 + 2 * bindingTypes.length];

		int i= -1;

		// Add rule for jwcid with double quotes
		rules[++i] = new SingleLineRule(" jwcid=\"", "\"", jwcIdAttr, '\\');
		// Add rule for jwcid with single quotes
		rules[++i] = new SingleLineRule(" jwcid='", "'", jwcIdAttr, '\\');

		// Add rules for each of the Tapestry binding types
		for (String bindingType: bindingTypes) {
			// double quote rule
			rules[++i] = new SingleLineRule("\"" + bindingType + ":", "\"", ognlExpr, '\\');
			// single quote rule
			rules[++i] = new SingleLineRule("'" + bindingType + ":", "'", ognlExpr, '\\');
		}

		// Add rule for attribute
		rules[++i] = new SingleLineRule(" ", "=", attributeName, '\\');

		// Add rule for double quotes
		rules[++i] = new SingleLineRule("\"", "\"", dqAttr, '\\');
		// Add a rule for single quotes
		rules[++i] = new SingleLineRule("'", "'", sqAttr, '\\');


		// Add generic whitespace rule.
		rules[++i] = new WhitespaceRule(new TapestryWhitespaceDetector());

		setRules(rules);
	}
}
