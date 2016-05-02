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
		StateRepresentation r = new StateRepresentation(state);
		//Set ship location to where the beacon is
		r.setAt(ship.getId(), goal.getPosition());
		//Add 2500 (energy in beacon) to ship
		r.addEnergy(ship.getId(), -1*getPathCost(state));
		//Remove the beacon from the state 
		r.removeObject(goal);
		return r;
	}
	
	public double getPathCost(StateRepresentation state) {
		return super.getPathCost(state) - 2500;
	}
}
