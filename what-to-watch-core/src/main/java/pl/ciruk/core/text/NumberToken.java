package pl.ciruk.core.text;

import com.google.common.base.Strings;

public class NumberToken {
	private String value;

	public NumberToken(String value) {
		this.value = value.replaceAll(",", ".")
				.replaceAll("[\\p{Z}\\p{Zs}\\s]", "");
	}
	
	public double asNormalizedDouble() {
		if (Strings.isNullOrEmpty(value)) {
			return 0.0;
		}
		
		if (isPercentage()) {
			return Double.valueOf(value.substring(0, value.length()-1)) / 100.0;
		} else if (isFraction()) {
			String[] fractionParts = value.split("/");
			return Double.valueOf(fractionParts[0]) / Double.valueOf(fractionParts[1]);
		}

		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	public String asString() {
		return value;
	}

	private boolean isFraction() {
		String number = "\\d+(\\.\\d+)?";
		return value.matches(number+"/"+number);
	}

	private boolean isPercentage() {
		return value.matches("[0-9]+%");
	}
	
	public boolean isEmpty() {
		return Strings.isNullOrEmpty(value);
	}
}
