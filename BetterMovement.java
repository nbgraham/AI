package grah8384;

import java.util.HashMap;
import java.util.UUID;

import spacesettlers.actions.MoveAction;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Movement;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

public class BetterMovement extends MoveAction{
	double targetReachedAccel =  MoveAction.TARGET_REACHED_ERROR;
	double targetErrorRadius;
	
	static HashMap <UUID, Position> lastVelocity = new HashMap<UUID, Position>();
	static MoveAction move = new MoveAction();

	public BetterMovement(
			Toroidal2DPhysics space, 
			Position currentLocation, 
			Position targetLocation){
		super(space, currentLocation, targetLocation);
		targetErrorRadius = MoveAction.TARGET_REACHED_ERROR;
	}
	
	public BetterMovement(
			Toroidal2DPhysics space, 
			Position currentLocation, 
			Position targetLocation,
			double targetErrorRadius){
		super(space, currentLocation, targetLocation);
		this.targetErrorRadius = targetErrorRadius;
		
		
	}
	
	/**
	 * Move the ship to the goal by turning and moving to the goal at the  same time.
	 * 
	 */
	@Override
	public Movement getMovement(Toroidal2DPhysics space, Ship ship) {
		// isOrientedToGoal, isAtGoal, isOrientedAtGoal, isOrientingAtGoal
		Movement movement = new Movement();
		
		

		if (isFinished) {
			return movement;
		}
		
		Vector2D goalAccel = getLeastAction(space, ship, targetLocation);
		movement.setTranslationalAcceleration(goalAccel);

		// figure out if it has reached the goal
		if ((goalAccel.getMagnitude() < targetReachedAccel) ||
				(space.findShortestDistance(targetLocation, ship.getPosition()) < targetErrorRadius)) {
			isFinished = true;
		}
		
		lastVelocity.put(ship.getId(), ship.getPosition());
		
		if(lastVelocity.containsKey(ship.getId())){
			Position last = lastVelocity.get(ship.getId());
			double shipVelX = ship.getPosition().getTranslationalVelocityX();
			double shipVelY = ship.getPosition().getTranslationalVelocityY();
			double lastVelX = last.getTranslationalVelocityX();
			double lastVelY = last.getTranslationalVelocityY();
			
			double delta = Math.sqrt(Math.pow(shipVelX-lastVelX, 2) + Math.pow(shipVelY-lastVelY, 2));
			double tolerance = .4;
			
			if(delta > tolerance*Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY*Math.sqrt(2)){
				isFinished = true;
			}
		}
		
		return movement;
	}

	
	private static Vector2D getLeastAction(Toroidal2DPhysics space, Position shipPosition, Position targetLocation){
		Vector2D goalDistVector = space.findShortestDistanceVector(shipPosition, targetLocation);
		
		return move.pdControlMoveToGoal(
				space, 
				targetLocation, 
				shipPosition, 
				goalDistVector.unit().multiply(Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY*Math.sqrt(2)*.6).add(shipPosition.getTranslationalVelocity().negate())
		);
	}
	
	private static Vector2D getLeastAction(Toroidal2DPhysics space, Ship ship, Position targetLocation){
		Vector2D goalDistVector = space.findShortestDistanceVector(ship.getPosition(), targetLocation);
		
		return move.pdControlMoveToGoal(
				space, 
				targetLocation, 
				ship.getPosition(), 
				goalDistVector.unit().multiply(Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY*Math.sqrt(2)*.6).add(ship.getPosition().getTranslationalVelocity().negate())
		);
		
		// .7 max, 1500 energy, 1000 pile, .25 distance
	}
	
	public static Vector2D getGoalVelocity(Toroidal2DPhysics space, Position shipPosition, Position targetLocation) {
		Vector2D goalDistVector = space.findShortestDistanceVector(shipPosition, targetLocation);
		return goalDistVector.unit().multiply(Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY*Math.sqrt(2)*.6).add(shipPosition.getTranslationalVelocity().negate());
	}
	
	public static int getEnergyCostNC(Toroidal2DPhysics space, Position shipPosition, int shipMass, Position targetLocation){
		int penalty = 0;
		
		while(space.findShortestDistance(shipPosition, targetLocation) > Ship.SHIP_RADIUS){
			Vector2D nextAccel = getLeastAction(space, shipPosition, targetLocation);
			double linearAccel = nextAccel.getMagnitude();
			double linearInertia = shipMass * linearAccel;
			penalty += (int) Math.floor(Toroidal2DPhysics.ENERGY_PENALTY * linearInertia);
			double timeStep = space.getTimestep();
			
			Position nextPos = shipPosition;
			nextPos.setTranslationalVelocity(nextPos.getTranslationalVelocity().add(nextAccel.multiply(timeStep)));
			nextPos.setX(nextPos.getX()+nextPos.getTranslationalVelocityX()*timeStep);
			nextPos.setY(nextPos.getY()+nextPos.getTranslationalVelocityY()*timeStep);
			
		}
		
		return penalty;
	}
	
	public static int getEnergyCost(Toroidal2DPhysics space, Ship ship, Position targetLocation){
		
		Ship testShip = ship.deepClone();
		int penalty = 0;
		
		
		while(space.findShortestDistance(testShip.getPosition(), targetLocation) > Ship.SHIP_RADIUS){
			Vector2D nextAccel = getLeastAction(space, testShip, targetLocation);
			double linearAccel = nextAccel.getMagnitude();
			double linearInertia = ship.getMass() * linearAccel;
			penalty += (int) Math.floor(Toroidal2DPhysics.ENERGY_PENALTY * linearInertia);
			double timeStep = space.getTimestep();
			
			Position nextPos = testShip.getPosition();
			nextPos.setTranslationalVelocity(nextPos.getTranslationalVelocity().add(nextAccel.multiply(timeStep)));
			nextPos.setX(nextPos.getX()+nextPos.getTranslationalVelocityX()*timeStep);
			nextPos.setY(nextPos.getY()+nextPos.getTranslationalVelocityY()*timeStep);
			
		}
		
		
		return penalty;
	}
}