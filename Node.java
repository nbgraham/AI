package grah8384;

import java.util.HashSet;
import java.util.Set;


import spacesettlers.utilities.Position;

public class Node implements Comparable<Node> {
	Node parent;
	Set<Node> neighbors;
	double heuristic;
	int bestPathCost;
	Position position;
	
	public Node(Position pointLocation, double distToGoal, int pathCost){
		parent = null;
		this.neighbors = new HashSet<Node>();
		this.heuristic = distToGoal;
		this.position = pointLocation;
		this.bestPathCost = pathCost;
	}
	
	public Node(Position pointLocation, double distToGoal){
		parent = null;
		this.neighbors = new HashSet<Node>();
		this.heuristic = distToGoal;
		this.position = pointLocation;
		this.bestPathCost = Integer.MAX_VALUE;
	}
	
	public Node(Position pointLocation){
		parent = null;
		this.neighbors = new HashSet<Node>();
		this.heuristic = Integer.MAX_VALUE;
		this.position = pointLocation;
		this.bestPathCost = Integer.MAX_VALUE;
	}
	
	public Node getParent(){
		return this.parent;
	}
	
	public void setParent(Node parent){
		this.parent = parent;
	}
	
	public boolean addNeighbor(Node n){
		return this.neighbors.add(n);
	}
	
	public boolean removeNeighbor(Node n){
		return this.neighbors.remove(n);
	}
	
	public boolean hasNeighbors(){
		return this.neighbors.size() > 0;
	}
	
	public Set<Node> getNeighbors(){
		return this.neighbors;
	}
	
	public double getHeuristic(){
		return this.heuristic;
	}
	
	public void setHeuristic(double heuristic){
		this.heuristic = heuristic;
	}
	
	public Position getPosition(){
		return this.position;
	}
	
	public int getBestPathCost(){
		return this.bestPathCost;
	}
	
	public void setPathCost(int pathCost){
		if (pathCost < this.bestPathCost) this.bestPathCost = pathCost;
	}

	@Override
	public int compareTo(Node other) {
		return Integer.compare((int) (heuristic + bestPathCost), (int) (other.heuristic + other.bestPathCost));
	}

}