package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.jface.text.rules.*;

public class NonMatchingRule implements IPredicateRule {

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		return Token.UNDEFINED;
	}

	@Override
	public IToken getSuccessToken() {
		return null;
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		return Token.UNDEFINED;
	}

}
