package grah8384;

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
		this.space = space;
		this.ship = ship;
		this.goalObject = goal;
		
		double dist = space.findShortestDistance(ship.getPosition(), goal.getPosition());
		this.goal = new GoalNode(goal, getPredictedPosition(goal.getPosition(), dist), dist);
		this.start = new Node(ship.getPosition(), space.findShortestDistance(ship.getPosition(), this.goal.getPosition()), 0);
	
		buildGraph(sampleSize);
	}
	
	private static Position getPredictedPosition(Position position, double dist) {
		Position predictedPosition = new Position(position.getX() + position.getTranslationalVelocity().getXValue() * dist * 0.03, position.getY() + position.getTranslationalVelocity().getYValue() * dist * 0.03);
		return predictedPosition;
	}

	private void buildGraph(int sampleSize){
		//set of avoided objects
		Set<AbstractObject> obstructions = new HashSet<AbstractObject>();
		//add objects as obstructions
		obstructions.addAll(Methods.getNonMineableAsteroids(space));
		obstructions.addAll(space.getBases());
		obstructions.addAll(space.getShips());
		
		obstructions.remove(ship);
		obstructions.remove(goalObject);

		//random seed for point generation
		Random seed = new Random(System.currentTimeMillis());
		//list of sampled nodes
		graph.add(this.start);
		graph.add(this.goal);
		
		Vector2D shortestVector = space.findShortestDistanceVector(start.position, goal.position);
		Position oneThird = add(start.position, shortestVector.multiply(.33));
		Position twoThirds = add(start.position, shortestVector.multiply(.66));
		
		if (space.isPathClearOfObstructions(start.getPosition(), goal.getPosition(), obstructions, Ship.SHIP_RADIUS))
		{
			start.addNeighbor(goal);
			goal.addNeighbor(start);
		}
		else
		{
			for(int i=0; i < sampleSize; i++){
				
				//get a random point
				Position samplePosition;
				if (i < sampleSize/2) samplePosition = space.getRandomFreeLocationInRegion(seed, Ship.SHIP_RADIUS, (int) oneThird.getX(), (int) twoThirds.getY(), shortestVector.getMagnitude()*.66);
				else samplePosition = space.getRandomFreeLocationInRegion(seed, Ship.SHIP_RADIUS, (int) twoThirds.getX(), (int) twoThirds.getY(), shortestVector.getMagnitude()*.66);
				//construct a node from the random point
				Node nodeI = new Node(samplePosition);
				
				for(Node nodeJ : graph){
					if ( nodeI.position == nodeJ.position) continue;
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
	}
	
	private Position add(Position position, Vector2D vector) {
		return new Position(position.getX() + vector.getXValue(), position.getY() + vector.getYValue());
	}

	public LinkedList<Node> getPath(){
		Node currentNode;

		if (start.neighbors.contains(goal))
		{
			goal.setParent(start);
		}
		else
		{
			HashSet<Node> visited = new HashSet<Node>();
			PriorityQueue<Node> q = new PriorityQueue<Node>();
			q.add(this.start);
			visited.add(this.start);
						
			while(!q.isEmpty()){			
				currentNode = q.poll();
				visited.add(currentNode);
	
				for (Node n : currentNode.neighbors){
					if (!visited.contains(n))
					{
						n.setHeuristic(space.findShortestDistance(n.position, goal.position));
						//If the path cost is better, then set the parent (change the path to a better path)
						if (n.setPathCost(currentNode.bestPathCost + getCost(currentNode,n))) n.setParent(currentNode);
						if (n.position.equals(goal.position)) {
							goal.setParent(currentNode);
							q.clear();
							break;
						}
						q.add(n);
					}
				}
			}
		}
		
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