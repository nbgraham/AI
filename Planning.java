package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.UUID;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class Planning {
	HashMap<UUID, LinkedList<Node>> paths;
	HashMap<UUID, UUID> shipIDToNextGoalID;
	
	static final int EXPLORATION_LIMIT = 5;
	
	public Planning(HashSet<Ship> teamShips) {
		paths = new HashMap<UUID, LinkedList<Node>>();
		shipIDToNextGoalID = new HashMap<UUID, UUID>();
		for (Ship ship : teamShips) {
			paths.put(ship.getId(), null);
		}
	}
	
	public void search(Toroidal2DPhysics space) {
		for (UUID shipID : paths.keySet()) {
			paths.put(shipID, null);
		}
		
		HashMap<UUID, Node> shipIDToNode = new HashMap<UUID, Node>();
		HashMap<UUID, Node> goalIDToNode = new HashMap<UUID, Node>();
		HashMap<UUID, PriorityQueue<Node>> shipIDToFringe = new HashMap<UUID, PriorityQueue<Node>>();
		
		//Initialize fringe for each ship to have starting node
		for (UUID shipID : paths.keySet()) {
			Ship ship = (Ship) space.getObjectById(shipID);
			Node head = new Node(new StateRepresentation(space, ship.getTeamName()), ship);
			
			shipIDToNode.put(shipID, head);
		}
		
		Node current;
			
		//Build fringes
		for (UUID shipID : paths.keySet()) {
			current = shipIDToNode.get(shipID);
						
			if (current == null) {
				System.err.println("Current node is null.");
				break;
			}
			
			shipIDToFringe.put(shipID, new PriorityQueue<Node>(current.explore()));
		}
		
		//Assign unique best actions to ships
		for (UUID shipID : paths.keySet()) {
			findAvailableGoal(shipID, shipIDToFringe, goalIDToNode, shipIDToNode);
		}
		
		for (UUID shipID : paths.keySet()) {
			shipIDToNextGoalID.put(shipID, shipIDToNode.get(shipID).action.goal.getId());
		}
	}
		
	private Node findAvailableGoal(UUID shipID, HashMap<UUID, PriorityQueue<Node>> fringe, HashMap<UUID, Node> goalObjectToNode,HashMap<UUID, Node> shipToNode) {
		Node current = fringe.get(shipID).poll();
		
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
		AbstractObject goal = space.getObjectById(shipIDToNextGoalID.get(shipID));
		
		//Bounced off base?
		if (goal instanceof Base && space.getObjectById(shipID).getResources().getTotal() == 0 && shipAimingForBase) {
			//Done with go to base action
			return null;
		} else {
			return goal;
		}
	}
}
