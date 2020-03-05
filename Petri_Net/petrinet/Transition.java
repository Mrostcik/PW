package petrinet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;


public class Transition<T> {

	private Map<T, Integer> input;
	private Collection<T> reset;
	private Collection<T> inhibitor;
	private Map<T, Integer> output;
	
    public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
    	
    	Map<T, Integer> map = new HashMap<T, Integer>();
    	map.putAll(input);
    	this.input = map;
    	
    	Map<T, Integer> map2 = new HashMap<T, Integer>();
    	map2.putAll(output);
    	this.output = map2;
    	
    	Collection<T> set = new HashSet<T>();
    	set.addAll(reset);
    	this.reset = set;
    	
    	Collection<T> set2 = new HashSet<T>();
    	set2.addAll(inhibitor);
    	this.inhibitor = set2;
    	
    }
    
    public Map<T, Integer> getInput() {
    	return input;
    }
    
    public Collection<T> getReset(){
    	return reset;
    }
    
    public Collection<T> getInhibitor(){
    	return inhibitor;
    }
    
    public Map<T, Integer> getOutput(){
    	return output;
    }

}