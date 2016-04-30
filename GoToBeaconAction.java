package grah8384;

import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;

public class GoToBeaconAction extends GoToAction {	
	
	public GoToBeaconAction(Ship ship, Beacon beacon) {
		this.ship = ship;
		this.goal = beacon;
	}
	
	/**
	 * Add effects of this action to the state
	 * @param state
	 */
	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation r = new StateRepresentation(state);
		//Set ship location to where the asteroid is
		r.setAt(ship.getId(), goal.getPosition());
		//Add 2500 (energy in beacon) to ship
		r.addEnergy(ship.getId(), 2500.0);
		//Remove the asteroid from the state 
		r.removeObject(goal);
		return r;
	}
	
	public double getPathCost(StateRepresentation state) {
		return super.getPathCost(state) - 2500;
	}
}
