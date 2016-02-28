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
	Ship ship;
	AbstractObject goalObject;
	
	Toroidal2DPhysics space;
	
	public Graph(Toroidal2DPhysics space, Ship ship, AbstractObject goal, int sampleSize){
		this.graph = new HashSet<Node>();
		this.start = new Node(ship.getPosition(), space.findShortestDistance(ship.getPosition(), goal.getPosition()), 0);
		this.goal = new Node(goal.getPosition(), 0);
		this.space = space;
		this.ship = ship;
		this.goalObject = goal;
		
		buildGraph(sampleSize);
	}
	
	private void buildGraph(int sampleSize){
		//set of avoided objects
		Set<AbstractObject> obstructions = new HashSet<AbstractObject>();
		//add objects as obstructions
		obstructions.addAll(Methods.getNonMineableAsteroids(space));
		obstructions.addAll(space.getBases());
		obstructions.addAll(space.getShips());
		
		System.out.println("Ship removed: " + obstructions.remove(ship));
		System.out.println("Goal removed: " + obstructions.remove(goalObject));

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
				if ( i == j) continue;
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
		HashSet<Node> visited = new HashSet<Node>();
		PriorityQueue<Node> q = new PriorityQueue<Node>();
		q.add(this.start);
		visited.add(this.start);
		
		int count = 0;
		System.out.println("Start neighbors: " + start.neighbors.size());
		System.out.println("Goal neighbors: " + goal.neighbors.size());

		Node currentNode;
		while(!q.isEmpty()){
			count++;
			
			currentNode = q.poll();
			visited.add(currentNode);

			for (Node n : currentNode.neighbors){
				if (!visited.contains(n))
				{
					n.setHeuristic(space.findShortestDistance(n.position, goal.position));
					n.setPathCost(currentNode.bestPathCost + getCost(currentNode,n));
					n.setParent(currentNode);
					if (n.position.equals(goal.position)) {
						System.out.println("Goal found");
						goal.setParent(currentNode);
						q.clear();
						break;
					}
					q.add(n);
				}
			}
		}
		
		System.out.println("Max depth: " + count);
		LinkedList<Node> result = new LinkedList<Node>();
		currentNode = goal;
		if (currentNode.parent == null) return null; //Failure
		while(currentNode.parent != null)
		{
			result.addFirst(currentNode);
			currentNode = currentNode.parent;
		}
		
		return result;
	}
	
	private int getCost(Node initialNode, Node finalNode)
	{
		return 200;
		//return (int) space.findShortestDistance(initialNode.position, finalNode.position)*2;
	}
	
	public void print(){
		for(Node n : this.graph){
			System.out.println("X: "+n.getPosition().getX()+" Y: "+n.getPosition().getY()+" Neighbors: "+n.getNeighbors().size());
		}
	}
}