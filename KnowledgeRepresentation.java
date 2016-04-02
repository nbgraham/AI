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
import grah8384.Graph;

public class KnowledgeRepresentation {
	/**
	 * The team that this model is for
	 */
	TeamClient team;
	/**
	 * The space that the team is operating inc
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
	protected HashMap<UUID, LinkedList<Node>> plannedPoints;
	
	/**
	 * Counter to re-plan every ten time steps
	 */
	protected int timeSteps = 20;
	
	/**
	 * The weight of the resources in evaluating asteroids
	 */
	final int MONEY_COEFFICIENT = 1;
	/**
	 * The weight of the distance in evaluating asteroids (should be negative)
	 */
	final int DISTANCE_COEFFICIENT = -1;
	Clustering c;
				
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
		plannedPoints = new HashMap<UUID, LinkedList<Node>>();
		asteroidCollectorID = null;
		c = new Clustering(space);
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
		
		LinkedList<Node> path = plannedPoints.get(ship.getId());
		
		if (path != null && !path.isEmpty() && path.getLast() != null && path.getLast() instanceof GoalNode && ((GoalNode) path.getLast()).isGone()) {
			//System.out.println("Goal gone, replan");
			if(plannedPoints.get(ship.getId()) != null)  plannedPoints.get(ship.getId()).clear();
			//Replan!
			timeSteps = 35;
		}
		
		if (space.getCurrentTimestep()%50 == 0) {
			if (space.getCurrentTimestep() < 500) {
			c.kMeansMineableAsteroids(7);
			} else {
				c.printAverages();
			}
		}
		
		Graph graph = null;
		
		if (ship.getCurrentAction() == null || ship.getCurrentAction().isMovementFinished(space))
		{
			if (timeSteps >= 10)
			{
				timeSteps = 0;

				AbstractObject goal = getActionObject(space, ship);

				if (goal != null)
				{	
					graph = new Graph(space, ship, goal, 50);
					
					path = graph.getPath();
					plannedPoints.put(ship.getId(), path);
					
					//Draw graph
					if (graph != null)
					{
						for(Node n : graph.graph)
						{
							graphicsToAdd.add(new StarGraphics(2, team.getTeamColor(), n.position));
							/*
							for (Node j : n.neighbors)
							{
								graphicsToAdd.add(new LineGraphics(n.position, j.position, space.findShortestDistanceVector(n.position, j.position)));
							}
							*/
						}
						graphicsToAdd.add(new StarGraphics(3, new Color(255,0,0), graph.goal.position));
						graphicsToAdd.add(new StarGraphics(3, new Color(0,255,0), graph.start.position));
					}	

					if (path != null)
					{						
						Node last = path.getLast();
						
						if (! (last instanceof GoalNode)) System.err.println("Last node is not a goal node");
					
						return getNextPlannedAction(ship);
					}
					else
					{
						//System.out.println("No path found");
					}	
				}
				
			}
			
			if (plannedPoints.get(ship.getId()) == null || plannedPoints.get(ship.getId()).isEmpty()) return new DoNothingAction();
			else return getNextPlannedAction(ship);
		}
		else
		{
			return ship.getCurrentAction();
		}
	}
	
	private enum STATE{
		LOW_ENERGY, FULL_LOAD, SEEK_RESOURCE;
	}
	
	private STATE getState(Toroidal2DPhysics space, Ship ship){
		if (ship.getEnergy() < 2000) {
			return STATE.LOW_ENERGY;
		}else{
			if (ship.getResources().getTotal() > 500) {
				return STATE.FULL_LOAD;
			}else{
				return STATE.SEEK_RESOURCE;
			}	
		}
	}
	
	public AbstractObject getActionObject(Toroidal2DPhysics space, Ship ship){
		AbstractObject newAction = null;
		
		switch(getState(space, ship)){
			case LOW_ENERGY:
				Beacon beacon = pickNearestBeacon(space, ship);
				
				// if there is no beacon, then just skip a turn
				if (beacon == null) {
					newAction = null;
				} else {
					newAction = beacon;
				}
				aimingForBase.put(ship.getId(), false);
				
			break;
			case FULL_LOAD:
				Base base = findNearestBase(space, ship);
				newAction = base;
				aimingForBase.put(ship.getId(), true);
			break;
			case SEEK_RESOURCE:
				if (ship.getResources().getTotal() == 0 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
					aimingForBase.put(ship.getId(), false);
				}
				
				aimingForBase.put(ship.getId(), false);
				Asteroid asteroid = pickHighestValueFreeAsteroid(space, ship);

				if (asteroid == null) {
					// there is no asteroid available so collect a beacon
					beacon = pickNearestBeacon(space, ship);
					// if there is no beacon, then just skip a turn
					if (beacon == null) {
						newAction = null;
					} else {
						newAction = asteroid;
					}
				} else {
					asteroidToShipMap.put(asteroid.getId(), ship);
					newAction = asteroid;
				}
			break;
		}
	
		return newAction;
	}
	
	
	private AbstractAction getNextPlannedAction(Ship ship) {
		Position shipPosition = ship.getPosition();
		Node nextNode = plannedPoints.get(ship.getId()).removeFirst();

		if (plannedPoints.get(ship.getId()).isEmpty())
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
		
		return new BetterMovement(space, shipPosition, nextNode.position, 5);
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

	public void clearPlan(Ship ship) {
		plannedPoints.remove(ship.getId());
	}
}