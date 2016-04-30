package grah8384;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Ship;

public class GoToAsteroidAction extends GoToAction {
	
	public GoToAsteroidAction(Ship ship, Asteroid asteroid) {
		this.ship = ship;
		this.goal = asteroid;
	}
	
	/**
	 * Add effects of this action to the state
	 * @param state
	 */
	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation result = new StateRepresentation(state);
		//Set ship location to where the asteroid is
		result.setAt(ship.getId(), goal.getPosition());
		//Get resources from asteroid
		result.addResources(ship.getId(), goal.getResources());
		//Remove the asteroid from the state 
		result.removeObject(goal);
		
		return result;
	}
	
	public int getResources() {
		return goal.getResources().getMass();
	}
}
