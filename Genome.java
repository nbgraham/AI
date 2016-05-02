package grah8384;

import java.io.Serializable;
import java.util.Random;

import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class Genome implements Comparable<Genome>{
	
	
	protected static Genome maxFit = null;
	protected double fitness; 
	protected double x, y;
	
	public Genome(double x, double y) {
		this.x = x;
		this.y = y;
		this.fitness = Double.NEGATIVE_INFINITY;
	}
	
	public void mutate(double mutRate, Toroidal2DPhysics space) {
		Random rand = new Random();

		if (rand.nextDouble() < mutRate) {
			double dist = space.findShortestDistance(new Position(x, y), new Position(maxFit.x, maxFit.y));
			x += rand.nextGaussian()*dist;
			y += rand.nextGaussian()*dist;
		}
		toroidalWrap(space);
	}

	@Override
	public int compareTo(Genome o) {
		//-1 so default sort does descending
		return -1 *Double.compare(fitness, o.fitness);
	}
	
	/**
	 * Torridial wrap based on the height/width of the enviroment
	 *  
	 * @param position
	 */
	void toroidalWrap(Toroidal2DPhysics space) {
		int width = space.getWidth();
		int height = space.getHeight();
		while (x < 0) {
			x = x + width;
		}

		while (y < 0) {
			y = y + height;
		}

		x = x % width;
		y = y % height;
	}
	
	
}