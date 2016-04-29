package grah8384;
import spacesettlers.clients.Team;
import spacesettlers.simulator.SimulatorException;
import spacesettlers.simulator.SpaceSettlersSimulator;
import spacesettlers.simulator.Toroidal2DPhysics;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

public class RunGA {
	JSAPResult config;

	/**
	 * Create a simulator with the command line arguments parsed.  If they fail to parse, this will die 
	 * before moving on.
	 * @param args
	 * @throws SimulatorException 
	 */
	public RunGA(String[] args) throws SimulatorException {
		SimpleJSAP parser = initializeParser(args);
		config = parseArgs(args, parser);
		verifyArguments();
	}
	
	/**
	 * Initialize the parser with all of the command line arguments
	 * @param args
	 * @return
	 */
	private SimpleJSAP initializeParser(String[] args) {
		SimpleJSAP parser = null;

		// create the parser and specify all the command line arguments
		try {
			parser = new SimpleJSAP(
					"Simulator",
					"Spacewar simulator",
					new Parameter[] {
						new FlaggedOption("simulatorConfigFile",
								JSAP.STRING_PARSER, null, JSAP.REQUIRED,
								JSAP.NO_SHORTFLAG, "simulatorConfigFile",
								"configuration file for the simulator (xml)"), 
						new FlaggedOption("ladderConfigFile",
								JSAP.STRING_PARSER, null, JSAP.REQUIRED,
								JSAP.NO_SHORTFLAG, "ladderConfigFile",
								"configuration file for the simulator (xml)"), 
						new FlaggedOption("configPath",
								JSAP.STRING_PARSER, null, JSAP.REQUIRED,
								JSAP.NO_SHORTFLAG, "configPath",
								"path to the configuration files"), 
						new FlaggedOption("graphics",
								JSAP.BOOLEAN_PARSER, "false", JSAP.REQUIRED,
								JSAP.NO_SHORTFLAG, "graphics",
								"boolean stating if graphics are to be shown (true) or not (false)"),
						new FlaggedOption("debug",
								JSAP.BOOLEAN_PARSER, "false", JSAP.REQUIRED,
								JSAP.NO_SHORTFLAG, "debug",
								"boolean stating if debugging mode (single threading) is on (true) or not (false)"),
					});
		} catch (JSAPException e) {
			e.printStackTrace();
			System.err.println("Error in the parser - exiting");
			System.exit(-1);
		}
		return parser;
	}

	/**
	 * Actually parse the command line arguments
	 * @param args
	 * @param parser
	 * @return
	 */
	private JSAPResult parseArgs(String[] args, SimpleJSAP parser) {
		// parse the arguments
		JSAPResult parserConfig = parser.parse(args);
		// exit if it failed to parse
		if (parser.messagePrinted()) {
			System.exit(-1);
		}
		return parserConfig;
	}
	

	/**
	 * Verify that the arguments are valid.  
	 */
	private void verifyArguments() throws SimulatorException {
		String configName = config.getString("simulatorConfigFile");
		
		int xmlIndex = configName.indexOf("xml");
		// verify that the argument ended in .xml
		if (xmlIndex != (configName.length() - 3)) {
			throw new SimulatorException("Error: invalid simulator config file name " + configName + " It must end in .xml");
		}
		
	}
	

	/**
	 * Run the spacewar simulator.  First parse and verify the command line arguments and then run the sim.
	 * 
	 * @param args
	 * @throws SimulatorException 
	 */
	public static void main(String[] args) throws SimulatorException {
		RunGA runSim = new RunGA(args);
	
		//GeneticSimulator simulator;
		System.out.println("-----------");
		
		EvolutionFramework GA = new EvolutionFramework(runSim);
		
		/*for(Team t : simulator.getTeams()){
			System.out.println(t.getLadderName());
		}
		
		int maxTimeSteps = 5000;
		double sum = 0;
		int maxGen = 3;
		for(int i = 0; i < maxGen; i+=1){
			simulator = new GeneticSimulator(runSim.config, maxTimeSteps, 2000, 500);
			sum+=simulator.run();
		}
		System.out.println(sum/maxGen);*/
	}



}