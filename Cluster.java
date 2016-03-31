package grah8384;

import java.util.ArrayList;

import spacesettlers.objects.AbstractObject;
import spacesettlers.utilities.Position;

public class Cluster {
	Position center;
	ArrayList<AbstractObject> objects;
	double xSum = 0;
	double ySum = 0;
	double totalWeight = 0;
	
	public Cluster(Position center) {
		this.center = center;
		objects = new ArrayList<AbstractObject>();
	}
	
	public Position getCenter() {
		return center;
	}
	
	public void addObject(AbstractObject object) {
		objects.add(object);
		xSum += object.getMass()*object.getPosition().getX();
		ySum += object.getMass()*object.getPosition().getY();
		totalWeight += object.getMass();
	}
	
	public Position resetCenter() {
		Position newCenter = new Position(xSum/totalWeight,ySum/totalWeight);
		center = newCenter;
		return newCenter;
	}
	
	public String toString() {
		return new String("Center: (" + center.getX() + ", " + center.getY() + ") Objects: " + objects.size());
	}
}
