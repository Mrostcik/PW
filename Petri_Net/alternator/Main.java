package alternator;
import petrinet.*;

import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

//Tworze szesc miejsc i szesc przejsc, kazde przejscie jest polaczone z 2 sasiadujacymi
//miejscami. Kazdemu z 3 watkow odpowiadaja 2 przejscia. Dzieki temu, ze na poczatku
//mozliwe do odpalenia jest tylko 1 przejscie, a kazde przejscie umozliwia odpalenie
//prawemu sasiadowi wsrod przejsc, bedziemy mieli wzajemne wykluczanie.
public class Main {
	public static void main(String[] args) {
		Map<String, Integer> places = new HashMap<String, Integer>();
		places.put("A1", 1);
		places.put("A2", 0);
		places.put("B1", 0);
		places.put("B2", 0);
		places.put("C1", 0);
		places.put("C2", 0);
		
		Map<String, Integer> input1 = new HashMap<String, Integer>();
		input1.put("A1", 1);
		Map<String, Integer> input4 = new HashMap<String, Integer>();
		input4.put("A2", 1);
		Map<String, Integer> input2 = new HashMap<String, Integer>();
		input2.put("B1", 1);
		Map<String, Integer> input5 = new HashMap<String, Integer>();
		input5.put("B2", 1);
		Map<String, Integer> input3 = new HashMap<String, Integer>();
		input3.put("C1", 1);
		Map<String, Integer> input6 = new HashMap<String, Integer>();
		input6.put("C2", 1);
		
		Map<String, Integer> output1 = new HashMap<String, Integer>();
		output1.put("A2", 1);
		Map<String, Integer> output4 = new HashMap<String, Integer>();
		output4.put("B1", 1);
		Map<String, Integer> output2 = new HashMap<String, Integer>();
		output2.put("B2", 1);
		Map<String, Integer> output5 = new HashMap<String, Integer>();
		output5.put("C1", 1);
		Map<String, Integer> output3 = new HashMap<String, Integer>();
		output3.put("C2", 1);
		Map<String, Integer> output6 = new HashMap<String, Integer>();
		output6.put("A1", 1);
		
		Collection<String> reset = new HashSet<String>();
		Collection<String> inhibitor = new HashSet<String>();
		
		Transition<String> transition1 = new Transition<String>(input1, reset, inhibitor, output1);
		Transition<String> transition2 = new Transition<String>(input2, reset, inhibitor, output2);
		Transition<String> transition3 = new Transition<String>(input3, reset, inhibitor, output3);
		Transition<String> transition4 = new Transition<String>(input4, reset, inhibitor, output4);
		Transition<String> transition5 = new Transition<String>(input5, reset, inhibitor, output5);
		Transition<String> transition6 = new Transition<String>(input6, reset, inhibitor, output6);
		PetriNet<String> petrinet = new PetriNet<String>(places, true);
		
		Collection<Transition<String>> transAll = new HashSet<Transition<String>>();
		transAll.add(transition1);
		transAll.add(transition2);
		transAll.add(transition3);
		transAll.add(transition4);
		transAll.add(transition5);
		transAll.add(transition6);
		Set<Map<String, Integer>> sss = petrinet.reachable(transAll);
		System.out.println(sss.size());
		
		boolean safe = true;
        int suma = 0;
		for(Map<String, Integer> x: sss) {
            suma = 0;
			for(String y: x.keySet()) {
                suma += x.get(y);
			}
            if(suma > 1)
                safe = false;
		}
		
		if(safe)
			System.out.println("WARUNEK BEZPIECZENSTWA DLA ZNAKOWAN SPELNIONY");
		
		Collection<Transition<String>> transA = new HashSet<Transition<String>>();
		transA.add(transition1);
		transA.add(transition4);
		Collection<Transition<String>> transB = new HashSet<Transition<String>>();
		transB.add(transition2);
		transB.add(transition5);
		Collection<Transition<String>> transC = new HashSet<Transition<String>>();
		transC.add(transition3);
		transC.add(transition6);
		
		Thread threads[] = new Thread[3];
		threads[0] = new Thread(new AlternatingThread("A", petrinet, transA));
		threads[1] = new Thread(new AlternatingThread("B", petrinet, transB));
		threads[2] = new Thread(new AlternatingThread("C", petrinet, transC));
		
		threads[0].start();
		threads[1].start();
		threads[2].start();
		
		try {
			Thread.sleep(30000);
		}
		catch(InterruptedException e) {};
		
		threads[0].interrupt();
		threads[1].interrupt();
		threads[2].interrupt();	
	}
}
