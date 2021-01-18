package pl.ciruk.whattowatch.core.score;

public enum ScoreType {
    AMATEUR(7, 200, 1),
    CRITIC(3, 4, 100),
    ;

    private final long weight;
    private final int significantQuantityThreshold;
    private final int scale;

    ScoreType(long weight, int significantQuantityThreshold, int scale) {
        this.weight = weight;
        this.significantQuantityThreshold = significantQuantityThreshold;
        this.scale = scale;
    }

    public long getWeight() {
        return weight;
    }

    public int getSignificantQuantityThreshold() {
        return significantQuantityThreshold;
    }

    public int getScale() {
        return scale;
    }
}
