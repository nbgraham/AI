package grah8384;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import spacesettlers.actions.MoveAction;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

import grah8384.Node;
import grah8384.Methods;


public class Graph {

	Set<Node> graph;
	Node start;
	Node goal;
	
	Toroidal2DPhysics space;
	
	public Graph(Toroidal2DPhysics space, Position start, Position goal, int sampleSize){
		this.graph = new HashSet<Node>();
		this.start = new Node(start, space.findShortestDistance(start, goal));
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
		
		//sample space <sampleSize> times
		for(int i=0; i<sampleSize; i+=1){
			//get a random point
			Position samplePosition = space.getRandomFreeLocation(seed, Ship.SHIP_RADIUS);
			//construct a node from the random point
			Node sampleNode = new Node(samplePosition, space.findShortestDistance(samplePosition, this.goal.getPosition()));
			//append the sample node to the list
			sampleNodes.add(sampleNode);
		}
		
		for(int i=0; i<sampleSize; i+=1){
			Node nodeI = sampleNodes.get(i);
			for(int j=i+1; j<sampleSize; j+=1){
				Node nodeJ = sampleNodes.get(j);
				//if connected
				if(space.isPathClearOfObstructions(nodeI.getPosition(), nodeJ.getPosition(), obstructions, Ship.SHIP_RADIUS)){
					//make nodes neighbors
					nodeI.addNeighbor(nodeJ);
					nodeJ.addNeighbor(nodeI);
				}
			}
			if(nodeI.hasNeighbors()){
				this.graph.add(nodeI);
			}
		}
	}
	
	public ArrayList<Node> getPath(){
		//TODO
		return null;
	}
	
	private int getCost(Node initialNode, Node finalNode)
	{
		double angularAccel =  Math.abs((new MoveAction()).pdControlOrientToGoal(space, finalNode.getPosition(),initialNode.getPosition(), 0)); 
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