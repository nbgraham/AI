package grah8384;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.utilities.Position;

public abstract class GoToAction{
	Ship ship;
	AbstractObject goal;
	
	/**
	 * Check that the action is applicable, given the state
	 * @param state
	 * @return
	 */
	public boolean isApplicable(StateRepresentation state) {
		if (!state.at.containsKey(ship.getId()) || !state.at.containsKey(goal.getId())) {
			return false;
		}
		
		//If ship is not already at goal
		if (state.at(ship.getId(), goal.getPosition())) {
			return false;
		}
		
		//And ship has enough energy to make it to goal
//		if (BetterObjectMovement.getEnergyCostNC(state.space, state.at.get(ship.getId()), ship.getMass(), state.at.get(goal.getId())) > state.energy.get(ship.getId())) {
//			return false;
//		} 
		
		return true;
	}
	
	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation result = new StateRepresentation(state);
		Position p = state.at.get(goal.getId()).deepCopy();
		p.setTranslationalVelocity(BetterMovement.getGoalVelocity(result.space, result.at.get(ship.getId()), result.at.get(goal.getId())));
		result.setAt(ship.getId(), p);
		result.addEnergy(ship.getId(), -1*getEnergyCost(state));
		return result;
	}
	
	public double getEnergyCost(StateRepresentation state) {
		return BetterObjectMovement.getEnergyCostNC(state.space, state.at.get(ship.getId()), ship.getMass(), state.at.get(goal.getId()));

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
		return getEnergyCost(state);
//		return BetterObjectMovement.getEnergyCostNC(state.space, state.at.get(ship.getId()), ship.getMass(), state.at.get(goal.getId())) 
//				+ 0.75*state.space.findShortestDistance(state.at.get(ship.getId()), state.at.get(goal.getId()))
//				+ 0.25 * getDistanceToNearestBaseFromGoal(state);
	}
	
	private double getDistanceToNearestBaseFromGoal(StateRepresentation state) {
		double min = Double.MAX_VALUE;
		for (Base b: state.bases) {
			double dist = state.space.findShortestDistance(state.at.get(goal.getId()), state.at.get(b.getId()));
			if (dist < min) min = dist;
		}
		if (min == Double.MAX_VALUE) return 0;
		return min;
	}
}
