package grah8384;

import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;

public class GoToBaseAction extends GoToAction {
	
	public GoToBaseAction(Ship ship, Base base) {
		this.ship = ship;
		this.goal = base;
	}
	
	public boolean isApplicable(StateRepresentation state) {
		return super.isApplicable(state) && state.resources.get(ship.getId()).getMass() > 100; 
	}

	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation result = super.effects(state);
		//Set ship location to where the asteroid is
		result.setAt(ship.getId(), goal.getPosition());
		result.addEnergy(ship.getId(), -1*getPathCost(state));
		return result;
	}
}
