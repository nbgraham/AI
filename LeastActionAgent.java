package grah8384;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.StarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * Collects nearby asteroids and brings them to the base, picks up beacons as needed for energy.
 * 
 * If there is more than one ship, this version happily collects asteroids with as many ships as it
 * has.  it never shoots (it is a pacifist)
 * 
 * @author amy
 */
public class LeastActionAgent extends TeamClient {
	HashMap <UUID, Ship> asteroidToShipMap;
	HashMap <UUID, Boolean> aimingForBase;
	ArrayList<SpacewarGraphics> graphicsToAdd;
	
	Planning planner = null;
	boolean needToPlan = false;
	int numShips = 0;
	


	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		if (space.getCurrentTimestep() > 0 && space.getCurrentTimestep() % 50 == 0) {
			needToPlan = true;
		}
		
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
		
		HashSet<Ship> ships = new HashSet<Ship>();
		
		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				ships.add(ship);
				
				AbstractAction action;
				action = getLeastAction(space, ship);
				actions.put(ship.getId(), action);
				
			} else {
				// it is a base.  Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		
		if (needToPlan) {
			if (ships.size() != numShips) {
				numShips = ships.size();
				planner = new Planning(ships);
			}
			planner.search(space);
			needToPlan = false;
		}
		
		return actions;
	}
	
	private enum STATE{
		 SEEK_BASE, SEEK_ENERGY, SEEK_RESOURCE;
	}
	
	private STATE getState(Toroidal2DPhysics space, Ship ship){
		if (ship.getEnergy() < 1500) {
			return STATE.SEEK_ENERGY;
		}else{
			if (ship.getResources().getTotal() > 2000) {
				return STATE.SEEK_BASE;
			}else{
				return STATE.SEEK_RESOURCE;
			}	
		}
	}
	
	/**
	 * Gets the action for the asteroid collecting ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getLeastAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();
		
		AbstractObject goal = null;

		if (planner != null) {
			LinkedList<Node> plan = planner.getPath(ship.getId());
			if (plan != null) {
				goal = space.getObjectById(plan.get(1).action.goal.getId());
				
				Position prev = null;
				Position next = null;
				for (Node n : plan) {
					next = n.state.at.get(ship.getId());
					if (prev != null) {
						graphicsToAdd.add(new LineGraphics(
								prev, 
								next, 
								space.findShortestDistanceVector(
										prev,  
										next
								)
						));
					}
					prev = next;
				}
			}
		}
		
		if (goal == null) {
			switch(getState(space,ship)){
				case SEEK_ENERGY:
					goal = findBestBeacon(space, ship);
					aimingForBase.put(ship.getId(), false);
					break;
				
				case SEEK_RESOURCE:
					// did we bounce off the base?
					if (ship.getResources().getTotal() == 0 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
						current = null;
						aimingForBase.put(ship.getId(), false);
					}
	
					// otherwise aim for the asteroid
					if (current == null || current.isMovementFinished(space)) {
						aimingForBase.put(ship.getId(), false);
						goal = findBestAsteroid(space, ship);
						if (goal != null) {
							asteroidToShipMap.put(goal.getId(), ship);
						}
					}
					break;
				
				case SEEK_BASE:
					goal = findBestBase(space, ship);
					aimingForBase.put(ship.getId(), true);
					break;
	
				
				default:
					return ship.getCurrentAction();
			}
		}
		
		if (goal == null){
			return new DoNothingAction();
		} else {
			graphicsToAdd.add(new LineGraphics(
					ship.getPosition(), 
					goal.getPosition(), 
					space.findShortestDistanceVector(
							ship.getPosition(),  
							goal.getPosition()
					)
			));
			return new BetterObjectMovement(space, currentPosition, goal);
		}
	}
	
	
	/**
	 * Gets the action for the asteroid collecting ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();

		
		// aim for a beacon if there isn't enough energy
		if (ship.getEnergy() < 1500) {
			Beacon beacon = findBestBeacon(space, ship);
			AbstractAction newAction = null;
			// if there is no beacon, then just skip a turn
			if (beacon == null) {
				newAction = new DoNothingAction();
			} else {
				newAction = new BetterObjectMovement(space, currentPosition, beacon);
				graphicsToAdd.add(new LineGraphics(
						ship.getPosition(), 
						beacon.getPosition(), 
						space.findShortestDistanceVector(
								ship.getPosition(),  
								beacon.getPosition()
						)
				));
			}
			aimingForBase.put(ship.getId(), false);
			return newAction;
		}

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 1500) {
			Base base = findBestBase(space, ship);
			AbstractAction newAction = new BetterObjectMovement(space, currentPosition, base);
			graphicsToAdd.add(new LineGraphics(
					ship.getPosition(), 
					base.getPosition(), 
					space.findShortestDistanceVector(
							ship.getPosition(),  
							base.getPosition()
					)
			));
			aimingForBase.put(ship.getId(), true);
			return newAction;
		}

		// did we bounce off the base?
		if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
			current = null;
			aimingForBase.put(ship.getId(), false);
		}

		// otherwise aim for the asteroid
		if (current == null || current.isMovementFinished(space)) {
			aimingForBase.put(ship.getId(), false);
			Asteroid asteroid = findBestAsteroid(space, ship);
			//graphicsToAdd.add(new StarGraphics(2, this.getTeamColor(), asteroid.getPosition()));
			graphicsToAdd.add(new LineGraphics(
								ship.getPosition(), 
								asteroid.getPosition(), 
								space.findShortestDistanceVector(
										ship.getPosition(),  
										asteroid.getPosition()
										)
								)
			);
			
		
			AbstractAction newAction = null;

			/*if (asteroid == null) {
				// there is no asteroid available so collect a beacon
				Beacon beacon = pickNearestBeacon(space, ship);
				// if there is no beacon, then just skip a turn
				if (beacon == null) {
					newAction = new DoNothingAction();
				} else {
					newAction = new BetterObjectMovement(space, currentPosition, beacon);
				}
			} else {
				asteroidToShipMap.put(asteroid.getId(), ship);
				newAction = new BetterObjectMovement(space, currentPosition, asteroid);
			}*/
			if (asteroid != null) {
				asteroidToShipMap.put(asteroid.getId(), ship);
				newAction = new BetterObjectMovement(space, currentPosition, asteroid);
			}
			
			return newAction;
		} 
		
		return ship.getCurrentAction();
	}


	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Base findBestBase(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.MAX_VALUE;
		Base nearestBase = null;

		for (Base base : space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				double dist = BetterMovement.getEnergyCost(space, ship, base.getPosition())+.75*space.findShortestDistance(ship.getPosition(), base.getPosition());
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
	private Asteroid pickHighestValueFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		int bestMoney = Integer.MIN_VALUE;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {
			if (!asteroidToShipMap.containsKey(asteroid)) {
				if (asteroid.isMineable() && asteroid.getResources().getTotal() > bestMoney) {
					bestMoney = asteroid.getResources().getTotal();
					bestAsteroid = asteroid;
				}
			}
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}
	
	/**
	 * Returns the asteroid of highest value that isn't already being chased by this team
	 * 
	 * @return
	 */
	private Asteroid findBestAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		double bestCost = Double.MIN_VALUE;
		double curCost;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {
			if (!asteroidToShipMap.containsKey(asteroid)) {
				curCost = ((double) asteroid.getResources().getTotal())/(BetterMovement.getEnergyCost(space, ship, asteroid.getPosition())
						+.75*space.findShortestDistance(ship.getPosition(), asteroid.getPosition()));
				if (asteroid.isMineable() && curCost > bestCost) {
					bestCost = curCost;
					bestAsteroid = asteroid;
				}
			}
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}

	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private Beacon findBestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = BetterMovement.getEnergyCost(space, ship, beacon.getPosition())+.75*space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}
	
	private boolean willCollide(Toroidal2DPhysics space, Ship ship) {
		Position nextShipPos = moveOneTimestep(space, ship.getPosition());
		Position nextObjectPos = null;
		double distance = 0;
		
		for(Asteroid asteroid : space.getAsteroids()){
			if(asteroid.isMineable())
				continue;
			
			nextObjectPos = moveOneTimestep(space, asteroid.getPosition());
			distance = space.findShortestDistance(nextShipPos , nextObjectPos );

			if (distance < (ship.getRadius() + asteroid.getRadius())) 
				return true;
		}
		
		for(Ship s : space.getShips()){
			if(s.getId().equals(ship.getId()))
				continue;
			
			nextObjectPos = moveOneTimestep(space, s.getPosition());
			distance = space.findShortestDistance(nextShipPos , nextObjectPos );

			if (distance < (ship.getRadius() + s.getRadius())) 
				return true;
		}
		
		for(Base base : space.getBases()){
			nextObjectPos = moveOneTimestep(space, base.getPosition());
			distance = space.findShortestDistance(nextShipPos , nextObjectPos );

			if (distance < (ship.getRadius() + base.getRadius())) 
				return true;
		}
		
		return false;
	}
	
	/**
	 * Advances one time step using the set velocities
	 * @param currentPosition
	 * @return
	 */
	private Position moveOneTimestep(Toroidal2DPhysics space, Position position) {
		double timeStep = space.getTimestep();
		double angularVelocity = position.getAngularVelocity();
		double orientation =  position.getOrientation() + (angularVelocity * timeStep);

		// make sure orientation wraps correctly (-pi to pi)
		if (orientation > Math.PI) {
			orientation -= (2 * Math.PI);
		} else if (orientation < -Math.PI) {
			orientation += (2 * Math.PI);
		}

		// new x,y coordinates
		double newX = position.getX() + (position.getTranslationalVelocityX() * timeStep);
		double newY = position.getY() + (position.getTranslationalVelocityY() * timeStep);

		Position newPosition = new Position(newX, newY, orientation);
		newPosition.setAngularVelocity(angularVelocity);
		newPosition.setTranslationalVelocity(position.getTranslationalVelocity());
		toroidalWrap(space,newPosition);
		return newPosition;
	}
	
	/**
	 * Torridial wrap based on the height/width of the enviroment
	 *  
	 * @param position
	 */
	void toroidalWrap(Toroidal2DPhysics space, Position position) {
		int width = space.getWidth();
		int height = space.getHeight();
		while (position.getX() < 0) {
			position.setX(position.getX() + width);
		}

		while (position.getY() < 0) {
			position.setY(position.getY() + height);
		}

		position.setX(position.getX() % width);
		position.setY(position.getY() % height);
	}



	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		ArrayList<Asteroid> finishedAsteroids = new ArrayList<Asteroid>();

		for (UUID asteroidId : asteroidToShipMap.keySet()) {
			Asteroid asteroid = (Asteroid) space.getObjectById(asteroidId);
			if (asteroid == null || !asteroid.isAlive()) {
 				finishedAsteroids.add(asteroid);
				//System.out.println("Removing asteroid from map");
			}
		}

		for (Asteroid asteroid : finishedAsteroids) {
			asteroidToShipMap.remove(asteroid);
		}


	}

	/**
	 * Demonstrates one way to read in knowledge from a file
	 */
	@Override
	public void initialize(Toroidal2DPhysics space) {
		asteroidToShipMap = new HashMap<UUID, Ship>();
		aimingForBase = new HashMap<UUID, Boolean>();
		graphicsToAdd = new ArrayList<SpacewarGraphics>();

		
		/*XStream xstream = new XStream();
		xstream.alias("ExampleKnowledge", ExampleKnowledge.class);

		try { 
			myKnowledge = (ExampleKnowledge) xstream.fromXML(new File(knowledgeFile));
		} catch (XStreamException e) {
			// if you get an error, handle it other than a null pointer because
			// the error will happen the first time you run
			myKnowledge = new ExampleKnowledge();
		}*/
	}

	/**
	 * Demonstrates saving out to the xstream file
	 * You can save out other ways too.  This is a human-readable way to examine
	 * the knowledge you have learned.
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space) {
	/*	XStream xstream = new XStream();
		xstream.alias("ExampleKnowledge", ExampleKnowledge.class);

		try { 
			// if you want to compress the file, change FileOuputStream to a GZIPOutputStream
			xstream.toXML(myKnowledge, new FileOutputStream(new File(knowledgeFile)));
		} catch (XStreamException e) {
			// if you get an error, handle it somehow as it means your knowledge didn't save
			// the error will happen the first time you run
			myKnowledge = new ExampleKnowledge();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			myKnowledge = new ExampleKnowledge();
		}*/
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		graphics.addAll(graphicsToAdd);
		graphicsToAdd.clear();
		return graphics;
	}

	@Override
	/**
	 * If there is enough resourcesAvailable, buy a base.  Place it by finding a ship that is sufficiently
	 * far away from the existing bases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		double BASE_BUYING_DISTANCE = 400;
		boolean bought_base = false;

		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					Set<Base> bases = space.getBases();

					// how far away is this ship to a base of my team?
					boolean buyBase = true;
					for (Base base : bases) {
						if (base.getTeamName().equalsIgnoreCase(getTeamName())) {
							double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
							if (distance < BASE_BUYING_DISTANCE) {
								buyBase = false;
							}
						}
					}
					if (buyBase) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						bought_base = true;
						//System.out.println("Buying a base!!");
						break;
					}
				}
			}		
		} 
		
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					purchases.put(ship.getId(), PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY);
				}
			}		
		}
		
		/*if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_SHIELD, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					purchases.put(ship.getId(), PurchaseTypes.POWERUP_SHIELD);
				}
			}		
		}*/
		
		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable) && bought_base == false) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}

			}

		}


		return purchases;
	}

	/**
	 * The pacifist asteroid collector doesn't use power ups 
	 * @param space
	 * @param actionableObjects
	 * @return
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		for (AbstractActionableObject actionableObject : actionableObjects) {
			if (actionableObject instanceof Ship) {
				Ship ship = (Ship) actionableObject;
				if(ship.getCurrentPowerups().contains(SpaceSettlersPowerupEnum.DOUBLE_MAX_ENERGY) && ship.getEnergy()>1500){
					powerUps.put(ship.getId(), SpaceSettlersPowerupEnum.DOUBLE_MAX_ENERGY);
					System.out.println("Doubled my max energy!");
				}
			}
		}	
		
		/*for (AbstractActionableObject actionableObject : actionableObjects) {
			if (actionableObject instanceof Ship) {
				Ship ship = (Ship) actionableObject;
				if(ship.getCurrentPowerups().contains(SpaceSettlersPowerupEnum.TOGGLE_SHIELD)){
					if(willCollide(space, ship)){
						if(!ship.isShielded())
							powerUps.put(ship.getId(), SpaceSettlersPowerupEnum.TOGGLE_SHIELD);
					}else{
						if(ship.isShielded())
							powerUps.put(ship.getId(), SpaceSettlersPowerupEnum.TOGGLE_SHIELD);
					}
				}
			}
		}*/
		
		
		return powerUps;
	}

}
