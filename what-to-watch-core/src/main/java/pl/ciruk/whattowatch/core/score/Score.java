package pl.ciruk.whattowatch.core.score;

public class Score {

    private final double grade;

    private final long quantity;

    private String source;

    private ScoreType type;

    private Score(double grade, long quantity, String source, ScoreType type) {
        this.grade = grade;
        this.quantity = quantity;
        this.source = source;
        this.type = type;
    }

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

    public double getGrade() {
        return this.grade;
    }

    public long getQuantity() {
        return this.quantity;
    }

    public String getSource() {
        return this.source;
    }

    public ScoreType getType() {
        return this.type;
    }

    public String toString() {
        return "Score(grade=" + this.getGrade() + ", quantity=" + this.getQuantity() + ", source=" + this.getSource() + ", type=" + this.getType() + ")";
    }

    public static class ScoreBuilder {
        private double grade;
        private long quantity;
        private String source;
        private ScoreType type;

        ScoreBuilder() {
        }

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

        public Score build() {
            return new Score(grade, quantity, source, type);
        }

        public String toString() {
            return "Score.ScoreBuilder(grade=" + this.grade + ", quantity=" + this.quantity + ", source=" + this.source + ", type=" + this.type + ")";
        }
    }
}
