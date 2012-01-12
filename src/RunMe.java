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

import dk.aau.cs.model.NTA.Edge;
import dk.aau.cs.model.NTA.Location;
import dk.aau.cs.model.NTA.NTA;
import dk.aau.cs.model.NTA.TimedAutomaton;


public class RunMe {


	static ArrayList<String> systemdecl = new ArrayList<String>();

	/**
	 * @param args
	 * @return 
	 */

	public static void appendGlobalDeclarations(NTA nta, String decl){
		nta.setGlobalDeclarations(nta.getGlobalDeclarations() + "\n" + decl);
	}

	
	static int factor = 1;
	
	public static void main(String[] args) throws Exception {


		String strLine;
		BufferedReader br=null;
		
		FileInputStream fstream = new FileInputStream("/home/kyrke/example-taskgraph.tg");
		//FileInputStream fstream = new FileInputStream("/home/kyrke/download/50/rand0000.stg");
		DataInputStream in = new DataInputStream(fstream);
		br = new BufferedReader(new InputStreamReader(in));



		System.out.println("Task Graph");


		System.out.println("");
		System.out.println("===================================");
		System.out.println("");

		NTA nta = new NTA();

		/* Create processor chans */
		
		appendGlobalDeclarations(nta, "chan take;");
		appendGlobalDeclarations(nta, "chan done;");

		/* Create process automata */ 
		TimedAutomaton processor = new TimedAutomaton();
		processor.setName("Processor1");
		systemdecl.add("Processor1");

		Location idle = new Location("idle", "");
		Location work = new Location("work", "");
		Location work2 = new Location("work2", "");

		processor.addLocation(idle);
		processor.addLocation(work);
		processor.addLocation(work2);

		//appendGlobalDeclarations(nta, "clock p;");
		//Edge e1 = new Edge(idle, work, "p == 0", "take!", "p = " + (maxconstant*factor) );
		//Edge e2 = new Edge(work, idle, "", "done?", "p = 0");
		
		Edge e1 = new Edge(idle, work, "", "take!", "" );
		Edge e2 = new Edge(work, idle, "", "done?", "");

		processor.addTransition(e1);
		processor.addTransition(e2);
		
		Edge e3 = new Edge(work, work2, "", "take!", "");
		Edge e4 = new Edge(work2, work, "", "done?", "");

		processor.addTransition(e3);
		processor.addTransition(e4);

		nta.addTimedAutomaton(processor);


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
			createTask(nta, id, time, predecessors);

		}

		Location start = new Location("start", "");
		processor.setInitLocation(start);

		String reset = "";
		/*for (int i=1; i < numberOfTasks; i++){
			reset += "x"+i + " = " + ((maxconstant*factor)) +", ";
		}
		reset = reset.replaceAll(", $", "");*/
		
		//reset = "x1 == " + ((maxconstant*factor));

		//Edge startEdge = new Edge(start, idle, reset, "", "p = 0");
		Edge startEdge = new Edge(start, idle, reset, "", "");
		
		processor.addLocation(start);
		processor.addTransition(startEdge);

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
		appendGlobalDeclarations(nta, "clock x"+ id +" ;");

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
		Edge e3 = new Edge(waiting, running, "", "take?", "x"+id +" = 0");
		//int reset = (maxconstant*factor);
		Edge e4 = new Edge(running, done, "x"+id+" == "+time*factor, "done!", "");
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
