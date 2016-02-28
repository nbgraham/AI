package grah8384;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
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
	protected int timeSteps = 10;
	
	/**
	 * The weight of the resources in evaluating asteroids
	 */
	final int MONEY_COEFFICIENT = 10;
	/**
	 * The weight of the distance in evaluating asteroids (should be negative)
	 */
	final int DISTANCE_COEFFICIENT = -2;

	
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
		
		//Draw planned path
		Node p = null;
		
		if(plannedPoints != null)
		{
			for (Node n : plannedPoints)
			{
				if (p != null)
				{
					graphicsToAdd.add(new LineGraphics(p.position, n.position, space.findShortestDistanceVector(p.position, n.position)));
				}
				p = n;
			}
		}
		
		if (ship.getCurrentAction() == null || ship.getCurrentAction().isMovementFinished(space))
		{
			if (timeSteps >= 10)
			{
				timeSteps = 0;
				AbstractAction action;
				// get the asteroids
				action = getAsteroidCollectorAction(space, ship);
				if (action instanceof BetterObjectMovement)
				{		
					AbstractObject goal = ((BetterObjectMovement) action).getGoalObject();
					Graph g = new Graph(space, ship, goal, 100);
					System.out.println("Generated graph");
					
					/*
					//Draw graph
					for(Node n : g.graph)
					{
						graphicsToAdd.add(new StarGraphics(2, team.getTeamColor(), n.position));
						for (Node j : n.neighbors)
						{
							graphicsToAdd.add(new LineGraphics(n.position, j.position, space.findShortestDistanceVector(n.position, j.position)));
						}
					}
					System.out.println("Current postion: X: " + ship.getPosition().getX() + " Y: " + ship.getPosition().getY());
					System.out.println("Goal postion: X: " + goal.getPosition().getX() + " Y: " + goal.getPosition().getY());
					*/
					
					graphicsToAdd.add(new StarGraphics(3, new Color(255,0,0), g.goal.position));
					graphicsToAdd.add(new StarGraphics(3, new Color(0,255,0), g.start.position));
					
					plannedPoints = g.getPath();
					
					if (plannedPoints != null)
					{
						return new BetterMovement(space, ship.getPosition(), plannedPoints.removeFirst().position);
					}
					else
					{
						System.out.println("No path found");
					}	
				}
				
				return action;
			}
			
			if (plannedPoints.isEmpty()) return new DoNothingAction();
			
			return new BetterMovement(space, ship.getPosition(), plannedPoints.removeFirst().position);
		}
		else
		{
			return ship.getCurrentAction();
		}
	}
	
	
	/**
	 * Gets the action for the asteroid collecting ship
	 * @param space
	 * @param ship
	 * @return
	 */
	public AbstractAction getAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction currentAction = ship.getCurrentAction();

		// aim for a beacon if there isn't enough energy
		if (ship.getEnergy() < 2000) {
			Beacon beacon = pickNearestBeacon(space, ship);
			AbstractAction newAction = null;
			// if there is no beacon, then just skip a turn
			if (beacon == null) {
				newAction = new DoNothingAction();
			} else {
				newAction = new BetterObjectMovement(space, ship.getPosition(), beacon);
			}
			aimingForBase.put(ship.getId(), false);
			return newAction;
		}

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 500) {
			Base base = findNearestBase(space, ship);
			AbstractAction newAction = new BetterObjectMovement(space, ship.getPosition(), base);
			aimingForBase.put(ship.getId(), true);
			return newAction;
		}

		// did we bounce off the base?
		if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
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
					newAction = fastAction(ship,asteroid, 1);
				}
			} else {
				asteroidToShipMap.put(asteroid.getId(), ship);
				newAction = new BetterObjectMovement(space, ship.getPosition(), asteroid);
			}
			return newAction;
		} else {
			return ship.getCurrentAction();
		}
	}


	public AbstractAction fastAction(Ship ship, AbstractObject goal, double percent)
	{
		return new MoveAction(space, ship.getPosition(), goal.getPosition(), space.findShortestDistanceVector(ship.getPosition(), goal.getPosition()).getUnitVector().multiply(Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY).multiply(percent));
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
