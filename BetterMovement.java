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

	/**
	 * Move the ship to the goal by turning and moving to the goal at the  same time.
	 * 
	 */
//	@Override
/*	public Movement getMovement(Toroidal2DPhysics space, Ship ship) {
		// isOrientedToGoal, isAtGoal, isOrientedAtGoal, isOrientingAtGoal
		Movement movement = new Movement();

		if (isFinished) {
			return movement;
		}
		
		Vector2D goalDistVector = space.findShortestDistanceVector(ship.getPosition(), this.targetLocation);
		Vector2D shipVelVector = ship.getPosition().getTranslationalVelocity();
		
		double maxAccel = Movement.MAX_TRANSLATIONAL_ACCELERATION*.7;
		double nextShipSpeed = shipVelVector.getMagnitude() + maxAccel;
		if(nextShipSpeed > Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY){
			nextShipSpeed = Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY;
		}
		Vector2D goalVelVector = goalDistVector.unit().multiply(nextShipSpeed);
		Vector2D resultantDir = shipVelVector.negate().add(goalDistVector).unit(); 
		
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
		
		
		movement.setTranslationalAcceleration(goalAccel);

		// figure out if it has reached the goal
		if ((goalAccel.getMagnitude() < targetReachedAccel) ||
				(space.findShortestDistance(targetLocation, ship.getPosition()) < targetErrorRadius)) {
			isFinished = true;
		}

		return movement;
	}
*/	
	
	private static Vector2D getLeastAction(Toroidal2DPhysics space, Ship ship, Position targetLocation){
		Vector2D goalDistVector = space.findShortestDistanceVector(ship.getPosition(), targetLocation);
		/*Vector2D shipVelVector = ship.getPosition().getTranslationalVelocity();
		
		double shipVelY = shipVelVector.getYValue();
		double shipVelX = shipVelVector.getXValue();
		
		double goalVelY = goalDistVector.unit().getYValue()*Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY;
		double goalVelX = goalDistVector.unit().getXValue()*Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY;
		
		
		double t = space.getTimestep();
		
		double yDiff = (goalVelY - shipVelY); 
		double xDiff = (goalVelX - shipVelX);
		
		double yMag = Math.abs(yDiff);
		double xMag = Math.abs(xDiff);
		
		double maxAccel = Movement.MAX_TRANSLATIONAL_ACCELERATION;
		double maxVelChange = maxAccel * t;
		double nextYAccel = yDiff/t;
		double nextXAccel = xDiff/t;*/
		/*
		if(yMag > xMag){
			if(yMag > maxVelChange){
				nextYAccel = sign(yDiff)*maxAccel;
				nextXAccel = 0;
			}else{
				nextYAccel = yDiff;
				nextXAccel = Math.sqrt(Math.pow(maxAccel,2)-Math.pow(yDiff,2))*sign(xDiff);
			}
		}else if(yMag < xMag){
			if(xMag > maxAccel){
				nextXAccel = sign(xDiff)*maxAccel;
				nextYAccel = 0;
			}else{
				nextXAccel = xDiff;
				nextYAccel = Math.sqrt(Math.pow(maxAccel,2)-Math.pow(xDiff,2))*sign(yDiff);
			}
		}else{
			nextXAccel = xDiff/Math.sqrt(2);
			nextYAccel = yDiff/Math.sqrt(2);
		}
		*/
		
		//Vector2D nextAccel = new Vector2D(ship.getPosition());
		//nextAccel.setX(nextXAccel);
		//nextAccel.setY(nextYAccel);
		
		return move.pdControlMoveToGoal(
				space, 
				targetLocation, 
				ship.getPosition(), 
				goalDistVector.unit().multiply(Toroidal2DPhysics.MAX_TRANSLATIONAL_VELOCITY*Math.sqrt(2)*.6).add(ship.getPosition().getTranslationalVelocity().negate())
		);
		
		// .7 max, 1500 energy, 1000 pile, .25 distance
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
	
	private static double sign(double a){
		if(a>0)
			return 1;
		else if (a<0)
			return -1;
		else
			return 0;
	}
}