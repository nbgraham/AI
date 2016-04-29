package grah8384;

import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;

public class GoToBeaconAction {
	Ship ship;
	Beacon beacon;
	
	public GoToBeaconAction(Ship ship, Beacon beacon) {
		this.ship = ship;
		this.beacon = beacon;
	}
	
	/**
	 * Check that the action is applicable, given the state
	 * @param state
	 * @return
	 */
	public boolean isApplicable(StateRepresentation state) {
		//If ship is not already at asteroid
		if (state.at(ship.getId(), beacon.getPosition())) {
			return false;
		}
		//And ship has enough energy to make it to asteroid
		if (BetterObjectMovement.getEnergyCost(state.space, ship, beacon.getPosition()) > ship.getEnergy()) {
			return false;
		}
		return true;
	}
	
	/**
	 * Add effects of this action to the state
	 * @param state
	 */
	public void effects(StateRepresentation state) {
		//Set ship location to where the asteroid is
		state.setAt(ship.getId(), beacon.getPosition());
		//Add 2500 (energy in beacon) to ship
		state.addEnergy(ship.getId(), 2500.0);
		//Remove the asteroid from the state 
		state.removeObject(beacon);
	}
}
