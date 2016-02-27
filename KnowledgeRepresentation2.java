package grah8384;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.StarGraphics;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class KnowledgeRepresentation2 {
	
	/**
	 * The team that this model is for
	 */
	TeamClient team;
	/**
	 * The space that the team is operating in
	 */
	Toroidal2DPhysics space;
	HashMap <UUID, UUID> asteroidToShip;
	HashMap <UUID, Boolean> aimingForBase;
	public HashMap <UUID, Ship> asteroidToShipMap;
	UUID asteroidCollectorID;
	public ArrayList<SpacewarGraphics> graphicsToAdd;
	
	final int MONEY_COEFFICIENT = 10;
	final int DISTANCE_COEFFICIENT = 2;

	
	/**
	 * Creates a new knowledge representation for the <code>team</code> based on the give <code>space</code>
	 * 
	 * @param team
	 * @param space
	 */
	public KnowledgeRepresentation2(TeamClient team, Toroidal2DPhysics space)
	{
		this.team = team;
		this.space = space;
		asteroidToShip = new HashMap<UUID, UUID>();
		aimingForBase = new HashMap<UUID, Boolean>();
		asteroidToShipMap = new HashMap<UUID, Ship>();
		graphicsToAdd = new ArrayList<SpacewarGraphics>();
		asteroidCollectorID = null;
	}
	
	/**
	 * Call asteroid collecting heuristic 
	 * @param ship
	 * @param space
	 * @return
	 */
	public AbstractAction getAction(Ship ship, Toroidal2DPhysics space) {
		// the first time we initialize, decide which ship is the asteroid collector
		if (asteroidCollectorID == null) {
			asteroidCollectorID = ship.getId();
		}
		
		AbstractAction action;
		if (ship.getId().equals(asteroidCollectorID)) {
			// get the asteroids
			action = getAsteroidCollectorAction(space, ship);
			
			//Draw line for debugging
			if (action instanceof MoveToObjectAction)
			{
				Position p = ((MoveToObjectAction) action).getGoalObject().getPosition();
				graphicsToAdd.add(new StarGraphics(3, team.getTeamColor(), p));
				
				LineGraphics line = new LineGraphics(ship.getPosition(), p, 
						space.findShortestDistanceVector(ship.getPosition(), p));
				line.setLineColor(team.getTeamColor());
				graphicsToAdd.add(line);
			}
		}
		else
		{
			//Couldn't make a move on a resource, do nothing
			action = new DoNothingAction();
		}
		
		return action;
	}
	
	/********************************************************************************************
	 * 		Heuristic 
	 ********************************************************************************************/
	
	/**
	 * Gets the action for the asteroid collecting ship
	 * @param space
	 * @param ship
	 * @return
	 */
	public AbstractAction getAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction currentAction = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();

		// aim for a beacon or base 
		// if there isn't enough energy
		if (ship.getEnergy() < 2000) {
			Beacon beacon = pickNearestBeacon(space, ship);
			AbstractAction newAction = null;
			// if there is no beacon, then just skip a turn
			if (beacon == null) {
				newAction = new DoNothingAction();
			} else {			
				Base base = findNearestBase(space, ship);
				double dist = space.findShortestDistance(currentPosition, beacon.getPosition());
				
				//going to our base is a better value
				if(costBenefit(base.getEnergy(), space.findShortestDistance(currentPosition, base.getPosition())) >
					costBenefit(Beacon.BEACON_ENERGY_BOOST, dist)){
					//goto base
					newAction = new MoveToObjectAction(space, currentPosition, base);
				}else{
					//if too far away, it will likely die
					if(!probablyAttainable(space, beacon, ship)){
						//do nothing
						newAction = new DoNothingAction();
					}else{
						//goto beacon
						newAction = new MoveToObjectAction(space, currentPosition, beacon);
					}
				}
			}
			aimingForBase.put(ship.getId(), false);
			return newAction;
		}

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 500) {
			Base base = findNearestBase(space, ship);
			AbstractAction newAction = new MoveToObjectAction(space, currentPosition, base);
			aimingForBase.put(ship.getId(), true);
			return newAction;
		}

		// did we bounce off the base?
		if (ship.getResources().getTotal() == 0 
				&& ship.getEnergy() > 2000 
				&& aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
			currentAction = null;
			aimingForBase.put(ship.getId(), false);
		}

		// otherwise aim for the asteroid
		if (currentAction == null || currentAction.isMovementFinished(space)) {
			aimingForBase.put(ship.getId(), false);
			Asteroid asteroid = pickHighestValueFreeAsteroid(space, ship);

			AbstractAction newAction = null;

			if (asteroid == null) {
				// there is no asteroid available so collect a beacon
				Beacon beacon = pickNearestBeacon(space, ship);
				// if there is no beacon, then just skip a turn
				if (beacon == null) {
					newAction = new DoNothingAction();
				} else {
					//is it likely we will attain the beacon?
					if(probablyAttainable(space, beacon, ship))
						newAction = new MoveToObjectAction(space, currentPosition, beacon);
					else
						newAction = new DoNothingAction();
				}
			} else {
				asteroidToShipMap.put(asteroid.getId(), ship);
				//is it likely we will attain the asteroid?
				if(probablyAttainable(space, asteroid ,ship))
					newAction = new MoveToObjectAction(space, currentPosition, asteroid);
				else
					newAction = new DoNothingAction();
			}
			return newAction;
		} else {
			return ship.getCurrentAction();
		}
	}
	
	
	/********************************************************************************************
	 * 		Knowledge Abstraction 
	 ********************************************************************************************/
	
	/**
	 * Count the number of objects within a radius of self object
	 * @param space
	 * @param self
	 * @param iterable
	 * @param radius
	 * @return
	 */
	public int countNeighborsInRadius(Toroidal2DPhysics space, 
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
	public boolean probablyAttainable(Toroidal2DPhysics space,
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
	public Set<Asteroid> getMineableAsteroids(Toroidal2DPhysics space){
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
	public Set<Asteroid> getNonMineableAsteroids(Toroidal2DPhysics space){
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
	public double costBenefit(double benefit, double cost){
		if(cost==0.0)
			return benefit;
		
		return benefit/cost;
	}
	
	/** 
	 * (Modified value comparison to benefit/cost)
	 * Returns the asteroid of highest value that isn't already being chased by this team
	 * 
	 * @return
	 */
	public Asteroid pickHighestValueFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		double bestValue = Double.MIN_VALUE;
		double value;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {
			if (!asteroidToShipMap.containsKey(asteroid) && asteroid.isMineable()) {
				value = costBenefit(MONEY_COEFFICIENT*asteroid.getResources().getTotal(),
						DISTANCE_COEFFICIENT*space.findShortestDistance(ship.getPosition(), asteroid.getPosition()));
				if (value  > bestValue) {
					bestValue = value;
					bestAsteroid = asteroid;
				}
			}
		}
		System.out.println("Best asteroid has " + bestValue);
		return bestAsteroid;
	}
	
	
	/* Methods below unchanged from original */


	/**
	 * Find the nearest ship on another team and aim for it
	 * @param space
	 * @param ship
	 * @return
	 */
	public Ship pickNearestEnemyShip(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			
			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return nearestShip;
	}

	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	public Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.MAX_VALUE;
		Base nearestBase = null;

		for (Base base : space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
				if (dist < minDistance) {
					minDistance = dist;
					nearestBase = base;
				}
			}
		}
		return nearestBase;
	}

	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	public Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}
}