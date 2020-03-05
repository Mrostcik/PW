package multiplicator;

import java.util.Collection;

import petrinet.PetriNet;
import petrinet.Transition;

public class MultiplicatingThread implements Runnable {

	private PetriNet<String> petrinet;
	private Collection<Transition<String>> transitions;
	private Integer fired;
	
	public MultiplicatingThread(PetriNet<String> petrinet, Collection<Transition<String>> transitions) {
		this.petrinet = petrinet;
		this.transitions = transitions;
		fired = 0;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				petrinet.fire(transitions);
				fired++;
			}
			catch(InterruptedException e) {
				System.out.println("Odpaliłem " + fired.toString() + " przejść.");
				break;
			}
			
		}

	}

}
