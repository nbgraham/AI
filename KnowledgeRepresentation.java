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
	
	private double[] weights = {2000,100,500,100,1,500,10,0.5,5};
	
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
	public AbstractAction getAction(Ship ship, Toroidal2DPhysics space) {
		timeSteps++;
		
		// the first time we initialize, decide which ship is the asteroid collector
		if (asteroidCollectorID == null) {
			asteroidCollectorID = ship.getId();
		}
		
//		//Draw graph
//		if (this.graph != null)
//		{
//			for(Node n : this.graph.graph)
//			{
//				graphicsToAdd.add(new StarGraphics(2, team.getTeamColor(), n.position));
//				/*
//				//Draw lines of graph
//				for (Node j : n.neighbors)
//				{
//					graphicsToAdd.add(new LineGraphics(n.position, j.position, space.findShortestDistanceVector(n.position, j.position)));
//				}
//				*/
//			}
//		}
//		
//		//Draw planned path
//		if (graph != null)
//		{
//			graphicsToAdd.add(new StarGraphics(3, new Color(255,0,0), graph.goal.position));
//			graphicsToAdd.add(new StarGraphics(3, new Color(0,255,0), graph.start.position));
//		}
//
//		if(plannedPath != null)
//		{
//			Node p = null;
//			for (Node n : plannedPath)
//			{
//				if (p != null)
//				{
//					graphicsToAdd.add(new LineGraphics(p.position, n.position, space.findShortestDistanceVector(p.position, n.position)));
//				}
//				p = n;
//			}
//		}
		
		//Straight shot
//		if (goalNode != null) {	
//			obstructions.remove(ship);
//			obstructions.remove(goalNode.getGoalObject());
//			
//			if (space.isPathClearOfObstructions(ship.getPosition(), goalNode.getGoalObject().getPosition(), obstructions, Ship.SHIP_RADIUS))
//			{
//				System.out.println("Change to straight shot");
//				
//				Position goalP = goalNode.getGoalObject().getPosition();
//				double rad = goalNode.getGoalRadius();
//				
//				if (plannedPoints != null) plannedPoints.clear();
//				goalNode = null;
//				return new BetterMovement(space, ship.getPosition(), goalP, Ship.SHIP_RADIUS + rad);
//			}
//			obstructions.add(ship);
//			obstructions.add(goalNode.getGoalObject());
//		}
		
//		if (goalNode != null && goalNode.isGone()) {
//			System.out.println("Goal gone, replan");
//			plannedPoints.clear();
//			ship.setCurrentAction(null);
//			return null;
//		}
		
		if (ship.getCurrentAction() == null || ship.getCurrentAction().isMovementFinished(space))
		{
			if (timeSteps >= 35)
			{
				timeSteps = 0;
				AbstractObject goal = getActionObject(space, ship);

				if (goal != null)
				{	
					if (graph != null)
					{
						for (Node n : graph.graph)
						{
							if (n.neighbors != null) n.neighbors.clear();
							n.neighbors = null;
							n.parent = null;
							n =  null;
						}
						graph.graph = null;
						graph.start = null;
						graph.goal = null;
					}
					graph = null;
					
					if (plannedPoints != null) plannedPoints.clear();
					
					this.graph = new Graph(space, ship, goal, 50);
					
					plannedPoints = graph.getPath();

					if (plannedPoints != null)
					{
						plannedPath = new LinkedList<Node>(plannedPoints); 
						
						Node last = plannedPoints.getLast();
						if (last instanceof GoalNode) goalNode = (GoalNode) last;
						else System.err.println("Last node is not a goal node");
					
						return getNextPlannedAction(ship.getPosition());
					}
					else
					{
						System.out.println("No path found");
					}	
				}
				
			}
			
			if (plannedPoints == null || plannedPoints.isEmpty()) return new DoNothingAction();
			else return getNextPlannedAction(ship.getPosition());
		}
		else
		{
			return ship.getCurrentAction();
		}
	}
	
	private AbstractObject getActionObject(Toroidal2DPhysics space, Ship ship){
		if (ship.getEnergy() < weights[0]) {
			Asteroid asteroid = getBestAsteroid(ship);
			if (getCost(asteroid, ship) < weights[1]) {
				return asteroid;
			}
			Base base = getBestBase(ship);
			Beacon beacon = getBestBeacon(ship);
			if (getCost(beacon, ship) < getCost(base, ship)) return beacon;
			else {
				aimingForBase.put(ship.getId(), true);
				return base;
			}
		}else{
			Base base = getBestDropOffBase(ship);
			if (ship.getResources().getTotal() > weights[2] && getCostDropOffResources(base, ship) < weights[3]) {
				aimingForBase.put(ship.getId(), true);
				return base;

			}else{
				return getBestAsteroid(ship);
			}	
		}
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
		ArrayList<Asteroid> asteroids = getMineableAsteroidsAroud(object);
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

	private int sumResources(ArrayList<Asteroid> asteroids) {
		int result = 0;
		for (Asteroid asteroid : asteroids) {
			result += asteroid.getMass();
		}
		return result;
	}

	private ArrayList<Asteroid> getMineableAsteroidsAroud(AbstractObject object) {
		int radius = 200;
		Position p = object.getPosition();
		ArrayList<Asteroid> asteroids = (ArrayList<Asteroid>) space.getAsteroids();
		for (Asteroid asteroid : asteroids) {
			if (!asteroid.isMineable()) asteroids.remove(asteroid);
			if (space.findShortestDistance(p, asteroid.getPosition()) > radius) asteroids.remove(asteroid);
		}
		return asteroids;
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
}
