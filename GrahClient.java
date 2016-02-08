package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.clients.TeamClient;

/**
 * A team of random agents
 * 
 * The agents pick a random location in space and aim for it.  They shoot somewhat randomly also.
 * @author amy
 *
 */
public class GrahClient extends TeamClient {
	HashSet<SpacewarGraphics> graphics;
	KnowledgeRepresentation model;
	Random random;
 	boolean fired = false;
	
	public static int RANDOM_MOVE_RADIUS = 200;
	public static double SHOOT_PROBABILITY = 0.1;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		graphics = new HashSet<SpacewarGraphics>();
		random = new Random();
		model = new KnowledgeRepresentation(this, space);
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}


	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> nextActions = new HashMap<UUID, AbstractAction>();
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				
				AbstractAction currentAction = ship.getCurrentAction();
				Position currentPosition = ship.getPosition();
				
				AbstractAction nextAction = getNextAction(currentAction, space, ship, currentPosition);
				nextActions.put(ship.getId(), nextAction);		
			} 
			else
			{
				// it is a base and random doesn't do anything to bases
				nextActions.put(actionable.getId(), new DoNothingAction());
			}
			
		}
	
		return nextActions;
	
	}


	private AbstractAction getNextAction(AbstractAction currentAction, Toroidal2DPhysics space,
			Ship ship, Position currentPosition) {
		
		if (model.isEnergyLow(ship))
		{
			AbstractObject nearestEnergySource = model.findNearestEnergySource(ship);
			
			if (nearestEnergySource != null)
			{
				System.out.println("Energy");
				return new MoveAction(space, currentPosition, nearestEnergySource.getPosition());

			}
		}
		
		if (currentAction == null || currentAction.isMovementFinished(space)) {			
			AbstractObject nearestAsteroid = model.claimBestAsteroid(ship);
			if (nearestAsteroid == null)
			{
				AbstractObject nearestBeacon = model.findNearestBeacon(ship);
				if (nearestBeacon != null)
				{
					System.out.println("Beacon");
					return new MoveAction(space, currentPosition, nearestBeacon.getPosition());
				}
				else
				{
					System.out.println("Do nothing");
					return new DoNothingAction();
				}
			}
			else
			{
				System.out.println("Asteroid");
				return new MoveAction(space, currentPosition, nearestAsteroid.getPosition());
			}
		}
		
		//Keep doing what you're doing
		System.out.print(".");
		return currentAction;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> newGraphics = new HashSet<SpacewarGraphics>(graphics);  
		graphics.clear();
		return newGraphics;
	}


	@Override
	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>();

	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				double shootProbability = SHOOT_PROBABILITY;
				shootProbability *= model.findNearbyEnemiesByID(ship).size();
				
				if (random.nextDouble() < shootProbability) {
					AbstractWeapon newBullet = ship.getNewWeapon(SpaceSettlersPowerupEnum.FIRE_MISSILE);
					if (newBullet != null) {
						powerupMap.put(ship.getId(), SpaceSettlersPowerupEnum.FIRE_MISSILE);
						//System.out.println("Firing!");
					}
				}
			}
		}
		return powerupMap;
	}
	
	class ActionObjective extends MoveAction
	{
		private AbstractObject objective;

		public ActionObjective(Toroidal2DPhysics space, Position currentPosition, AbstractObject objective) {
			super(space, currentPosition, objective == null ? currentPosition : objective.getPosition());
			this.objective = objective;
		}

		public void setObjective(AbstractObject objective)
		{
			this.objective = objective;
		}
		
		public AbstractObject getObjective ()
		{
			return objective;
		}
	}


}
