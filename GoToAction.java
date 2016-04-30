package grah8384;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;

public abstract class GoToAction{
	Ship ship;
	AbstractObject goal;
	
	/**
	 * Check that the action is applicable, given the state
	 * @param state
	 * @return
	 */
	public boolean isApplicable(StateRepresentation state) {
		//If ship is not already at goal
		if (state.at(ship.getId(), goal.getPosition())) {
			return false;
		}
		//And ship has enough energy to make it to goal
		if (BetterObjectMovement.getEnergyCost(state.space, ship, goal.getPosition()) > ship.getEnergy()) {
			return false;
		}
		return true;
	}
	
	public StateRepresentation effects(StateRepresentation state) {
		return null;
	}
	
	public int getResources() {
		return 0;
	}
	
	/**
	 * Returns the path cost of taking this action
	 * Energy cost + 75% of distance
	 * @param state
	 * @return
	 */
	public double getPathCost(StateRepresentation state) {
		return BetterObjectMovement.getEnergyCost(state.space, ship, goal.getPosition()) + 0.75*state.space.findShortestDistance(ship.getPosition(), goal.getPosition());
	}
}
