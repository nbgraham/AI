package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import spacesettlers.clients.Team;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class StateRepresentation {
	HashSet<Ship> ships;
	HashSet<Beacon> beacons;
	HashSet<Asteroid> asteroids;
	HashSet<Base> bases;
	
	HashMap<UUID, Position>  at;
	HashMap<UUID, ResourcePile> resources;
	HashMap<UUID, Double> energy;
	
	Toroidal2DPhysics space;
	
	/**
	 * Constructor to create initial state from space object
	 * @param space
	 * @param team
	 */
	public StateRepresentation(Toroidal2DPhysics space, Team team) {
		this.space = space.deepClone();
		
		for (Asteroid a : space.getAsteroids()) {
			if (!a.isMineable()) continue;
			asteroids.add(a);
			at.put(a.getId(), a.getPosition());
		}
		for (Beacon b : space.getBeacons()) {
			beacons.add(b);
			at.put(b.getId(), b.getPosition());
		}
		for (Ship s : team.getShips()) {
			ships.add(s);
			at.put(s.getId(), s.getPosition());
			resources.put(s.getId(), s.getResources());
			energy.put(s.getId(), s.getEnergy());
		}
		for (Base b : space.getBases()) {
			if (b.getTeamName() == team.getTeamName()) {
				bases.add(b);
			}
		}
	}
	
	/**
	 * Checks if the UUID is a ship by comparing to the set of ships in the state
	 * @param object
	 * @return true if the UUID is in the ship list
	 * 			false if the UUID is not in the ship list
	 */
	public boolean isShip(UUID object) {
		return ships.contains(object);
	}
	
	/**
	 * Checks if the UUID is a beacon by comparing to the set of beacons in the state
	 * @param object
	 * @return true if the UUID is in the beacon list
	 * 			false if the UUID is not in the beacon list
	 */
	public boolean isBeacon(UUID object) {
		return beacons.contains(object);
	}
	
	/**
	 * Checks if the UUID is a asteroid by comparing to the set of asteroids in the state
	 * @param object
	 * @return true if the UUID is in the asteroid list
	 * 			false if the UUID is not in the asteroid list
	 */
	public boolean isAsteroids(UUID object) {
		return asteroids.contains(object);
	}
	
	/**
	 * Checks if the given object has the given locations stored in the state
	 * @param object
	 * @param pos
	 * @return
	 */
	public boolean at(UUID object, Position pos) {
		return at.containsKey(object) && pos.equalsLocationOnly(at.get(object));
	}
	
	/**
	 * Changes the position of the given object in the state representation
	 * @param object
	 * @param pos
	 */
	public void setAt(UUID object, Position pos) {
		at.put(object, pos);
	}
	
	public void addResources(UUID ship, ResourcePile resource) {
		resources.get(ship).add(resource);
	}
	
	public void addEnergy(UUID ship, Double energy) {
		this.energy.put(ship, this.energy.get(ship) + energy);
	}
	
	/**
	 * Removes the given object from the list (asteroid, ship, or beacon) that it is currently in
	 * @param object
	 * @return true if the object was in some state list
	 * 			false if the object was not in a state list
	 */
	public boolean removeObject(AbstractObject object) {
		return asteroids.remove(object) || beacons.remove(object) || ships.remove(object);
	}
}
