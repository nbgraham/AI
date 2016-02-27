package grah8384;

import java.util.HashSet;
import java.util.Set;


import spacesettlers.utilities.Position;

public class Node {
	Set<Node> neighbors;
	double heuristic;
	Position position;
	
	public Node(Position pointLocation, double distToGoal){
		this.neighbors = new HashSet<Node>();
		this.heuristic = distToGoal;
		this.position = pointLocation;
	}
	
	public boolean addNeighbor(Node n){
		return this.neighbors.add(n);
	}
	
	public boolean removeNeighbor(Node n){
		return this.neighbors.remove(n);
	}
	
	public boolean hasNeighbors(){
		return this.neighbors.size()>0;
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
	
	

}