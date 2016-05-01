package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.UUID;

import spacesettlers.clients.Team;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class Planning {
	HashMap<UUID, LinkedList<Node>> paths;
	HashMap<UUID, UUID> shipToObjectMap;
	
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
		
		while(count < 200) {
			count++;
			current = fringe.poll();
			if (current == null) {
				System.err.println("current node is null");
				break;
			} else if (current.isGoal()) {
				path = current.getPath();
				System.out.println("Found goal");
				break;
			}
			fringe.addAll(current.explore(shipToObjectMap));
			System.out.println("Exploring node");
		}
		
		if (count == 200) System.err.println("Search took too long");
		
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
	
	 /*
	public static LinkedList<Node> search(Toroidal2DPhysics space, Ship ship) {
		Node head = new Node(new StateRepresentation(space, ship.getTeamName()), ship);
		LinkedList<Node> path = null;
		
		PriorityQueue<Node> fringe = new PriorityQueue<Node>();
		fringe.add(head);
		Node current;
		int count = 0;
		while(count < 200) {
			count ++;
			current = fringe.poll();
			if (current == null) {
				path = null;
				System.err.println("current node is null");
				break;
			} else if (current.isGoal()) {
				path = current.getPath();
				System.out.println("Found goal: Count " + count);
				break;
			}
			fringe.addAll(current.explore(shipToObjectMap));
			System.out.println("Exploring node");
		}
		if (count == 200) {
			System.err.println("Search took too long");
			return null;
		} else {
			System.out.println(path);
			return path;
		}
	}
	*/

}
