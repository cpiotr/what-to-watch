package pl.ciruk.whattowatch.score;

public enum ScoreType {
    AMATEUR(7),
    CRITIC(3);
    private long weight;

    ScoreType(long weight) {
        this.weight = weight;
    }

    public long getWeight() {
        return weight;
    }
}
