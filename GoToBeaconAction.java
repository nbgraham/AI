package grah8384;

import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;

public class GoToBeaconAction extends GoToAction {	
	
	public GoToBeaconAction(Ship ship, Beacon beacon) {
		this.ship = ship;
		this.goal = beacon;
	}
	
	public boolean isApplicable(StateRepresentation state) {
		return super.isApplicable(state) && state.energy.get(ship.getId()) < 3000;
	}
	
	/**
	 * Add effects of this action to the state
	 * @param state
	 */
	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation result = super.effects(state);
		//Remove the beacon from the state 
		result.removeObject(goal);
		return result;
	}
	
	public double getPathCost(StateRepresentation state) {
		return super.getPathCost(state) - 2500;
	}
}
