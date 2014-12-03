package pl.ciruk.films.whattowatch.score;

public class Score {

	private double score;
	private int quantity;

	public Score(double nextDouble, int nextInt) {
		this.score = nextDouble;
		// TODO Auto-generated constructor stub
		this.quantity = nextInt;
	}

	public double getScore() {
		return score;
	}
	
	public int getQuantity() {
		return quantity;
	}
}
