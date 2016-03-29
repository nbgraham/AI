package grah8384;

import java.util.Random;

public class Gene implements Comparable<Gene>{
	public double[] weights;
	public double fitness = -1;
	
	public Gene(double[] weights) {
		this.weights = weights;
	}
	
	public void mutate(double mutRate) {
		Random rand = new Random();

		for (int j = 0; j < weights.length; j++){
			if (rand.nextDouble() < mutRate) {
				weights[j] *= (0.5 + rand.nextDouble());
			}
		}
	}

	@Override
	public int compareTo(Gene o) {
		//-1 so default sort does descending
		return -1 *Double.compare(fitness, o.fitness);
	}
	
	
}
