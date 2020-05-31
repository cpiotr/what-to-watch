package pl.ciruk.whattowatch.core.score;

public record Score(
        double grade,
        long quantity,
        String source,
        ScoreType type,
        String url) {

    public static Score amateur(double grade) {
        return amateur(grade, ScoreType.AMATEUR.getWeight());
    }

    public static Score amateur(double grade, long quantity) {
        return Score.builder()
                .grade(grade)
                .quantity(quantity)
                .type(ScoreType.AMATEUR)
                .build();
    }

    public static Score critic(double grade) {
        return critic(grade, ScoreType.CRITIC.getWeight());
    }

    public static Score critic(double grade, long quantity) {
        return Score.builder()
                .grade(grade)
                .quantity(quantity)
                .type(ScoreType.CRITIC)
                .build();
    }

    public static ScoreBuilder builder() {
        return new ScoreBuilder();
    }

    public boolean isSignificant() {
        return quantity >= type.getSignificantQuantityThreshold();
    }

    public static class ScoreBuilder {
        private double grade;
        private long quantity;
        private String source;
        private ScoreType type;
        private String url;

        public Score.ScoreBuilder grade(double grade) {
            this.grade = grade;
            return this;
        }

        public Score.ScoreBuilder quantity(long quantity) {
            this.quantity = quantity;
            return this;
        }

        public Score.ScoreBuilder source(String source) {
            this.source = source;
            return this;
        }

        public Score.ScoreBuilder type(ScoreType type) {
            this.type = type;
            return this;
        }

        public Score.ScoreBuilder url(String url) {
            this.url = url;
            return this;
        }

        public Score build() {
            return new Score(grade, quantity, source, type, url);
        }
    }
}
