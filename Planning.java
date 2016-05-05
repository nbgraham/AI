package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.UUID;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class Planning {
	HashMap<UUID, LinkedList<Node>> paths;
	static final int EXPLORATION_LIMIT = 10;
	
	public Planning(HashSet<Ship> teamShips) {
		paths = new HashMap<UUID, LinkedList<Node>>();
		for (Ship ship : teamShips) {
			paths.put(ship.getId(), null);
		}
	}
	
	public void search(Toroidal2DPhysics space) {
		HashSet<UUID> takenObjects = new HashSet<UUID>();
		
		//Initialize fringe for each ship to have starting node
		HashMap<UUID, PriorityQueue<Node>> shipToFringe = new HashMap<UUID, PriorityQueue<Node>>();
		for (UUID shipID : paths.keySet()) {
			Ship ship = (Ship) space.getObjectById(shipID);
			Node head = new Node(new StateRepresentation(space, ship.getTeamName()), ship);
			PriorityQueue<Node> fringe = new PriorityQueue<Node>();
			fringe.add(head);
			shipToFringe.put(shipID, fringe);
		}
		
		Node current;
		int depth = 0;
		
		while(depth < EXPLORATION_LIMIT) {
			depth++;
			
			//Assign unique best actions to ships
			HashMap<UUID, Node> shipIDToNode = new HashMap<UUID, Node>();
			HashMap<UUID, Node> goalIDToNode = new HashMap<UUID, Node>();
			for (UUID shipID : paths.keySet()) {
				if (paths.get(shipID) != null) continue; //Pass if a complete plan is already found
				shipIDToNode.put(shipID, shipToFringe.get(shipID).poll());
//				findAvailableGoal(shipID, shipToFringe, goalIDToNode, shipIDToNode);
			}
			
			//Add all chosen objects to taken objects set
			for (UUID shipID : shipIDToNode.keySet()) {
				if (paths.get(shipID) != null) continue; //Pass if a complete plan is already found
				if (shipIDToNode.get(shipID).action != null) takenObjects.add(shipIDToNode.get(shipID).action.goal.getId());
			}
			
			//Explore each ship's node
			for (UUID shipID : paths.keySet()) {
				if (paths.get(shipID) != null) continue; //Pass if a complete plan is already found

				current = shipIDToNode.get(shipID);
				
				if (current == null) {
					System.err.println("Current node is null. Count: " + depth);
					break;
				} else if (current.action != null && current.action instanceof GoToBaseAction) {
					paths.put(shipID, current.getPath());
					System.out.println("Found goal. Count:" + depth + " Path length: " + paths.get(shipID).size() + ". Evaluate: " + current.evaluate());
					break;
				}
				
				shipToFringe.get(shipID).clear();
				shipToFringe.get(shipID).addAll(current.explore(takenObjects));
			}
		}
		
		if (depth == EXPLORATION_LIMIT) {
			System.err.println("Search went deeper than " + EXPLORATION_LIMIT);
		}
	}
		
	private Node findAvailableGoal(UUID shipID, HashMap<UUID, PriorityQueue<Node>> fringe, HashMap<UUID, Node> goalObjectToNode,HashMap<UUID, Node> shipToNode) {
		Node current = fringe.get(shipID).poll();
		
		//Initial node with no action
		if (current.action == null) {
			shipToNode.put(shipID, current);
			return current;
		}
		
		while (current != null && goalObjectToNode.containsKey(current.action.goal.getId())) { 
			if (goalObjectToNode.get(current.action.goal.getId()).evaluate() <= current.evaluate()) {
				current = fringe.get(shipID).poll();
			} else {
				shipToNode.put(shipID, current);
				goalObjectToNode.put(current.action.goal.getId(), current);
				findAvailableGoal(goalObjectToNode.get(current.action.goal.getId()).ship.getId(), fringe, goalObjectToNode, shipToNode);
			}
		}
		
		shipToNode.put(shipID, current);
		goalObjectToNode.put(current.action.goal.getId(), current);
		
		return current;
	}
	
	public LinkedList<Node> getPath(UUID shipID) {
		return paths.get(shipID);
	}
	
	public AbstractObject getNextTarget(UUID shipID, Toroidal2DPhysics space, boolean shipAimingForBase) {
		if (paths == null) {
			System.err.println("Planner paths object is null");
			return null;
		}

		LinkedList<Node> plan = paths.get(shipID);
		if (plan == null) {
			System.err.println("Plan for ship is null");
			return null;
		}
		
		Node currentNode = plan.peek();
		AbstractObject currentGoal;
		
		if (currentNode == null) {
			System.err.println("First node in plan is null");
			return null;
		}
		
		if (currentNode.action == null) {
			currentGoal = null;
		} else {
			currentGoal = space.getObjectById(currentNode.action.goal.getId());
		}
		
		while (currentGoal == null || !currentGoal.isAlive()) {
			plan.pop();
			currentNode = plan.peek();
			currentGoal = space.getObjectById(currentNode.action.goal.getId());
		}
		
		//Bounced off base?
		if (currentNode.action instanceof GoToBaseAction && space.getObjectById(shipID).getResources().getTotal() == 0 && shipAimingForBase) {
			//Done with go to base action
			plan.pop();
			return null;
		} else {
			return currentGoal;
		}
	}
}
