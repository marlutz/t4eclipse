package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class TapestryWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
