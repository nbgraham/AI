package grah8384;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import spacesettlers.clients.Team;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class Planning {
	HashMap<Node, LinkedList<Node>> heads;
	
	public Planning(Toroidal2DPhysics space, Team team) {
		heads = new HashMap<Node, LinkedList<Node>>();
		for (Ship s : team.getShips()) {
			heads.put(new Node(new StateRepresentation(space, team.getTeamName()),s), null);
		}
	}
	
	public void search() {
		for (Node head : heads.keySet()) {
			PriorityQueue<Node> fringe = new PriorityQueue<Node>();
			fringe.add(head);
			Node current;
			while(true) {
				current = fringe.poll();
				if (current == null) {
					heads.put(head, null);
					System.err.println("current node is null");
					break;
				} else if (current.isGoal()) {
					heads.put(head, current.getPath());
					System.out.println("Found goal");
					break;
				}
				fringe.addAll(current.explore());
				System.out.println("Exploring node");
			}
		}
	}
	
	public static LinkedList<Node> search(Toroidal2DPhysics space, Ship ship) {
		Node head = new Node(new StateRepresentation(space, ship.getTeamName()), ship);
		LinkedList<Node> path;
		
		PriorityQueue<Node> fringe = new PriorityQueue<Node>();
		fringe.add(head);
		Node current;
		while(true) {
			current = fringe.poll();
			if (current == null) {
				path = null;
				System.err.println("current node is null");
				break;
			} else if (current.isGoal()) {
				path = current.getPath();
				System.out.println("Found goal");
				break;
			}
			fringe.addAll(current.explore());
			System.out.println("Exploring node");
		}
		
		System.out.println(path);
		return path;
	}

}
