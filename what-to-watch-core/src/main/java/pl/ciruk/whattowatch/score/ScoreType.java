package pl.ciruk.whattowatch.score;

public enum ScoreType {
    AMATEUR(7, 1000),
    CRITIC(3, 10);
    private final long weight;

    private final int significantQuantityThreshold;

    ScoreType(long weight, int significantQuantityThreshold) {
        this.weight = weight;
        this.significantQuantityThreshold = significantQuantityThreshold;
    }

    public long getWeight() {
        return weight;
    }

    public int getSignificantQuantityThreshold() {
        return significantQuantityThreshold;
    }
}
