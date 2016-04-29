package grah8384;

import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;

public class GoToBaseAction {
	Ship ship;
	Base base;
	
	public GoToBaseAction(Ship ship, Base base) {
		this.ship = ship;
		this.base = base;
	}
	
	public boolean isApplicable(StateRepresentation state) {
		//If ship is not at the base already
		if (state.at(ship.getId(), base.getPosition())) {
			return false;
		}
		//And ship has enough energy to make it to asteroid
		if (BetterObjectMovement.getEnergyCost(state.space, ship, base.getPosition()) > ship.getEnergy()) {
			return false;
		}
		return true;
	}
	
	public void effects(StateRepresentation state) {
		//Set ship location to where the asteroid is
		state.setAt(ship.getId(), base.getPosition());
	}
}
