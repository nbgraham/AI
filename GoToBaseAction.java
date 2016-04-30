package grah8384;

import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;

public class GoToBaseAction extends GoToAction {
	
	public GoToBaseAction(Ship ship, Base base) {
		this.ship = ship;
		this.goal = base;
	}

	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation r = new StateRepresentation(state);
		//Set ship location to where the asteroid is
		r.setAt(ship.getId(), goal.getPosition());
		return r;
	}
}
