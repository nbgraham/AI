package grah8384;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;


public class GoalNode extends Node
{
	private AbstractObject goal;
	private Position predictedPosition;
	Toroidal2DPhysics space;
	
	public GoalNode( Toroidal2DPhysics space, AbstractObject goal, Position predPos, double shortestDistanceFromStartToGoal) {
		super(predPos, 0);
		this.goal = goal;
		this.predictedPosition = predPos;
		this.space = space;
	}
	
	public int getGoalRadius() {
		return goal.getRadius();
	}
	
	public AbstractObject getGoalObject() {
		return goal;
	}
	
	public boolean isGone() {
		if (space.getObjectById(goal.getId()) == null) {
			return true;
		}
		
		if (!space.getObjectById(goal.getId()).isAlive()) {
			return true;
		} 
		
		Vector2D disp = new Vector2D(predictedPosition.getX() - goal.getPosition().getX(), predictedPosition.getY() - goal.getPosition().getY());
		if (disp.angleBetween(goal.getPosition().getTranslationalVelocity()) > 30)
		{
			if (disp.getMagnitude() > (Ship.SHIP_RADIUS + goal.getRadius()))
				return true;
		}
		
		return false;
	}
}
