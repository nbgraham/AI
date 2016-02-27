package grah8384;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import spacesettlers.actions.MoveAction;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;


public class Graph {

	HashSet<Node> graph;
	Node start;
	Node goal;
	
	Toroidal2DPhysics space;
	
	public Graph(Toroidal2DPhysics space, Position start, Position goal, int sampleSize){
		this.graph = new HashSet<Node>();
		this.start = new Node(start, space.findShortestDistance(start, goal), 0);
		this.goal = new Node(goal, 0);
		this.space = space;
		
		buildGraph(sampleSize);
	}
	
	private void buildGraph(int sampleSize){
		//set of avoided objects
		Set<AbstractObject> obstructions = new HashSet<AbstractObject>();
		//add objects as obstructions
		obstructions.addAll(Methods.getNonMineableAsteroids(space));
		obstructions.addAll(space.getBases());
		obstructions.addAll(space.getShips());
		//random seed for point generation
		Random seed = new Random(System.currentTimeMillis());
		//list of sampled nodes
		List<Node> sampleNodes = new ArrayList<Node>();
		sampleNodes.add(this.start);
		sampleNodes.add(this.goal);
		
		for(int i=0; i < sampleSize; i++){
			
			//get a random point
			Position samplePosition = space.getRandomFreeLocation(seed, Ship.SHIP_RADIUS);
			//construct a node from the random point
			Node nodeI = new Node(samplePosition);
			//append the sample node to the list
			sampleNodes.add(nodeI);
			
			for(int j=0; j < sampleNodes.size(); j++){
				Node nodeJ = sampleNodes.get(j);
				//if connected
				if(space.isPathClearOfObstructions(nodeI.getPosition(), nodeJ.getPosition(), obstructions, Ship.SHIP_RADIUS)){
					//make nodes neighbors
					nodeI.addNeighbor(nodeJ);
					nodeJ.addNeighbor(nodeI);
				}
			}
			
			this.graph.add(nodeI);
		}
	}
	
	public LinkedList<Node> getPath(){
		PriorityQueue<Node> q = new PriorityQueue<Node>();
		q.add(this.start);
		
		Node currentNode;
		while(!q.isEmpty()){
			currentNode = q.peek();
			q.remove();
			
			for (Node n : currentNode.neighbors){
				n.setHeuristic(space.findShortestDistance(start.position, n.position));
				n.setPathCost(currentNode.bestPathCost + getCost(currentNode,n));
				n.setParent(currentNode);
				if (n.position == goal.position) {
					goal.setParent(currentNode);
					break;
				}
				q.add(n);
			}
		}
		
		LinkedList<Node> result = new LinkedList<Node>();
		currentNode = goal;
		while(currentNode.parent != null)
		{
			result.addFirst(currentNode.parent);
		}
		
		return result;
	}
	
	private int getCost(Node initialNode, Node finalNode)
	{
		double angularAccel = ((new MoveAction()).pdControlMoveToGoal(space, finalNode.getPosition(),initialNode.getPosition(), new Vector2D(0,0))).getMagnitude(); 
		double angularInertia = (3.0 * Ship.SHIP_MASS * Ship.SHIP_RADIUS * angularAccel) / 2.0;
		int penalty = (int) Math.floor(Toroidal2DPhysics.ENERGY_PENALTY * angularInertia);
		
		
		return penalty;
	}
	
	public void print(){
		for(Node n : this.graph){
			System.out.println("X: "+n.getPosition().getX()+" Y: "+n.getPosition().getY()+" Neighbors: "+n.getNeighbors().size());
		}
	}
}