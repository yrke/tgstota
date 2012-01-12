import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.ObjectInputStream.GetField;
import java.util.*;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import pipe.dataLayer.Template;

import dk.aau.cs.model.NTA.Edge;
import dk.aau.cs.model.NTA.Location;
import dk.aau.cs.model.NTA.NTA;
import dk.aau.cs.model.NTA.TimedAutomaton;


public class TASKGraphNoMax {


	static ArrayList<String> systemdecl = new ArrayList<String>();

	/**
	 * @param args
	 * @return 
	 */

	public static void appendGlobalDeclarations(NTA nta, String decl){
		nta.setGlobalDeclarations(nta.getGlobalDeclarations() + "\n" + decl);
	}


	
	public static void main(String[] args) throws Exception {


		String strLine;
		BufferedReader br=null;
		
		//FileInputStream fstream = new FileInputStream("/home/kyrke/example-taskgraph.tg");
		//FileInputStream fstream = new FileInputStream("/home/kyrke/download/50/rand0000.stg");
		//FileInputStream fstream = new FileInputStream("/home/kyrke/a.stg");
		FileInputStream fstream = new FileInputStream("/home/kyrke/f.tg");
		DataInputStream in = new DataInputStream(fstream);
		br = new BufferedReader(new InputStreamReader(in));



		System.out.println("Task Graph");


		System.out.println("");
		System.out.println("===================================");
		System.out.println("");

		NTA nta = new NTA();

		/* Create processor chans */
		
		//appendGlobalDeclarations(nta, "chan take;");
		//appendGlobalDeclarations(nta, "chan done;");

		/* Create process automata */ 
		
		int numproc = 2;
		
		ArrayList<TimedAutomaton> processors = new ArrayList<TimedAutomaton>();
		
		for( int i = 1; i <= numproc; i++){
			TimedAutomaton processor = new TimedAutomaton();
			processor.setName("Processor"+i);
			systemdecl.add("Processor"+i);

			Location idle = new Location("idle", "");

			processor.addLocation(idle);
			processor.setInitLocation(idle);

			appendGlobalDeclarations(nta, "clock p"+i+";");

			nta.addTimedAutomaton(processor);
			
			processors.add(processor);
		}


		boolean parseTasks = false;
		int numberOfTasks = -1;

		while ((strLine = br.readLine()) != null)   {

			if (strLine.startsWith("#")){
				continue;
			}

			if (!parseTasks){

				numberOfTasks = Integer.parseInt(strLine.trim());
				br.readLine(); // Skip line with start task
				parseTasks = true;
				continue;

			}

			//Parsing a task 

			String[] task = strLine.split("\\s+"); 

			/*strLine = strLine.replaceFirst("\\s+", "");*/

			int id = Integer.parseInt(task[1].trim());
			int time = Integer.parseInt(task[2].trim());


			ArrayList<Integer> predecessors = new ArrayList<Integer>();

			if (Integer.parseInt(task[4]) != 0){
				for (int i=4; i < task.length; i++){
					predecessors.add(Integer.parseInt(task[i]));
				}
			}	
			System.out.println(strLine);
			System.out.println("id: " +id+ " : "+predecessors);
			
			Location workTask = new Location("Work"+id, "");
			
			int j = 1;
			for (TimedAutomaton processor : processors) {
				processor.addLocation(workTask);

				processor.addTransition(new Edge(processor.getInitLocation(), workTask, "p"+j+" == 0", "take"+id+"?", "p"+j+" = 0"));
				processor.addTransition(new Edge(workTask, processor.getInitLocation(), "p"+j+" == " + time, "done"+id+"!", "p"+j+" = 0"));
				j += 1;
			}
			
			appendGlobalDeclarations(nta, "chan take"+id+";");
			appendGlobalDeclarations(nta, "chan done"+id+";");
			
			createTask(nta, id, time, predecessors);

		}

		

		nta.setSystemDeclarations("system " + Join(systemdecl, ",") + ";");

		nta.outputToUPPAALXML(new PrintStream("/tmp/abemad.xml"));


		fstream.close();
		in.close();
		br.close();


	}


	public static void  createTask(NTA nta, int id, int time, ArrayList<Integer> predescessors){

		/* Create the task */

		/* Declarations */
		appendGlobalDeclarations(nta, "chan done_t" + id +";");
		//appendGlobalDeclarations(nta, "clock x"+ id +" ;");

		/* TA */

		TimedAutomaton task = new TimedAutomaton();
		task.setName("t"+id);
		systemdecl.add("t"+id);

		Location waiting = new Location("waiting","");
		Location running = new Location("running", "");
		Location done = new Location("l_done", "");

		task.addLocation(waiting);
		task.addLocation(running);
		task.addLocation(done);

		if (predescessors.size() == 0){
			task.setInitLocation(waiting);
		} else {

			Location last = waiting;

			for (int i : predescessors){

				Location tmp = new Location("l_waiting_"+id+"_"+i, "");
				Edge e = new Edge(tmp, last, "", "done_t"+i+"!", "");

				task.addLocation(tmp);
				task.addTransition(e);

				last = tmp;

			}

			task.setInitLocation(last);

		}

		// Add pre edges
		Edge e3 = new Edge(waiting, running, "", "take"+id+"!","");
		Edge e4 = new Edge(running, done, "", "done"+id+"?", "");
		Edge e5 = new Edge(done, done, "", "done_t" + id+"?", "");

		task.addTransition(e3);
		task.addTransition(e4);
		task.addTransition(e5);

		nta.addTimedAutomaton(task);

	}




	public static String Join(ArrayList<String> coll, String delimiter)
	{
		if (coll.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		for (String x : coll)
			sb.append(x + delimiter);

		sb.delete(sb.length()-delimiter.length(), sb.length());

		return sb.toString();


	}
}
