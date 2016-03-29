package grah8384;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
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
import spacesettlers.utilities.Vector2D;
import grah8384.Graph;

public class KnowledgeRepresentation {
	/**
	 * The team that this model is for
	 */
	TeamClient team;
	/**
	 * The space that the team is operating in
	 */
	Toroidal2DPhysics space;
	
	/**
	 * Map of whether or not each ship is aiming for the base
	 */
	HashMap <UUID, Boolean> aimingForBase;
	/**
	 * Maps asteroids to ship so that no two ships of the same team go after the same asteroid
	 */
	protected HashMap <UUID, Ship> asteroidToShipMap;
	/**
	 * The ID of the ship that is the asteroid collector
	 */
	protected UUID asteroidCollectorID;
	/**
	 * A list of graphics that need to be added to the GUI
	 */
	protected ArrayList<SpacewarGraphics> graphicsToAdd;
	
	/**
	 * List of planned out actions
	 * Linked list because we only really need to add to the end and access the head
	 */
	protected LinkedList<Node> plannedPoints;
	
	/**
	 * Counter to re-plan every ten time steps
	 */
	protected int timeSteps = 35;
	
	/**
	 * The weight of the resources in evaluating asteroids
	 */
	final int MONEY_COEFFICIENT = 1;
	/**
	 * The weight of the distance in evaluating asteroids (should be negative)
	 */
	final int DISTANCE_COEFFICIENT = -1;
	
	Graph graph = null;
	
	protected LinkedList<Node> plannedPath;
	
	protected GoalNode goalNode = null;
	
	private double[] weights = {2000,1500000,500,1,1,10000,1,1,1};
	
	private HashSet<AbstractObject> obstructions = null;
	
	FollowPathAction followPathAction;
	HashMap <UUID, Graph> graphByShip = new HashMap<UUID, Graph>();
	
	/**
	 * Creates a new knowledge representation for the <code>team</code> based on the give <code>space</code>
	 * 
	 * @param team
	 * @param space
	 */
	public KnowledgeRepresentation(TeamClient team, Toroidal2DPhysics space)
	{
		this.team = team;
		this.space = space;
		aimingForBase = new HashMap<UUID, Boolean>();
		asteroidToShipMap = new HashMap<UUID, Ship>();
		graphicsToAdd = new ArrayList<SpacewarGraphics>();
		plannedPoints = new LinkedList<Node>();
		asteroidCollectorID = null;
	}
	
	/**
	 * Returns a good action for the specified ship
	 * @param ship
	 * @param space
	 * @return
	 */
	public AbstractAction getAction(Ship ship, Toroidal2DPhysics space, int currentTimesteps) {		
		// the first time we initialize, decide which ship is the asteroid collector
		if (asteroidCollectorID == null) {
			asteroidCollectorID = ship.getId();
		}
		
		AbstractObject goal = getActionObject(space, ship);
		if (goal == null) return new DoNothingAction();
		AbstractAction action = getAStarPathToGoal(space, ship, goal.getPosition());
		graphicsToAdd.add(new StarGraphics(Color.RED, goal.getPosition()));
		
		return action;
	}

	private AbstractAction getAStarPathToGoal(Toroidal2DPhysics space2, Ship ship, Position goalPosition) {
		AbstractAction newAction;
		
		Graph graph = AStarSearch.createGraphToGoalWithBeacons(space, ship, goalPosition, new Random());
		Vertex[] path = graph.findAStarPath(space);
		followPathAction = new FollowPathAction(path);
		//followPathAction.followNewPath(path);
		newAction = followPathAction.followPath(space, ship);
		graphByShip.put(ship.getId(), graph);
		return newAction;
	}

	private AbstractObject getActionObject(Toroidal2DPhysics space, Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();

		// aim for a beacon if there isn't enough energy
		if (ship.getEnergy() < 2000) {
			Beacon beacon = pickNearestBeacon(space, ship);
			aimingForBase.put(ship.getId(), false);
			return beacon;
		}

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 500) {
			Base base = findNearestBase(space, ship);
			aimingForBase.put(ship.getId(), true);
			return base;
		}

		// did we bounce off the base?
		if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
			current = null;
			aimingForBase.put(ship.getId(), false);
		}

		// otherwise aim for the asteroid
		aimingForBase.put(ship.getId(), false);
		Asteroid asteroid = pickHighestValueFreeAsteroid(space, ship);

		/*if (asteroid == null) {
			// there is no asteroid available so collect a beacon
			Beacon beacon = pickNearestBeacon(space, ship);
			// if there is no beacon, then just skip a turn
			if (beacon == null) {
				newAction = new DoNothingAction();
			} else {
				newAction = new MoveToObjectAction(space, currentPosition, beacon);
			}
		} else {
			asteroidToShipMap.put(asteroid.getId(), ship);
			newAction = new MoveToObjectAction(space, currentPosition, asteroid);
		}*/
		if (asteroid != null) {
			asteroidToShipMap.put(asteroid.getId(), ship);
		}
		
		return asteroid;
	
//		if (ship.getEnergy() < weights[0]) {
//			Asteroid asteroid = getBestAsteroid(ship);
//			if (asteroid != null && getCost(asteroid, ship) < weights[1]) {
//				return asteroid;
//			}
//			Base base = getBestBase(ship);
//			Beacon beacon = getBestBeacon(ship);
//			if (beacon != null && base != null && getCost(beacon, ship) < getCost(base, ship)) return beacon;
//			else {
//				aimingForBase.put(ship.getId(), true);
//				return base;
//			}
//		}else{
//			Base base = getBestDropOffBase(ship);
//			if (ship.getResources().getTotal() > weights[2] && base != null && getCostDropOffResources(base, ship) < weights[3]) {
//				aimingForBase.put(ship.getId(), true);
//				return base;
//
//			}else{
//				return getBestAsteroid(ship);
//			}	
//		}
	}
	
	private Beacon getBestBeacon(Ship ship) {
		HashSet<Beacon> Beacons = (HashSet<Beacon>) space.getBeacons();
		Beacon result = null;
		double lowestCost = Double.MAX_VALUE;
		for (Beacon beacon : Beacons) {
			double cost = getCost(beacon, ship);
			if (cost < lowestCost) {
				lowestCost = cost;
				result = beacon;
			}
		}
		return result;
	}

	private Base getBestDropOffBase(Ship ship) {
		HashSet<Base> Bases = (HashSet<Base>) space.getBases();
		Base result = null;
		double lowestCost = Double.MAX_VALUE;
		for (Base base : Bases) {
			if (base.getTeamName() != team.getTeamName()) continue;
			double cost = getCostDropOffResources(base, ship);
			if (cost < lowestCost) {
				lowestCost = cost;
				result = base;
			}
		}
		return result;
	}

	private Base getBestBase(Ship ship) {
		HashSet<Base> Bases = (HashSet<Base>) space.getBases();
		Base result = null;
		double lowestCost = Double.MAX_VALUE;
		for (Base base : Bases) {
			if (base.getTeamName() != team.getTeamName()) continue;
			double cost = getCost(base, ship);
			if (cost < lowestCost) {
				lowestCost = cost;
				result = base;
			}
		}
		return result;
	}

	private Asteroid getBestAsteroid(Ship ship) {
		HashSet<Asteroid> asteroids = (HashSet<Asteroid>) space.getAsteroids();
		Asteroid result = null;
		double lowestCost = Double.MAX_VALUE;
		for (Asteroid asteroid : asteroids) {
			if (!asteroid.isMineable()) continue;
			double cost = getCost(asteroid, ship);
			if (cost < lowestCost) {
				lowestCost = cost;
				result = asteroid;
			}
		}
		return result;
	}

	private double getCostDropOffResources(Base base, Ship ship) {
		return weights [6] * space.findShortestDistance(base.getPosition(), ship.getPosition())
				+ weights[8] * momentumDetriment(base, ship);
	}

	private double getCost(AbstractObject object, Ship ship) {
		HashSet<Asteroid> asteroids = getMineableAsteroidsAround(object);
		int totalResourcesAround = sumResources(asteroids);
		int asteroidsAround = asteroids.size();
		
		if (object instanceof Asteroid) {
			Asteroid asteroid = (Asteroid) object;
			return -1 * weights[4] * totalResourcesAround
					+ weights[5] * asteroidsAround
					+ weights[6] * space.findShortestDistance(ship.getPosition(), asteroid.getPosition())
					//Scaled by 8 by expected value
					- weights[7] * 8 * asteroid.getResources().getMass()
					+ weights[8] * momentumDetriment(object, ship);
		} else {
			double energy = 0;
			if (object instanceof Base) energy = ((Base) object).getEnergy();
			else if (object instanceof Beacon) energy = Beacon.BEACON_ENERGY_BOOST;

			return -1 * weights[4] * totalResourcesAround
					+ weights[5] * asteroidsAround
					+ weights[6] * space.findShortestDistance(ship.getPosition(), object.getPosition())
					- weights[7] * energy
					+ weights[8] * momentumDetriment(object, ship);
		}
	}

	private double momentumDetriment(AbstractObject object, Ship ship) {
		Vector2D displacement = space.findShortestDistanceVector(ship.getPosition(), object.getPosition());
		Vector2D velocity = ship.getPosition().getTranslationalVelocity();
		
		return velocity.getMagnitude() * velocity.angleBetween(displacement);
	}

	private int sumResources(HashSet<Asteroid> asteroids) {
		int result = 0;
		for (Asteroid asteroid : asteroids) {
			result += asteroid.getMass();
		}
		return result;
	}

	private HashSet<Asteroid> getMineableAsteroidsAround(AbstractObject object) {
		int radius = 200;
		Position p = object.getPosition();
		HashSet<Asteroid> asteroids = (HashSet<Asteroid>) space.getAsteroids();
		HashSet<Asteroid> result = new HashSet<Asteroid>();
		for (Asteroid asteroid : asteroids) {
			if (!asteroid.isMineable() || space.findShortestDistance(p, asteroid.getPosition()) > radius) continue;
			result.add(asteroid);
		}
		return result;
	}	
	
	private AbstractAction getNextPlannedAction(Position shipPosition) {
		Node nextNode = plannedPoints.removeFirst();

		if (plannedPoints.isEmpty())
		{
			if (nextNode instanceof GoalNode)
			{
				return new BetterMovement(space, shipPosition, nextNode.position, Ship.SHIP_RADIUS + ((GoalNode)nextNode).getGoalRadius());
			}
			else
			{
				System.err.println("Last node in path was not a goal");
				return new DoNothingAction();
			}
		}
		
		return new BetterMovement(space, shipPosition, nextNode.position);
	}
	
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
				value = MONEY_COEFFICIENT*asteroid.getResources().getTotal() + DISTANCE_COEFFICIENT*space.findShortestDistance(ship.getPosition(), asteroid.getPosition());
				if (value  > bestValue) {
					bestValue = value;
					bestAsteroid = asteroid;
				}
			}
		}
		//System.out.println("Best asteroid has " + bestValue);
		return bestAsteroid;
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

	public void clear() {
		if (plannedPoints != null) plannedPoints.clear();
		if (plannedPath != null) plannedPath.clear();
		goalNode = null;
	}
}
