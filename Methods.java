package grah8384;

import java.util.HashSet;
import java.util.Set;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;


public class Methods {
	
	//private constructor for static class
	private Methods(){
		
	}
	
	/**
	 * Count the number of objects within a radius of self object
	 * @param space
	 * @param self
	 * @param iterable
	 * @param radius
	 * @return
	 */
	public static int countNeighborsInRadius(Toroidal2DPhysics space, 
			AbstractObject self, 
			Set<? extends AbstractObject> iterable,
			double radius) {
		//initialize to 0
		int neighbors = 0;
		
		for (AbstractObject other : iterable) {
			// don't count yourself
			if (other.getId().equals(self.getId())) {
				continue;
			}
			//count those within the radius
			double distance = space.findShortestDistance(self.getPosition(), other.getPosition());
			if (distance < radius) {
				neighbors++;
			}
		}
		
		return neighbors;
	}
	
	/**
	 * Decides whether an object is attainable by type
	 * @param space
	 * @param beacon
	 * @param self
	 * @return
	 */
	public static boolean probablyAttainable(Toroidal2DPhysics space,
			AbstractObject other,
			Ship self){
		//beacons should be close, not have lots of missiles or other ships close by
		//otherwise, we probably wont be able to attain it
		if(other instanceof Beacon){
			double minDist = 600;
			return space.findShortestDistance(other.getPosition(), self.getPosition()) < minDist &&
					countNeighborsInRadius(space, other, space.getShips(), 100.0) < 2 &&
					countNeighborsInRadius(space, other, space.getWeapons(), 300.0) <= 10;
		//asteroids shouldn't have too many ships near by
		}else if(other instanceof Asteroid){
			return countNeighborsInRadius(space, other, space.getShips(), 150.0) <= 2;
		}
		
		return false;
	}
	
	/**
	 * Returns a set of mineable (resource-filled) asteroids
	 * @param space
	 * @return
	 */
	public static Set<Asteroid> getMineableAsteroids(Toroidal2DPhysics space){
		Set<Asteroid> minableAsteroids = new HashSet<Asteroid>();
		for(Asteroid asteroid :  space.getAsteroids()){
			if(asteroid.isMineable())
				minableAsteroids.add(asteroid);
		}
		return minableAsteroids;
	}
	
	/**
	 * Returns a set of non mineable asteroids
	 * @param space
	 * @return
	 */
	public static Set<Asteroid> getNonMineableAsteroids(Toroidal2DPhysics space){
		Set<Asteroid> minableAsteroids = new HashSet<Asteroid>();
		for(Asteroid asteroid :  space.getAsteroids()){
			if(!asteroid.isMineable())
				minableAsteroids.add(asteroid);
		}
		return minableAsteroids;
	}

	
	/**
	 * Returns the benefit/cost ratio of two values
	 * @param cost
	 * @param benefit
	 * @return
	 */
	public static double costBenefit(double benefit, double cost){
		if(cost==0.0)
			return benefit;
		
		return benefit/cost;
	}
	

}