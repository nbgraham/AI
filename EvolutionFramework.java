package grah8384;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class EvolutionFramework {
	int popSize = 10;
	int tournSize = 3;
	double mutRate = 0.2;
	
	ArrayList<Gene> population = new ArrayList<Gene>();
	
	public EvolutionFramework() {
		Random rand = new Random();
		
		//Create random population
		for (int i = 0; i < popSize; i++ ){
			double shipEnergyBoundary = ((rand.nextGaussian() * 1000) + 2000);
			double asteroidCostBoundary = ((rand.nextGaussian() * 500000) + 1500000); 			//Need to change mean and sd
			double resourceBoundary = ((rand.nextGaussian() * 250) + 500);
			double homeCostBoundary = ((rand.nextGaussian() * 250) + 500); 			//Need to change mean and sd
			
			//Need to play with mean and sd
			double resourcesAroundWeight = ((rand.nextGaussian() * 10000) + 10000);
			double asteroidsAroundWeight = ((rand.nextGaussian() * 10000) + 10000);
			double distanceFromShipWeight = ((rand.nextGaussian() * 10000) + 10000);
			double distanceFromHomeWeight = ((rand.nextGaussian() * 10000) + 10000);
			double resourceOrEnergyWeight = ((rand.nextGaussian() * 10000) + 10000);
			double momentumDetrimentWeight = ((rand.nextGaussian() * 10000) + 10000);		
			
			double[] current = {shipEnergyBoundary, asteroidCostBoundary, resourceBoundary, homeCostBoundary, 
			                    resourcesAroundWeight, asteroidsAroundWeight, distanceFromShipWeight, distanceFromHomeWeight, resourceOrEnergyWeight, momentumDetrimentWeight};
			
			population.add(new Gene(current));
		}
	}
	
	public void evolve(int maxGen) {
		evaluateFitness();
		report() ;
		
		for (int i = 0; i < maxGen; i++) {
			createNewPop();
			mutatePop();
			evaluateFitness();
			report();
		}
	}
	
	private void evaluateFitness() {
		for (int i = 0; i < population.size(); i ++) {
			Gene cur = population.get(i);
			//TODO 
			//cur.fitness = simulate(cur);
		}
	}

	public void report() {
		assert(population.size() == popSize);
		
		Collections.sort(population);
		double fitnessSum = 0;
		for (int i = 0; i < popSize; i++) {
			fitnessSum += population.get(i).fitness;
			System.out.println("Individual " + i + ": " + population.get(i).weights + " fitness: "  + population.get(i).fitness);
		}
		
		System.out.println("Average fitness: " + fitnessSum/population.size() + "\n\n\n");
	}
	
	public Gene crossover(Gene parent1, Gene parent2) {
		
		double[] p1 = parent1.weights;
		double[] p2 = parent2.weights;
		
		assert(p1.length == p2.length);
		
		ArrayList<Double> child = new ArrayList<Double>();
		Random rand = new Random();
		
		for (int i = 0; i < p1.length; i++){
			if (rand.nextDouble() < .5) {
				child.add(p1[i]);
			} else {
				child.add(p2[i]);
			}
		}
		
		double[] result = new double[child.size()];
		
		for (int i = 0; i < child.size(); i ++) {
			result[i] = child.get(i).doubleValue();
		}
		
		return new Gene(result);
	}
	
	public void createNewPop() {
		assert(popSize == population.size());
		//number of elites to carry to next gen
        int elites = (int) (popSize*.1);
        ArrayList<Gene> newPop = new ArrayList<Gene>();

        //sort population based on fitness (high to low)
        Collections.sort(population);
        
        //copy over elites into the next generation
        for (int i = 0; i < elites; i++) {
        	newPop.add(population.get(i));
        }
        
        //perform crossover on tourney winners until next gen is full (constant pop size)
        for (int i = 0; i < popSize - elites; i++) {
        	Random rand = new Random();
        	int[] parentsIndex = {-1, -1};
        	
        	//tourney for each parent
        	for (int j = 0; j < parentsIndex.length; j++) {
        		int min = 1000000000;
        		for (int k = 0; k < tournSize; k++) {
        			int cur = rand.nextInt(popSize);
        			if (cur < min) min = cur;
        		}
        		parentsIndex[j] = min;
        	}
        	
        	newPop.add(crossover(population.get(parentsIndex[0]), population.get(parentsIndex[1])));
        }
        
        population = newPop;
	}
	
	public void mutatePop() {
		assert(popSize == population.size());
		
		for (int i = 0; i < popSize; i ++) {
			assert(population.get(i).fitness != -1);
			population.get(i).mutate(mutRate);
		}
	}
}
