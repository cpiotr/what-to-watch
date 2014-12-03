package pl.ciruk.core.text;

import static java.lang.Character.isDigit;
import static java.lang.Character.isSpaceChar;

public class NumberTokenizer {
	private static final String VALID_MID_CHARS = ",./";

	private static final String VALID_SUFFIXES = "%";

	private String input;
	
	private int index = 0;
	
	public NumberTokenizer(String input) {
		this.input = input;
	}
	
	public boolean hasMoreTokens() {
		while (index < input.length() && !isDigit(current())) {
			index++;
		}
		
		return index < input.length();
	}
	
	public NumberToken nextToken() {
		StringBuilder buffer = new StringBuilder();
		while (index < input.length() 
				&& (isDigit(current()) 
						|| (isPreviousADigit() && isCurrentAValidSeparator() && isNextADigit())
						|| (isPreviousADigit() && isSpaceChar(current()) && isNextADigit())
						|| (isPreviousADigit() && isCurrentAValidSuffix()) )
		) {
			buffer.append(current());
			index++;
		}
		return new NumberToken(buffer.toString());
	}

	private boolean isCurrentAValidSuffix() {
		return VALID_SUFFIXES.indexOf(current()) > -1;
	}

	private boolean isNextADigit() {
		return hasNext() && isDigit(next());
	}

	private char next() {
		return input.charAt(index+1);
	}

	private boolean hasNext() {
		return index+1 < input.length();
	}

	private boolean isCurrentAValidSeparator() {
		return VALID_MID_CHARS.indexOf(current()) > -1;
	}

	private char current() {
		return input.charAt(index);
	}
	
	private boolean isPreviousADigit() {
		return hasPrevious() && isDigit(previous());
	}

	boolean hasPrevious() {
		return index > 0;
	}
	
	char previous() {
		return input.charAt(index - 1);
	}
}