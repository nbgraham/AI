package grah8384;

import spacesettlers.objects.AbstractObject;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class BetterObjectMovement extends BetterMovement {
	
	AbstractObject goalObject;
	Position originalLocation;
	

	public BetterObjectMovement(Toroidal2DPhysics space,
			Position currentLocation, AbstractObject goalObject) {
		super(space, currentLocation, goalObject.getPosition());
		this.goalObject = goalObject;
		this.originalLocation = goalObject.getPosition().deepCopy();
	}
	
	/**
	 * Return the goal object (and remember it is a clone so use its UUID!)
	 * @return
	 */
	public AbstractObject getGoalObject() {
		return goalObject;
	}


	/**
	 * Returns true if the movement finished or the goal object died or moved
	 * 
	 */
	public boolean isMovementFinished(Toroidal2DPhysics space) {
		if (super.isMovementFinished(space)) {
			return true;
		}
		
		AbstractObject newGoalObj = space.getObjectById(goalObject.getId());
		
		if (newGoalObj == null) {
			return true;
		}
		
		if (!newGoalObj.isAlive()) {
			return true;
		} 
		
		if (!newGoalObj.getPosition().equals(originalLocation)) {
			return true;
		}
		
		return false;
	}	

}
