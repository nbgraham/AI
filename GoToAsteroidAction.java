package grah8384;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Ship;

public class GoToAsteroidAction {
	Ship ship;
	Asteroid asteroid;
	
	public GoToAsteroidAction(Ship ship, Asteroid asteroid) {
		this.ship = ship;
		this.asteroid = asteroid;
	}
	
	/**
	 * Check that the action is applicable, given the state
	 * @param state
	 * @return
	 */
	public boolean isApplicable(StateRepresentation state) {
		//If ship is not already at asteroid
		if (state.at(ship.getId(), asteroid.getPosition())) {
			return false;
		}
		//And ship has enough energy to make it to asteroid
		if (BetterObjectMovement.getEnergyCost(state.space, ship, asteroid.getPosition()) > ship.getEnergy()) {
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
		state.setAt(ship.getId(), asteroid.getPosition());
		//Get resources from asteroid
		state.addResources(ship.getId(), asteroid.getResources());
		//Remove the asteroid from the state 
		state.removeObject(asteroid);
	}
}
