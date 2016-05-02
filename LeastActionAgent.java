package grah8384;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
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
	HashMap<UUID, LinkedList<Node>> plans;
	
    ArrayList<SpacewarGraphics> baseEvolve;
    Set<Base> teamBases;
    boolean once = true;


	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		if (space.getCurrentTimestep() % 50 == 0) {
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
		AbstractAction currentAction = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();
		UUID shipID = ship.getId();
		AbstractObject goal = null;
		
		if (planner != null) {
			LinkedList<Node> plan = plans.get(shipID);
			LinkedList<Node> pastPlan = null;
			if (planner.getPath(shipID) != null) {
				pastPlan = new LinkedList<Node>(planner.getPath(shipID));
			}
			if (plan == null) plan = plans.put(shipID, pastPlan);
				if (plan != null && plan.size() > 0) {
					Node currentNode = plan.peek();
					AbstractObject currentGoal;
					
					if (currentNode.action == null) {
						currentGoal = null;
					} else {
						currentGoal = space.getObjectById(currentNode.action.goal.getId());
					}
					
					while (currentNode.action == null || currentGoal == null || !currentGoal.isAlive()) {
						plan.pop();
						currentNode = plan.peek();
						currentGoal = space.getObjectById(currentNode.action.goal.getId());
					}
					
					if (ship.getResources().getTotal() == 0 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
						goal = null;
						aimingForBase.put(ship.getId(), false);
						plan.pop();
						return new DoNothingAction();
					} else {
						goal = space.getObjectById(plan.peek().action.goal.getId());
					}
							
					//Draw plan
					Position prev = null;
					Position next = null;
					for (Node n : planner.getPath(shipID)) {
						next = n.state.at.get(shipID);
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
						currentAction = null;
						aimingForBase.put(ship.getId(), false);
					}
	
					// otherwise aim for the asteroid
					if (currentAction == null || currentAction.isMovementFinished(space)) {
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
			aimingForBase.put(ship.getId(), goal instanceof Base);
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
		
	    Set<Base> teamBases = new HashSet<Base>();
        for(Base b : space.getBases()){
            if(b.getTeamName().equalsIgnoreCase(this.getTeamName())){
                teamBases.add(b);
                //System.out.println("Added a base to the set.");
            }
        }
       
        if(!this.teamBases.equals(teamBases)){
            once = true;
            this.teamBases = teamBases;
        }
       
        if(once){
           
            if(this.teamBases.size()>0){
                baseEvolve = (new EvolutionModule(space, this.teamBases)).getGraphics();
                once = false;
            }
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
		teamBases = new HashSet<Base>();
		plans = new HashMap<UUID, LinkedList<Node>>();
	}

	/**
	 * Demonstrates saving out to the xstream file
	 * You can save out other ways too.  This is a human-readable way to examine
	 * the knowledge you have learned.
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space) {

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
		
		boolean bought_base = false;
		 
        if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
            Position target = new Position(Genome.maxFit.x, Genome.maxFit.y);
            for (AbstractActionableObject actionableObject : actionableObjects) {
                if (actionableObject instanceof Ship) {
                    Ship ship = (Ship) actionableObject;
                    
                    boolean buyBase = false;
                    if(space.findShortestDistance(ship.getPosition(), target) < 200)
                        buyBase = true;
                   
                    if (buyBase) {
                        purchases.put(ship.getId(), PurchaseTypes.BASE);
                        bought_base = true;
                        once = true;
                        if(baseEvolve != null) baseEvolve.clear();
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
