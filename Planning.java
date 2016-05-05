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
	HashMap<UUID, UUID> shipToObjectMap;
	static final int EXPLORATION_LIMIT = 25;
	
	public Planning(HashSet<Ship> teamShips) {
		paths = new HashMap<UUID, LinkedList<Node>>();
		for (Ship ship : teamShips) {
			paths.put(ship.getId(), null);
		}
	}
	
	public void search(Toroidal2DPhysics space) {
		shipToObjectMap = new HashMap<UUID, UUID>();
		for (UUID shipID : paths.keySet()) {
			Ship ship = (Ship) space.getObjectById(shipID);
			Node head = new Node(new StateRepresentation(space, ship.getTeamName()), ship);
			search(head);
		}
	}
	
	private void search(Node head) {
		PriorityQueue<Node> fringe = new PriorityQueue<Node>();
		fringe.add(head);
		
		LinkedList<Node> path = null;
		Node current;
		int count = 0;
		int maxExplore = (int) (EXPLORATION_LIMIT * 0.7 * (1+ paths.size()));
		while(count < maxExplore) {
			count++;
			current = fringe.poll();
			if (current == null) {
				System.err.println("current node is null");
				break;
			} else if (current.isGoal()) {
				path = current.getPath();
				System.out.println("Found goal. Count:" + count + " Path length: " + path.size() + ". Evaluate: " + current.evaluate());
				break;
			}
			fringe.addAll(current.explore(shipToObjectMap));
		}
		
		if (count == maxExplore) System.err.println("Search explored more than " + maxExplore + " nodes");
		
		paths.put(head.ship.getId(), path);
		
		if (path != null) {
			for (Node node : path) {
				if (node.action != null) shipToObjectMap.put(head.ship.getId(), node.action.goal.getId());
			}
		}
	}
	
	public LinkedList<Node> getPath(UUID shipID) {
		return paths.get(shipID);
	}
	
	public AbstractObject getNextTarget(UUID shipID, Toroidal2DPhysics space, boolean shipAimingForBase) {
		LinkedList<Node> plan = paths.get(shipID);
		
		Node currentNode = plan.peek();
		AbstractObject currentGoal = space.getObjectById(currentNode.action.goal.getId());
		
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
