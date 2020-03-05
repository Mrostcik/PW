package multiplicator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import petrinet.*;

//Tworze 3 miejsca i 3 przejscia. W 1 miejscu jest poczatkowo min(A,B) zetonow,
//w 2 max(A,B) zetonow a w 3 abs(A-B) zetonow. Za pomoca przejscia skladajacego sie z:
//krawedzi wejsciowej o wadze 1 do 1 miejsca, krawedzi wejsciowej o wadze 1 do 2 miejsca,
//krawedzi wyjsciowej o wadze A do 2 miejsca oraz przejscia skladajacego sie z:
//krawedzi wejsciowej o wadze abs(A-B) do 3 miejsca oraz wyjsciowej o wadze abs(A-B) z 2 miejsca
//koncowo w 2 miejscu mam A*B zetonow, a w 1 i 3 po 0 zetonow.
//Przejscie skladajace sie z inhibitorow do miejsc 1 i 3 pozwala mi stwierdzic, ze obliczenie jest
//zakonczone. Odczytuje wynik ze znakowania gdy jest > 0, w przeciwnym wypadku wypisuje 0.
public class Main {

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		int x = scan.nextInt();
		int y = scan.nextInt();
		if(x > y) {
			int c = x;
			x = y;
			y = c;
		}
		Map<String, Integer> places = new HashMap<String, Integer>();
		places.put("A", x);
		places.put("B", y);
		places.put("C", y - x);
		Map<String, Integer> input = new HashMap<String, Integer>();
		input.put("A", 1);
		input.put("B", 1);
		Map<String, Integer> output = new HashMap<String, Integer>();
		output.put("B", y);		
		Collection<String> reset = new HashSet<String>();
		Collection<String> inhibitor = new HashSet<String>();
		
		Map<String, Integer> input2 = new HashMap<String, Integer>();
		input2.put("C", y - x);
		input2.put("B", y - x);
  
		Map<String, Integer> output2 = new HashMap<String, Integer>();
		Collection<String> reset2 = new HashSet<String>();
		Collection<String> inhibitor2 = new HashSet<String>();
		
		Map<String, Integer> input3 = new HashMap<String, Integer>();
		Map<String, Integer> output3 = new HashMap<String, Integer>();
		Collection<String> reset3 = new HashSet<String>();
		Collection<String> inhibitor3 = new HashSet<String>();
		inhibitor3.add("C");
		inhibitor3.add("A");
		
		Transition<String> transition1 = new Transition<String>(input, reset, inhibitor, output);
		Transition<String> transition2 = new Transition<String>(input2, reset2, inhibitor2, output2);
		Transition<String> transition3 = new Transition<String>(input3, reset3, inhibitor3, output3);
		
		Collection<Transition<String>> trans1 = new HashSet<Transition<String>>();
		trans1.add(transition1);
        if(x != y)
		    trans1.add(transition2);
		Collection<Transition<String>> trans2 = new HashSet<Transition<String>>();
		trans2.add(transition3);
		
		PetriNet<String> petrinet = new PetriNet<String>(places, true);
		
		Thread threads[] = new Thread[4];
		threads[0] = new Thread(new MultiplicatingThread(petrinet, trans1));
		threads[1] = new Thread(new MultiplicatingThread(petrinet, trans1));
		threads[2] = new Thread(new MultiplicatingThread(petrinet, trans1));
		threads[3] = new Thread(new MultiplicatingThread(petrinet, trans1));
		
		
		threads[0].start();
		threads[1].start();
		threads[2].start();
		threads[3].start();
		try {
			petrinet.fire(trans2);
		}
		catch(InterruptedException e) {}
		
		Set<Map<String, Integer>> set = petrinet.reachable(new HashSet<Transition<String>>());
		boolean positiveValue = false;
		for(Map<String, Integer> m: set) {
			for(Integer i: m.values()) {
				if(i > 0) {
					System.out.println(i);
					positiveValue = true;
				}
			}
		}
		
		if(!positiveValue)
			System.out.println(0);

		threads[0].interrupt();
		threads[1].interrupt();
		threads[2].interrupt();
		threads[3].interrupt();
		scan.close();
	}

}
