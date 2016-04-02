package grah8384;

import java.util.ArrayList;
import java.util.Random;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class Clustering {
	Toroidal2DPhysics space;
	ArrayList<Average> avgErrorPerIteration;
	Average iterations;
	
	public Clustering(	Toroidal2DPhysics space) {
		this.space = space;
		avgErrorPerIteration = new ArrayList<Average>();
	}
	
	public void kMeansMineableAsteroids(int k) {
		assert (k >= 1);
		Random rand = new Random();
		
		//Get all mineable asteroids
		ArrayList<Asteroid> asts = new ArrayList<Asteroid>(Methods.getMineableAsteroids(space));
		
		ArrayList<Position> centers = new ArrayList<Position>();
		
		centers.add(asts.get(rand.nextInt(asts.size())).getPosition());
		
		//Add k-1 other centers
		for (int i=1; i<k; i++) {
			Position choice = null;
			double maxDist = Double.MIN_VALUE;
			
			//Choose furthest center from 3 random options
			for (int j=0; j<3; j++) {
				Position cur = asts.get(rand.nextInt(asts.size())).getPosition();
				double dist = sumDistance(cur, centers);
				
				if (dist > maxDist) {
					choice = cur;
					maxDist = dist;
				}
			}
			assert(choice != null);
			
			centers.add(choice);
		}
		
		ArrayList<Cluster> clusters = clustering(asts, centers);
		System.out.println(clusters);
	}
	
	public void printAverages() {
		System.out.println("Averages: " + avgErrorPerIteration.size());
		for (int i = 0; i < avgErrorPerIteration.size(); i++) {
			System.out.println("Iteration " + (i+1) + " :: Count: " +  avgErrorPerIteration.get(i).count + " Avg weighted error: " + avgErrorPerIteration.get(i).average());
		}
	}

	private ArrayList<Cluster> clustering(ArrayList<Asteroid> asts,
			ArrayList<Position> centers) {	
		
		ArrayList<Position> prevCenters = null;
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		
		int count = 0;
		//Loop until the centers move by an average less than 5
		while (prevCenters == null || avgDelta(prevCenters, centers) > 5) {
			count++;

			//Create clusters based on centers
			for (int i = 0; i < centers.size(); i++) {
				clusters.add(new Cluster(centers.get(i)));
			}
			
			//Place asteroid in nearest cluster
			for (int i = 0; i < asts.size(); i ++) {
				double minDist = Double.MAX_VALUE;
				int bestCluster = -1;
				
				//Find closest cluster
				for (int j = 0; j < clusters.size(); j++) {
					double dist = space.findShortestDistance(asts.get(i).getPosition(), clusters.get(j).getCenter());
					if (dist < minDist) {
						minDist = dist;
						bestCluster = j;
					}
				}
				
				assert(bestCluster > -1);
				
				clusters.get(bestCluster).addObject(asts.get(i));
			}
			
			//Create new centers from clusters
			prevCenters = new ArrayList<Position>(centers);
			centers.clear();
			for (int i = 0; i < clusters.size(); i++) {
				centers.add(clusters.get(i).resetCenter());
			}
			
			double error = weightedAverageError(clusters);
			System.out.println("Error: " + weightedAverageError(clusters));
			
			while (count > avgErrorPerIteration.size()) {
				avgErrorPerIteration.add(new Average());
			}
			
			avgErrorPerIteration.get(count-1).add(error);
		}
		
		System.out.println("Iterations: " + count);
		
		return clusters;
	}
	
	private double weightedAverageError(ArrayList<Cluster> clusters) {
		double sum = 0;
		double count = 0;
		for (Cluster cluster : clusters) {
			for (AbstractObject obj : cluster.objects) {
				sum += space.findShortestDistance(obj.getPosition(), cluster.center) * obj.getMass();
				count += obj.getMass();
			}
		}
		return sum/count;
	}

	private double avgDelta(ArrayList<Position> prevCenters, ArrayList<Position> centers) {
		assert (prevCenters.size() == centers.size());
		
		double sum = 0;
		int count = 0;
		for (int i = 0; i < prevCenters.size(); i++){
			if (prevCenters.get(i).equals(centers.get(i))) {
				count++;
			} else {
				double dist = space.findShortestDistance(prevCenters.get(i), centers.get(i));
				if (!Double.isNaN(dist)) {
					sum += dist;
					count ++;
				}
			}
		}
		return sum/count;
	}

	private double sumDistance(Position cur, ArrayList<Position> positions) {
		double sum = 0;
		for (int i = 0; i < positions.size(); i++) {
			sum += space.findShortestDistance(cur, positions.get(i));
		}
		return sum;
		
	}
}
