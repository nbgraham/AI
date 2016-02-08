package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.clients.TeamClient;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class KnowledgeRepresentation {
	
	/**
	 * The team that this model is for
	 */
	TeamClient team;
	/**
	 * The space that the team is operating in
	 */
	Toroidal2DPhysics space;
	Map<UUID, UUID> asteroidToShip;
	
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
		asteroidToShip = new HashMap<UUID, UUID>();
				
	}
	
	/**
	 * Returns true if the given <code>ship</code> has less than a certain energy
	 * @param ship
	 * @return
	 */
	public boolean isEnergyLow(Ship ship)
	{
		return ship.getEnergy() < 2000;
	}
	
	/**
	 * Returns true if the given <code>ship</code> has more than a certain amount of resources
	 * @param ship
	 * @return
	 */
	public boolean isCargoNearFull(Ship ship)
	{
		return ship.getResources().getTotal() > 500;
	}
	
	/**
	 * Returns the base that is nearest to the given <code>ship</code>
	 * If there is no closest base, returns null
	 * 
	 * @param ship
	 * @return
	 */
	public Base findNearestBase(Ship ship)
	{
		Set<Base> bases = space.getBases();
		Base nearestBase = null;
		
		double minDistance = Double.MAX_VALUE;
		double distanceToBase;
		
		for ( Base base : bases)
		{
			if (!base.isAlive()) continue;
			
			distanceToBase = space.findShortestDistance(ship.getPosition(), base.getPosition());
			
			if ( distanceToBase < minDistance )
			{
				minDistance = distanceToBase;
				nearestBase = base;
			}
					
		}
		
		return nearestBase;
	}
	
	/**
	 * Returns the asteroid that is nearest to the given <code>ship</code>
	 * If there is no closest asteroid, returns null
	 * 
	 * @param ship
	 * @return
	 */
	public Asteroid findBestAsteroid(Ship ship)
	{
		Set<Asteroid> asteroids = space.getAsteroids();
		Asteroid bestAsteroid = null;
		
		double bestScore = Double.MIN_VALUE;
		double distanceToAsteroid;
		int totalValue;
		double score;
		
		for ( Asteroid asteroid : asteroids)
		{
			if(asteroidToShip.containsKey(asteroid.getId()) || !asteroid.isAlive() || !asteroid.isMineable()) continue;
			
			distanceToAsteroid = space.findShortestDistance(ship.getPosition(), asteroid.getPosition());
			totalValue = asteroid.getResources().getTotal();
			
			score = totalValue*100 - distanceToAsteroid;
			if ( distanceToAsteroid > bestScore )
			{
				bestScore = score;
				bestAsteroid = asteroid;
			}
					
		}
		
		return bestAsteroid;
	}
	
	public Asteroid claimBestAsteroid(Ship ship)
	{
		Asteroid asteroid = findBestAsteroid(ship);
		if (asteroid != null) asteroidToShip.put(asteroid.getId(), ship.getId());
		return asteroid;
	}
	
	/**
	 * Returns the beacon that is nearest to the given <code>ship</code>
	 * If there is no closest beacon, returns null
	 * 
	 * @param ship
	 * @return
	 */
	public Beacon findNearestBeacon(Ship ship)
	{
		Set<Beacon> beacons = space.getBeacons();
		Beacon nearestBeacon = null;
		
		double minDistance = Double.MAX_VALUE;
		double distanceToBeacon;
		
		for ( Beacon beacon : beacons)
		{
			if (!beacon.isAlive()) continue;
			
			distanceToBeacon = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			
			if ( distanceToBeacon < minDistance )
			{
				minDistance = distanceToBeacon;
				nearestBeacon = beacon;
			}
					
		}
		
		return nearestBeacon;
	}
	
	/**
	 * Returns a list of UUIDs for enemy ships that a within a certain radius of the given <code>ship</code>
	 * @param ship
	 * @return
	 */
	public Set<UUID> findNearbyEnemiesByID(Ship ship)
	{
		Set<UUID> nearbyEnemiesIDs = new HashSet<UUID>();
		
		double awarenessRadius = 300;
		Set<Ship> ships = space.getShips();
		
		double distanceToShip;
		
		for ( Ship enemyShip : ships)
		{
			if (!enemyShip.getTeamName().equalsIgnoreCase(ship.getTeamName()))
			{
				distanceToShip = space.findShortestDistance(ship.getPosition(), enemyShip.getPosition());
				
				if ( distanceToShip < awarenessRadius )
				{
					nearbyEnemiesIDs.add(enemyShip.getId());
				}
			}
					
		}
		
		return nearbyEnemiesIDs;
	}
	
	/**
	 * Returns the nearest source of energy, or null if there is none
	 * @param ship
	 * @return
	 */
	public AbstractObject findNearestEnergySource(Ship ship)
	{
		Beacon nearestBeacon = findNearestBeacon(ship);
		Base nearestBase = findNearestBase(ship);
		
		if (nearestBeacon == null) return nearestBase;
		return nearestBeacon;
	}
}
