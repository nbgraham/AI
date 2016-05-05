package grah8384;

import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;

public class GoToBaseAction extends GoToAction {
	
	public GoToBaseAction(Ship ship, Base base) {
		this.ship = ship;
		this.goal = base;
	}
	
	public boolean isApplicable(StateRepresentation state) {
		return super.isApplicable(state) && state.resources.get(ship.getId()).getTotal() > 100; 
	}
}
