package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class TapestryScanner extends RuleBasedScanner {

	public TapestryScanner(ColorManager manager) {
		IToken procInstr =
			new Token(
				new TextAttribute(
					manager.getColor(IXMLColorConstants.PROC_INSTR)));

		IRule[] rules = new IRule[2];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", procInstr);
		// Add generic whitespace rule.
		rules[1] = new WhitespaceRule(new TapestryWhitespaceDetector());

		setRules(rules);
	}
}
