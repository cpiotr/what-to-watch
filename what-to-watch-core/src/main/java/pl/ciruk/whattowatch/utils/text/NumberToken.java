package pl.ciruk.whattowatch.utils.text;

public class NumberToken {
    private final String value;

    NumberToken(String value) {
        this.value = value.replaceAll(",", ".")
                .replaceAll("[\\p{Z}\\p{Zs}\\s]", "");
    }

    public double asNormalizedDouble() {
        if (value.isBlank()) {
            return 0.0;
        }

        if (isPercentage()) {
            return Double.parseDouble(value.substring(0, value.length() - 1)) / 100.0;
        } else if (isFraction()) {
            String[] fractionParts = value.split("/");
            return Double.parseDouble(fractionParts[0]) / Double.parseDouble(fractionParts[1]);
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public long asSimpleLong() {
        try {
            return Long.parseLong(value.replaceAll("\\W+", ""));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private boolean isFraction() {
        String number = "\\d+(\\.\\d+)?";
        return value.matches(number + "/" + number);
    }

    private boolean isPercentage() {
        return value.matches("[0-9]+%");
    }

    public boolean isEmpty() {
        return value.isBlank();
    }
}
