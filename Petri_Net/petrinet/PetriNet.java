package petrinet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Iterator;

public class PetriNet<T> {
	@SuppressWarnings("unused")
	private boolean fair;
	private volatile Map<T, Integer> places;
	//pary (semafor, przejscie) odpowiadajace wznawianemu watkowi
	private volatile PairST<T> pairs;
	private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock(true);
	private Semaphore continued = new Semaphore(1, true);
	private Semaphore continued2 = new Semaphore(1, true);

    public PetriNet(Map<T, Integer> initial, boolean fair) {
    	pairs = new PairST<T>();
    	Map<T, Integer> map = new HashMap<T, Integer>();
    	map.putAll(initial);
    	places = map;
    	this.fair = fair;
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
    	//wpuszcza watki jesli fire nie dziala
    	rwlock.readLock().lock();
    	
    	ArrayList<Map<T, Integer>> set = new ArrayList<Map<T, Integer>>();
    	Map<T, Integer> placesCopy = new HashMap<T, Integer>();
    	placesCopy.putAll(places);
    	set.add(placesCopy);
    	Set<Map<T, Integer>> set2 = new HashSet<Map<T, Integer>>();
    	Iterator<Map<T, Integer>> value = set.iterator();
    	
    	//Tworzymy set2 mozliwych znakowan, dopoki w set sa znakowania,
    	//na ktorych mozemy odpalic przejscia.
    	//BFS
    	while(set.size() > 0) {
			Map<T, Integer> p = value.next();
			Map<T, Integer> pCopy = new HashMap<T, Integer>();
			pCopy.putAll(p);
			RemoveZeros(pCopy);
			set2.add(pCopy);
    		for(Transition<T> x: transitions) {
    			if(canBeFired(p, x)) {
    				Map<T, Integer> p2 = fireExecutor(p, x);
					Map<T, Integer> p2Copy = new HashMap<T, Integer>();
					p2Copy.putAll(p2);
					RemoveZeros(p2Copy);
    				if(!set2.contains(p2Copy)) {
    					set.add(p2);
    					set2.add(p2Copy);
    				}
    			}
    		}
    		set.remove(p);    		
        	value = set.iterator();
    	}
    	
    	rwlock.readLock().unlock();
    	return set2;
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
    	//wpuszcza jeden watek
    	rwlock.writeLock().lockInterruptibly();
    	//czeka az wznowione watki wykonaja fire
    	try {
    		continued.acquire();
    	}
    	catch(InterruptedException e) {
    		rwlock.writeLock().unlock();
    		throw new InterruptedException();
    	}
    	continued.release();
    	
    	Transition<T> toFire = null;
    	for(Transition<T> t: transitions) {
    		if(canBeFired(places, t)) {
    			toFire = t;
    			break;
    		}		
    	}
    	
    	//jesli watek nie ma przejscia do wykonania
    	if(toFire == null) {
    		//Wrzucamy do pairs semafor i kolekcje przejsc dla 
    		//danego watku i czekamy na tym semaforze.
    		Semaphore sem = new Semaphore(0);
    		pairs.add(sem, transitions);
    		//Wpuszczamy inny watek bo ten czeka.
    		rwlock.writeLock().unlock();
    		sem.acquire();
        	for(Transition<T> t: transitions) {
        		if(canBeFired(places, t)) {
        			toFire = t;
        			break;
        		}		
        	}
        	places = fireExecutor(places, toFire);
        	//Wznawiamy czekajacy watek, jesli mozliwe
        	boolean causedFire = false;
    		for(int i = 0; i < pairs.getSize(); i++) {
    			Collection<Transition<T>> x = pairs.getSecond(i);
    			for(Transition<T> t: x) {
    				if(canBeFired(places, t)) {
    					Semaphore semaphore = pairs.getFirst(i);
    					pairs.remove(i);
    					causedFire = true;
    					semaphore.release();
    					break;
    				}
    			}
    			if(causedFire)
    				break;
    		}
    		//Jezeli watek nic nie wznowil
    		if(causedFire == false) {
    			//zwalniamy semafor odpowiadajacy watkowi, ktory nigdy nie byl watkiem
    			//czekajacym na wznowienie, ale wznowil inny watek
    			//Musi on czekac na wykonanie fire przez wszystkie watki, ktore
    			//wznawialy sie lancuchowo.
    			continued2.release();
    			//zwalniamy semafor, ktory oznacza ze dziala jakikolwiek watek wznowiony
        		continued.release();
    		}
    	}
    	//watek, ktory nie czekal
    	else {
    		places = fireExecutor(places, toFire);
    		boolean done = false;
    		//wznawiamy mozliwie 1 watek
    		for(int i = 0; i < pairs.getSize(); i++) {
    			Collection<Transition<T>> x = pairs.getSecond(i);
    			for(Transition<T> t: x) {
    				if(canBeFired(places, t)) {
    					Semaphore semaphore = pairs.getFirst(i);
    					pairs.remove(i);

    					//Watek blokuje semafor watkow wznowionych, dzieki
    					//czemu zaden nowy nie bedzie w tym samym czasie robil fire
    					try {
    						continued.acquire();
    					}
    					catch(InterruptedException e) {
    						rwlock.writeLock().unlock();
    						throw new InterruptedException();
    					}
    					
   						//budzimy wznawiany watek
    					semaphore.release();
    					done = true;
    					//czekamy az lancuch wznawianych watkow skonczy fireowac
    					try {
    						continued2.acquire();
    					}
    					catch(InterruptedException e) {
    						rwlock.writeLock().unlock();
    						throw new InterruptedException();
    					}
    					break;
    				}
    			}
    			if(done)
    				break;
    		}
    		
        	rwlock.writeLock().unlock();	
    	}
    	
    	return toFire;	
    }

    public boolean canBeFired(Map<T, Integer> placess, Transition<T> transition) {
		Map<T, Integer> map = transition.getInput();
		Collection<T> inhib = transition.getInhibitor();
    	
    	for(T x : map.keySet()) {
    		Integer y = map.get(x);
    		if(!placess.containsKey(x))
    			return false;
    		if(placess.get(x) < y)
    			return false;
    	}
    	
    	for(T x: inhib) {
    		if(placess.containsKey(x) &&  placess.get(x) > 0)
    			return false;		
    	}
    	return true;
    }
    
    private Map<T, Integer> fireExecutor(Map<T, Integer> placess, Transition<T> transition) {
		Map<T, Integer> map = transition.getInput();
		Collection<T> reset = transition.getReset();
		Map<T, Integer> output = transition.getOutput();
		Map<T, Integer> places2 = new HashMap<T, Integer>();
		places2.putAll(placess);
		
    	for(T x : places2.keySet()) {
    		if(map.containsKey(x))
    			places2.put(x, places2.get(x) - map.get(x));
    		if(reset.contains(x))
    			places2.put(x, 0); 		
    	}
    	
    	for(T x: output.keySet()) {
    		if(places2.containsKey(x))
    			places2.put(x, places2.get(x) + output.get(x));
    		else
    			places2.put(x, output.get(x));
    	}
    
    	return places2;
    }
    
    //Usuwa miejsca o 0 zetonach z mapy.
    private void RemoveZeros(Map<T, Integer> m){
        for(Iterator<Map.Entry<T, Integer>> it = m.entrySet().iterator(); it.hasNext(); ) {
        	Map.Entry<T, Integer> entry = it.next();
        	if(entry.getValue() == 0) {
        		it.remove();
        	}
        }    
    }
    
}
