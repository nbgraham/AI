package grah8384;

import java.util.HashSet;
import java.util.LinkedList;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;

public class Node implements Comparable<Node> {
	StateRepresentation state;
	GoToAction action;
	HashSet<Node> possibleActions;
	Node parent;
	Ship ship;
	
	double pathCost;
	int resourcesCollected;
	
	public Node(StateRepresentation state, Ship ship) {
		this.state = state;
		action = null;
		possibleActions = null;
		parent = null;
		pathCost = 0;
		resourcesCollected = 0;
		this.ship = ship;
	}
	
	public HashSet<Node> explore() {
		GoToAction action;
		possibleActions = new HashSet<Node>();
		for (Beacon b : state.beacons) {
			action = new GoToBeaconAction(ship, b);
			if (action.isApplicable(state)) {
				possibleActions.add(createChild(action));
			}
		}
		for (Base b : state.bases) {
			action = new GoToBaseAction(ship, b);
			if (action.isApplicable(state)) {
				possibleActions.add(createChild(action));
			}
		}
		for (Asteroid a : state.asteroids) {
			action = new GoToAsteroidAction(ship, a);
			if (action.isApplicable(state)) {
				possibleActions.add(createChild(action));
			}
		}
		return possibleActions;
	}
	
	public Node createChild(GoToAction action) {
		Node result = new Node(state, ship);
		result.action = action;
		result.parent = this;
		result.state = action.effects(state);
		result.pathCost = this.pathCost + action.getPathCost(state);
		result.resourcesCollected = this.resourcesCollected + action.getResources();
		return result;
	}
	
	public double evaluate() {
		return resourcesCollected/pathCost;
	}

	@Override
	public int compareTo(Node o) {
		return -1 * Double.compare(this.evaluate(), o.evaluate());
	}
	
	public boolean isGoal() {
		if (evaluate() < 1) return false;
		for (Base b : state.bases) {
			if (state.at(ship.getId(), b.getPosition())) return true;
		}
		return false;
	}
	
	public LinkedList<Node> getPath() {
		Node current = this;
		LinkedList<Node> result = new LinkedList<Node>();
		while (current.action != null) {
			result.addFirst(current);
			current = current.parent;
		}
		return result;
	}
	
	public String toString() {
		String result = "";
		if (action instanceof GoToAsteroidAction) {
			result += "Go to asteroid. ";
		} else if (action instanceof GoToBeaconAction) {
			result += "Go to beacon. ";
		} else if (action instanceof GoToBaseAction) {
			result += "Go to base. ";
		}
		result += "Path cost: " + pathCost;
		result += ". Resources collected: " + resourcesCollected;
		
		return result;
	}
}
