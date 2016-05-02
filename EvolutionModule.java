package grah8384;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.StarGraphics;
import spacesettlers.objects.Base;
import spacesettlers.simulator.SimulatorException;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class EvolutionModule {
	
	final static int popSize = 30;
	final static int tournSize = 5;
	final static double mutRate = 1;
	
	final static double delta = .01;
	
	boolean stop = false;
	
	Toroidal2DPhysics space;
	ArrayList<Genome> population;
	Set<Base> bases;
	
	public EvolutionModule(Toroidal2DPhysics space, Set<Base> bases){
		this.bases = bases;
		this.space = space;
		initializePopulation();
		evolve(50);
	}
	
	private double rand(double lo, double hi, Random r){
		return r.nextDouble()*(hi-lo)+lo;
	}
	
	//@SuppressWarnings("unchecked")
	private void initializePopulation(){
		 /* try
	        {
	            FileInputStream fis = new FileInputStream("GenPopulation");
	            ObjectInputStream ois = new ObjectInputStream(fis);
	            population = (ArrayList<Genome>) ois.readObject();
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
		*/
		population = new ArrayList<Genome>();
		Random seed = new Random();
		double height = space.getHeight();
		double width = space.getWidth();
		
		for (int i = 0; i < popSize; i++ ){
			population.add(
				new Genome(
						rand(0, width, seed),
						rand(0, height, seed)
				)
			);
		}
	}
	
	public void evolve(int maxGen) {
		evaluateFitness();
		report(0);
		
		for (int i = 1; i < maxGen+1; i++) {
			createNewPop();
			mutatePop();
			evaluateFitness();
			report(i);
			
			if(stop)
				break;
		}
	}
	
	private void evaluateFitness() {
		for (int i = 0; i < population.size(); i ++) {
			Genome cur = population.get(i);
			cur.fitness = 0;
			Position curPos = new Position(cur.x, cur.y);
			
			double avgD = 0;
			for(Base base : bases){
				avgD += space.findShortestDistance(base.getPosition(), curPos);
			}
			avgD/=bases.size();
			cur.fitness = avgD;
			
			
			
			//for(Base base : bases){
			//	cur.fitness += Math.pow(space.findShortestDistance(base.getPosition(), curPos)-avgD, 2);
			//}

		}
	}

	public void report(int gen) {
		//assert(population.size() == popSize);
		//System.out.println("************** Genomeration "+gen+" **************");
		Collections.sort(population);
		
		double fitnessSum = 0;
		for (int i = 0; i < popSize; i++) {
			Genome cur = population.get(i);
			
			fitnessSum += cur.fitness;
			String u ="<"+cur.x+", "+cur.y+">";
			//System.out.println(u + " Fitness: "  + cur.fitness);
		}
		
		//System.out.println("Average fitness: " + fitnessSum/population.size() + "\n\n\n");
		for(Base b : bases){
			//System.out.println("<"+b.getPosition().getX()+", "+b.getPosition().getY()+">");
		}
		
		if(population.get(0).fitness -  fitnessSum/population.size() < delta){
			stop = true;
		}
		
		/*try{
	         FileOutputStream fos= new FileOutputStream("GenPopulation");
	         ObjectOutputStream oos= new ObjectOutputStream(fos);
	         oos.writeObject(population);
	         oos.close();
	         fos.close();
	       }catch(IOException ioe){
	            ioe.printStackTrace();
	        }*/
	}
	
	public Genome crossover(Genome parent1, Genome parent2) {
		return new Genome(
			.5*(parent1.x + parent2.x), 
			.5*(parent1.y + parent2.y)
		);
	}
	
	public void createNewPop() {
		//number of elites to carry to next gen
        int elites = (int) (popSize*.1);
        ArrayList<Genome> newPop = new ArrayList<Genome>();
        
        //sort population based on fitness (high to low)
        Collections.sort(population);
        Genome.maxFit = population.get(0);
        
        //copy over elites into the next Genomeration
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
		for (int i = 0; i < popSize; i ++) {
			population.get(i).mutate(mutRate, space);
		}
	}
	
	public ArrayList<SpacewarGraphics> getGraphics(){
		ArrayList<SpacewarGraphics> result = new ArrayList<SpacewarGraphics>();
		for(Base b : bases)
			result.add(new StarGraphics(new Color(255, 255, 255), b.getPosition()));
		result.add(new StarGraphics(new Color(255, 255, 255), new Position(Genome.maxFit.x, Genome.maxFit.y)));
		
		return result;
	}
}