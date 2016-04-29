package grah8384;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import spacesettlers.simulator.SimulatorException;

public class EvolutionFramework {
	final static int popSize = 30;
	final static int tournSize = 5;
	final static double mutRate = .1;
	
	
	RunGA executor;
	
	ArrayList<Chromosome> population;
	Chromosome maxFit;
	
	public EvolutionFramework(RunGA executor) throws SimulatorException {
		this.executor = executor;
		initializePopulation();
		evolve(1, 4000);
	}
	
	private double rand(double lo, double hi, Random r){
		return r.nextDouble()*(hi-lo)+lo;
	}
	
	//@SuppressWarnings("unchecked")
	private void initializePopulation(){
		  try
	        {
	            FileInputStream fis = new FileInputStream("GenPopulation");
	            ObjectInputStream ois = new ObjectInputStream(fis);
	            population = (ArrayList<Chromosome>) ois.readObject();
	            ois.close();
	            fis.close();
	         }catch(IOException ioe){
	             ioe.printStackTrace();
	             return;
	          }catch(ClassNotFoundException c){
	             System.out.println("Class not found");
	             c.printStackTrace();
	             return;
	          }
		/*
		population = new ArrayList<Gene>();
		Random rand = new Random();
		//Create random population
				for (int i = 0; i < popSize; i++ ){
					
					double shipEnergyBoundary = rand(0,5000,rand);
					double asteroidCostBoundary = rand(10000,50000,rand);// ((rand.nextGaussian() * 500000) + 1500000); 			//Need to change mean and sd
					double resourceBoundary =  rand(0,5000,rand);//rand(0,5000,rand);
					double homeCostBoundary =  rand(5000,30000,rand);//((rand.nextGaussian() * 250) + 500); 			//Need to change mean and sd
					
					//Need to play with mean and sd
					double resourcesAroundWeight =  rand(0,5000,rand); //((rand.nextGaussian() * 10000) + 10000);
					double asteroidsAroundWeight =  rand(0,5000,rand);//((rand.nextGaussian() * 10000) + 10000);
					double distanceWeight =  rand(0,5000,rand);//((rand.nextGaussian() * 10000) + 10000); 
					double resourceOrEnergyWeight =  rand(0,5000,rand);//((rand.nextGaussian() * 10000) + 10000);
					double momentumDetrimentWeight =  rand(0,5000,rand);//((rand.nextGaussian() * 10000) + 10000);	
					
					
					double[] current = {shipEnergyBoundary, 
										asteroidCostBoundary, 
										resourceBoundary, 
										homeCostBoundary, 
					                    resourcesAroundWeight, 
					                    asteroidsAroundWeight, 
					                    distanceWeight, 
					                    resourceOrEnergyWeight, 
					                    momentumDetrimentWeight};
					
					
					
					population.add(new Gene(current));
				}*/
	}
	
	public void evolve(int maxGen, int maxSteps) throws SimulatorException {
		//evaluateFitness(maxSteps);
		report(0);
		
		for (int i = 1; i < maxGen+1; i++) {
			createNewPop();
			mutatePop();
			evaluateFitness(maxSteps);
			report(i);
		}
	}
	
	private void evaluateFitness(int maxSteps) throws SimulatorException {
		for (int i = 0; i < population.size(); i ++) {
			System.out.println(i+1);
			Chromosome cur = population.get(i);
			cur.fitness = (new GeneticSimulator(executor.config, maxSteps, cur.weights)).run();
			GeneticSimulator.mySleep(4000);
		}
	}

	public void report(int gen) {
		assert(population.size() == popSize);
		System.out.println("************** Generation "+gen+" **************");
		Collections.sort(population);
		
		double fitnessSum = 0;
		for (int i = 0; i < popSize; i++) {
			Chromosome cur = population.get(i);
			
			fitnessSum += cur.fitness;
			String w ="[";
			for(int j=0; j< cur.weights.length-1; j+=1){
				w+=cur.weights[j]+", ";
			} 	w+=cur.weights[cur.weights.length-1]+"]";
			
			System.out.println(w + " Fitness: "  + cur.fitness);
		}
		
		System.out.println("Average fitness: " + fitnessSum/population.size() + "\n\n\n");
		
		try{
	         FileOutputStream fos= new FileOutputStream("GenPopulation");
	         ObjectOutputStream oos= new ObjectOutputStream(fos);
	         oos.writeObject(population);
	         oos.close();
	         fos.close();
	       }catch(IOException ioe){
	            ioe.printStackTrace();
	        }
	}
	
	public Chromosome crossover(Chromosome parent1, Chromosome parent2) {
		
		double[] p1 = parent1.weights;
		double[] p2 = parent2.weights;
		
		assert(p1.length == p2.length);
		
		ArrayList<Double> child = new ArrayList<Double>();
		Random rand = new Random();
		
		for (int i = 0; i < p1.length; i++){
			/*if (rand.nextDouble() < .5) {
				child.add(p1[i]);
			} else {
				child.add(p2[i]);
			}*/
			child.add(.5*(p1[i]+p2[i]));
		}
		
		double[] result = new double[child.size()];
		
		for (int i = 0; i < child.size(); i ++) {
			result[i] = child.get(i).doubleValue();
		}
		
		return new Chromosome(result);
	}
	
	public void createNewPop() {
		assert(popSize == population.size());
		//number of elites to carry to next gen
        int elites = (int) (popSize*.1);
        ArrayList<Chromosome> newPop = new ArrayList<Chromosome>();

        //sort population based on fitness (high to low)
        Collections.sort(population);
        maxFit=population.get(0);
        
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
			population.get(i).mutate(mutRate, maxFit);
		}
	}
}