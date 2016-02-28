package grah8384;

import spacesettlers.actions.MoveAction;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Movement;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

public class BetterMovement extends MoveAction{
	
	public BetterMovement(
			Toroidal2DPhysics space, 
			Position currentLocation, 
			Position targetLocation){
		super(space, currentLocation, targetLocation);
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
		
		Vector2D goalDistVector = space.findShortestDistanceVector(ship.getPosition(), this.targetLocation);
		Vector2D shipVelVector = ship.getPosition().getTranslationalVelocity();
		Vector2D resultantDir = shipVelVector.negate().add(goalDistVector).unit(); 
		
		double maxAccel = Movement.MAX_TRANSLATIONAL_ACCELERATION;
		//one dimension case
		double shipSpeedToGoal = shipVelVector.dot(goalDistVector.unit());
		double maxAccelToGoal = resultantDir.multiply(maxAccel).dot(goalDistVector.unit());
		double stoppingDistance = 1/2*maxAccelToGoal*(Math.pow(maxAccelToGoal, 2)-Math.pow(shipSpeedToGoal, 2));
		double distanceToGoal = space.findShortestDistance(ship.getPosition(), this.targetLocation);
		
		
		Vector2D goalAccel = null;
		if(distanceToGoal > stoppingDistance){
			if(shipVelVector.getMagnitude() < Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY){
				goalAccel = resultantDir.multiply(maxAccel);
			}else{
				goalAccel = resultantDir.multiply(0);
			}
			
		}else{
			 goalAccel = resultantDir.multiply(-maxAccel);
		}
		
		/*
		// set the angular and translational velocity at the same time
		double angularAccel = pdControlOrientToGoal(space, targetLocation, ship.getPosition(), 0);
		movement.setAngularAccleration(angularAccel);
		Vector2D goalAccel = pdControlMoveToGoal(space, targetLocation, ship.getPosition(), targetVelocity);
		movement.setTranslationalAcceleration(goalAccel);
		*/
		
		movement.setTranslationalAcceleration(goalAccel);

		// figure out if it has reached the goal
		if ((goalAccel.getMagnitude() < 1) ||
				(space.findShortestDistance(targetLocation, ship.getPosition()) < TARGET_REACHED_ERROR)) {
			isFinished = true;
		}

		return movement;
	}
}
