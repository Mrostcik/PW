package alternator;
import java.util.Collection;

import petrinet.*;

public class AlternatingThread implements Runnable {
	private String name;
	private PetriNet<String> petrinet;
	private Collection<Transition<String>> transitions;
	
	public AlternatingThread(String name, PetriNet<String> petrinet, Collection<Transition<String>> transitions) {
		this.name = name;
		this.petrinet = petrinet;
		this.transitions = transitions;
	}

	@Override
	public void run() {
		while(true) {
			try {
				petrinet.fire(transitions);
				System.out.print(name);
				System.out.print(".");
				petrinet.fire(transitions);
			}
			catch(InterruptedException e) {break;}
			
		}
	}

}
