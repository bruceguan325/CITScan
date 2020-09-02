package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * A state represents an element in the Aho-Corasick tree.
 */

public class State {
	static int _STATE_ID_SEED = 0;
	final static int UNSET_ID = -1;
	
	protected final int id;
	private State fail;

	// for deserialization
	protected State(int id) {
		super();
		this.id = id;
	}
	
	public State() {
		super();
		id = ++_STATE_ID_SEED;
	}

	public State extend(char b) {
//		if (get(b) != null) return get(b);
		State nextState = new State();
		
		this.put(b, nextState);
		return nextState;
	}

	public void extendAllAndAddOutput(char[] chars, Output output) {
		State state = this;
		for (int i = 0; i < chars.length; i++) {
			State s = state.get(chars[i]);
			if (s != null) {
				state = s;
			}
			else {
				state = state.extend(chars[i]);
			}
		}
		state.addOutput(output);
	}

	public void extendAllAndAddAllOutputs(char[] chars, Set<Output> outputs) {
		State state = this;
		for (int i = 0; i < chars.length; i++) {
			State s = state.get(chars[i]);
			if (s != null) {
				state = s;
			}
			else {
				state = state.extend(chars[i]);
			}
		}
		state.addAllOutputs(outputs);
	}

	/**
	 * Returns the size of the tree rooted at this State. Note: do not call this
	 * if there are loops in the edgelist graph, such as those introduced by
	 * AhoCorasick.prepare().
	 */
	public int size() {
		char[] keys = keys();
		int result = 1;
		for (int i = 0; i < keys.length; i++)
			result += get(keys[i]).size();
		return result;
	}

	public State getFail() {
		return this.fail;
	}

	public void setFail(State f) {
		this.fail = f;
	}

	// BEGIN STATE MAP
	// This is basically an inlined map of bytes to states.
	// It is very hacky because it is designed to use absolutely
	// as little space as possible. There be great evil here.

	static final char[] EMPTY_CHARACTERS = new char[0];

	// Oh what a hack
	// if the map has size 0 keys is null, states is null
	// if the map has size 1 keys is a Byte, states is a T
	// else keys is byte[] and states is an Object[] of the same size
	// carrying the bytes in parallel
	/*
	HashMap<Character, State> map = new HashMap<Character, State>();
	BitSet bs = new BitSet(128);
	
	public State get(char key) {
		if (bs.get(key)) {
			return map.get(key);
		}
		else return null;
	}
	
	public void put(char key, State value) {
		bs.set(key);
		map.put(key, value);
	}
	
	char[] keys = null;
	public char[] keys() {
		if (keys == null) {
			Set<Character> keySet = map.keySet();
			char[] keys = new char[keySet.size()];
			Iterator<Character> itr = keySet.iterator();
			for (int i=0; i < keySet.size(); i++) {
				keys[i] = itr.next();
			}
			this.keys = keys;
		}
		
		return this.keys;
	}
	 */
	protected Object keys;
	protected Object states;

	public State get(char key) {
		if (keys == null)
			return null;
		if (keys instanceof Character) {
			return ((Character) keys).charValue() == key ? (State) states
					: null;
		}
		char[] keys = (char[]) this.keys;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == key)
				return (State) ((Object[]) states)[i];
		}
		return null;
	}

	public void put(char key, State value) {
		if (keys == null) {
			keys = key;
			states = value;
			return;
		}

		if (keys instanceof Character) {
			if (((Character) keys).charValue() == key) {
				states = value;
			} else {
				keys = new char[] { ((Character) keys).charValue(), key };
				states = new Object[] { states, value };
			}
			return;
		}

		char[] keys = (char[]) this.keys;
		Object[] states = (Object[]) this.states;

		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == key) {
				states[i] = value;
				return;
			}
		}

		char[] newkeys = new char[keys.length + 1];
		for (int i = 0; i < keys.length; i++) {
			newkeys[i] = keys[i];
		}
		newkeys[newkeys.length - 1] = key;

		Object[] newstates = new Object[states.length + 1];
		for (int i = 0; i < states.length; i++) {
			newstates[i] = states[i];
		}
		newstates[newstates.length - 1] = value;

		this.keys = newkeys;
		this.states = newstates;
	}

	public char[] keys() {
		if (keys == null)
			return EMPTY_CHARACTERS;
		if (keys instanceof Character)
			return new char[] { ((Character) keys).charValue() };
		return ((char[]) keys);
	}

	// END STATE MAP

	// BEGIN OUTPUT SET

	// Same story. here's an inlined set of ints backed by an array of
	// ints.

	private static int[] EMPTY_INTS = new int[0];

	// null when empty
	// an Integer when size 1
	// an int[] when size > 1
	private Object outputs;

	void addAllOutputs(State that) {
		/*for (int j : that.getOutputs())
			addOutput(j);
		*/
		if (set == null) {
			set = new HashSet<Output>();
		}
		set.addAll(that.getOutputs());
	}

	void addAllOutputs(Set<Output> toBeAdd) {
		if (set == null) {
			set = new HashSet<Output>();
		}
		
		if (toBeAdd != null)
			for (Output o: toBeAdd)
				set.add(o);
		/*if (outputs == null)
			outputs = toBeAdd;
		else if (outputs instanceof Integer) {
			int output = (Integer)outputs;
			for (int v: toBeAdd) {
				if (v == output) {
					outputs = toBeAdd;
					return;
				}
			}
			
			int[] newoutputs = Arrays.copyOf(toBeAdd, toBeAdd.length + 1);
			newoutputs[newoutputs.length - 1] = output;
			outputs = newoutputs;
		}
		else {
			Set<Integer> hs = new HashSet<Integer>();
			for (int v : (int[])outputs) {
				hs.add(v);
			}
			for (int v : toBeAdd) {
				hs.add(v);
			}
			this.outputs = hs.toArray();
		}*/
	}
	
	Set<Output> set = null;//new HashSet<Integer>();

	void addOutput(Output output) {
		if (set == null) {
			set = new HashSet<Output>();
		}
		else if (set.contains(output)) {
			return;
		}
		set.add(output);
		/*
		if (outputs == null)
			outputs = i;
		else if (outputs instanceof Integer) {
			int j = ((Integer) outputs).intValue();
			if (i != j) {
				outputs = new int[] { j, i };
			}
		} else {
			int[] outputs = (int[]) this.outputs;
			for (int v : outputs) {
				if (v == i)
					return;
			}
			int[] newoutputs = new int[outputs.length + 1];
			System.arraycopy(outputs, 0, newoutputs, 0, outputs.length);
			newoutputs[newoutputs.length - 1] = i;
			this.outputs = newoutputs;
		}*/
	}

	public Set<Output> getOutputs() {
		return set;
		/*if (outputs == null)
			return EMPTY_INTS;
		else if (outputs instanceof Integer)
			return new int[] { ((Integer) outputs).intValue() };
		else
			return (int[]) outputs;*/
	}

	public boolean hasOutput() {
		return set != null && set.size() > 0;
	}

	// END OUTPUT SET
	

	// Serialization
	/*public void writeTo(ByteBuffer bb) {
		//System.out.println("Write State ID:" + id + " at " + bb.position());
		bb.putInt(id);
		
		char[] keys = keys();
		
		//System.out.println("Key size:" + keys.length + " at " + bb.position());
		bb.putInt(keys.length);
		for (int i=0; i < keys.length; i++) {
			State child = get(keys[i]);
			
			//System.out.println("Key (" + keys[i] + ") at " + bb.position() + " child id=" + child.id);
			bb.putChar(keys[i]);
			bb.putInt(child.id);
		}
		
		if (hasOutput()) {
			Set<Output> outputs = getOutputs();
			bb.putInt(outputs.size());
			
			for (Output output: outputs) {
				output.writeTo(bb);
			}
		}
		else {
			bb.putInt(0);
		}
	}*/
}
