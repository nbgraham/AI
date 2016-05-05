package grah8384;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Ship;
import spacesettlers.objects.resources.ResourcePile;

public class GoToAsteroidAction extends GoToAction {
	
	public GoToAsteroidAction(Ship ship, Asteroid asteroid) {
		this.ship = ship;
		this.goal = asteroid;
	}
	
	public boolean isApplicable(StateRepresentation state) {
		return super.isApplicable(state) && state.energy.get(ship.getId()) > 1500 && state.resources.get(ship.getId()).getTotal()/state.energy.get(ship.getId()) < 1;
	}
	
	/**
	 * Add effects of this action to the state
	 * @param state
	 */
	public StateRepresentation effects(StateRepresentation state) {
		StateRepresentation result = super.effects(state);
		//Get resources from asteroid
		result.addResources(ship.getId(), goal.getResources());
		//Remove the asteroid from the state 
		result.removeObject(goal);

		return result;
	}
	
	public ResourcePile getResources() {
		return goal.getResources();
	}
	
	public double getPathCost(StateRepresentation state) {
		return super.getPathCost(state)/goal.getResources().getTotal();
	}
}
