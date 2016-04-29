package grah8384;

import java.io.Serializable;
import java.util.Random;

public class Chromosome implements Comparable<Chromosome>, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8413894446028271805L;
	
	public double[] weights;
	public double fitness = Double.NEGATIVE_INFINITY;
	
	public Chromosome(double[] weights) {
		this.weights = weights;
	}
	
	public void mutate(double mutRate, Chromosome maxfit) {
		Random rand = new Random();

		for (int j = 0; j < weights.length; j++){
			if (rand.nextDouble() < mutRate) {
				
				weights[j] += rand.nextGaussian()*(maxfit.weights[j]-weights[j]);
			}
		}
	}

	@Override
	public int compareTo(Chromosome o) {
		//-1 so default sort does descending
		return -1 *Double.compare(fitness, o.fitness);
	}
	
	
}