package grah8384;

import spacesettlers.objects.AbstractObject;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;


public class GoalNode extends Node
{
	private AbstractObject goal;
	
	public GoalNode(AbstractObject goal, double shortestDistanceFromStartToGoal) {
		super(add(goal.getPosition(), goal.getPosition().getTranslationalVelocity().multiply(shortestDistanceFromStartToGoal*0.1)), 0);
		this.goal = goal;
	}
	
	private static Position add(Position position, Vector2D displacementVector) {
		position.setX(position.getX() + displacementVector.getXValue());
		position.setY(position.getY() + displacementVector.getYValue());
		return position;
	}
	
	public int getGoalRadius() {
		return goal.getRadius();
	}
}
