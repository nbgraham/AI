package grah8384;

public class Average {
	double sum;
	int count;
	
	public Average() {
		sum = 0;
		count = 0;
	}
	
	public void add(double value) {
		sum += value;
		count++;
	}
	
	public double average() {
		return sum/count;
	}
}
