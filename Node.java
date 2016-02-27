package grah8384;

import java.util.HashSet;
import java.util.Set;


import spacesettlers.utilities.Position;

public class Node implements Comparable<Node> {
	Set<Node> neighbors;
	double heuristic;
	int bestPathCost;
	Position position;
	
	public Node(Position pointLocation, double distToGoal){
		this.neighbors = new HashSet<Node>();
		this.heuristic = distToGoal;
		this.position = pointLocation;
		this.bestPathCost = Integer.MAX_VALUE;
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
	
	public Position getPosition(){
		return this.position;
	}
	
	public int getBestPathCost(){
		return this.bestPathCost;
	}
	
	public void setPathCost(int pathCost){
		this.bestPathCost = pathCost;
	}

	@Override
	public int compareTo(Node other) {
		return Integer.compare((int) (heuristic + bestPathCost), (int) (other.heuristic + other.bestPathCost));
	}

}