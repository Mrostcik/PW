package petrinet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;

//Klasa trzymajaca dla kazdego watku czekajacego na wznowienie w fire
//odpowiadajacy mu semafor oraz odpowiadajaca mu kolekcje przejsc,
//z ktora byl wywolywany.
public class PairST<T> {
	private volatile ArrayList<Semaphore> available;
	private volatile ArrayList<Collection<Transition<T>>> tran;
	
	public PairST() {
		available = new ArrayList<Semaphore>();
		tran = new ArrayList<Collection<Transition<T>>>();
	}
	
	public Semaphore getFirst(int index) {
		return available.get(index);
	}
	
	public Collection<Transition<T>> getSecond(int index){
		return tran.get(index);
	}
	
	public void add(Semaphore s, Collection<Transition<T>> c) {
		available.add(s);
		tran.add(c);
	}
	
	public int getSize() {
		return available.size();
	}
	
	public void remove(int index) {
		available.remove(index);
		tran.remove(index);
	}
}
