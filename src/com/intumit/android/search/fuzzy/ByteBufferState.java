package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import com.intumit.android.search.Constants;
import com.intumit.android.search.fuzzy.ByteBufferState.RootByteBufferState;
import com.intumit.android.search.util.ByteBufferUtils;

/**
 * 
 * Structure of each state
 * byte 00~03 						=> int 	=> state id
 * byte 04	 						=> byte => depth
 * byte 05~08 						=> int 	=> pre-allocated number of keys
 * byte 09~12						=> int	=> number of keys
 * byte 13+((N-1)*6)~14+((N-1)*6) 	=> char	=> char of Nth key
 * byte 15+((N-1)*6)~18+((N-1)*6) 	=> int	=> child state position of Nth key
 * byte 13+(N*6)~16+(N*6) 			=> int	=> number of outputs
 * 
 * [if] number of outputs == 0		==> end
 * [else if] (number of outputs > NUM_OF_OUTPUTS_SPACE_RESERVED)	==> 
 * byte 17+(N*6)~20+(N*6) 			=> output	=> outputs
 * [else]
 * byte 17+(N*6)~20+(N*6) 			=> output	=> outputs
 * [reserve]	NUM_OF_OUTPUTS_SPACE_RESERVED * SIZE_OF_EACH_OUTPUT bytes
 * 
 * @author herb
 */
public class ByteBufferState extends State {
	final static boolean ENABLE_RESERVE_OUTPUT_SPACE = false;
	final static int NUM_OF_OUTPUTS_SPACE_RESERVED = 1;
	
	final static int MAX_CHUNK_SIZE = 1024 * 1024;
	
	final static int[] PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH = {128, 64, 64, 64, 32, 4};
	
	final static int BYTES_OF_CHAR = 2;
	final static int BYTES_OF_INTEGER = 4;
	
	final static int OFFSET_TO_VAR_END_OF_BUFFER = 12;
	
	final static int OFFSET_TO_DEPTH = 4;
	final static int OFFSET_TO_PREALLOCATED_KEY_SIZE = 5;
	final static int OFFSET_TO_KEY_SIZE = 9;
	final static int OFFSET_TO_KEYS = OFFSET_TO_KEY_SIZE + 4;
	
	final static int SIZE_OF_INDEX_HEADER = 16;
	final static int SIZE_OF_EACH_KEY = BYTES_OF_CHAR + BYTES_OF_INTEGER;
	final static int SIZE_OF_EACH_OUTPUT = 12;

	final static int CMD_JUMP = -1;

	
	private int startPos;
	RootByteBufferState root = null;
	
	public static class RootByteBufferState extends ByteBufferState {
		ByteBuffer bb;
		int endOfBuffer;

		public RootByteBufferState(int presetId, ByteBuffer bb, int pos) {
			super(presetId, null, pos);
			this.bb = bb;
			endOfBuffer = bb.limit();
		}


		public RootByteBufferState(int presetId, ByteBuffer bb, int pos, int endOfBuffer) {
			super(presetId, null, pos);
			this.bb = bb;
			this.endOfBuffer = endOfBuffer;
		}
	}

	ByteBufferState(int presetId, RootByteBufferState root, int pos) {
		super(presetId);
		this.root  = root;
		this.startPos = pos;
		
		// Check if is a jumped chunk
		startPos = getRealStartPosition(root, startPos);
	}

	private ByteBufferState(RootByteBufferState root, int pos) {
		super();
		this.root  = root;
		this.startPos = pos;
		
		// Check if is a jumped chunk
		startPos = getRealStartPosition(root, startPos);
	}
	
	private static State newState(RootByteBufferState root,
			int startPos) {
		return new ByteBufferState(root, startPos);
	}

	public int getStartPosition() {
		return startPos;
	}
	
	// Method need for add

	@Override
	public void addAllOutputs(State that) {
		super.addAllOutputs(that);
	}

	@Override
	public void addAllOutputs(Set<Output> toBeAdd) {
		super.addAllOutputs(toBeAdd);
	}
	
	static int hit=0;
	static int nothit=0;

	@Override
	public void addOutput(Output output) {
		if (root.endOfBuffer + MAX_CHUNK_SIZE >= root.bb.capacity()) {
			if (Constants.DEBUG)
				System.out.println("addOutput() enlarge resizing...");
			enlargeBuffer();
		}

		int preallocatedKeySize = root.bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
		int offset = startPos + OFFSET_TO_KEYS + (preallocatedKeySize * SIZE_OF_EACH_KEY);
		int numOfOutput = root.bb.getInt(offset);
		
		int newNodeStartPos = root.endOfBuffer;
		
		if (ENABLE_RESERVE_OUTPUT_SPACE) {
		if (numOfOutput > 0 && numOfOutput < NUM_OF_OUTPUTS_SPACE_RESERVED) {
			hit++;
			newNodeStartPos = startPos;
		}
		else {
			nothit++;
		}
		}
		
		root.bb.limit(root.bb.capacity());
		copyAndAddOutputToCurrent(root, startPos, newNodeStartPos, output);
		this.startPos = newNodeStartPos;
	}

	@Override
	public State extend(char b) {
		if (root.endOfBuffer + MAX_CHUNK_SIZE >= root.bb.capacity()) {
			if (Constants.DEBUG)
				System.out.println("extend() enlarge resizing...");
			enlargeBuffer();
		}

		int newStartPosOfCurrentState = root.endOfBuffer;
		int preallocatedSize = getPreallocatedSize();
		int keySize = keys().length;
		
		if (keySize < preallocatedSize) {
			//System.out.println("Extend at the same position:" + startPos);
			newStartPosOfCurrentState = startPos;
		}
		else {
			//System.out.println("Extend at the new position:" + startPos + "->" + newStartPosOfCurrentState);
		}
		root.bb.limit(root.bb.capacity());
		int startPosOfExtendedState = copyAndExtendState(root, b, startPos, newStartPosOfCurrentState);
		this.startPos = newStartPosOfCurrentState;
		
		return ByteBufferState.newState(root, startPosOfExtendedState);
	}

	private int getPreallocatedSize() {
		return root.bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
	}

	static protected void updateEndOfBuffer(ByteBufferState.RootByteBufferState root, int endOfLastAddedState) {
		if (endOfLastAddedState > root.endOfBuffer) {
			root.endOfBuffer = endOfLastAddedState;
			root.bb.putInt(OFFSET_TO_VAR_END_OF_BUFFER, root.endOfBuffer);
		}
	}
	
	/**
	 * For given start pos of State, calculate the bytes it occupied.
	 * @param startPos be sure it's a real start position, not a jumped state.
	 * @return
	 */
	static protected int getStateSize(ByteBuffer bb, int startPos) {
		//startPos = getRealStartPosition(root, startPos);
		int preallocatedKeySize = bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
		//int keySize = root.bb.getInt(startPos + OFFSET_TO_KEY_SIZE);
		int sizeWithoutOutput = ByteBufferState.OFFSET_TO_KEYS + ByteBufferState.SIZE_OF_EACH_KEY * preallocatedKeySize;
		int outputSize = bb.getInt(startPos + sizeWithoutOutput);
		
		if (ENABLE_RESERVE_OUTPUT_SPACE) {
			return sizeWithoutOutput + BYTES_OF_INTEGER + (SIZE_OF_EACH_OUTPUT * (outputSize == 0 ? 0 : Math.max(outputSize, NUM_OF_OUTPUTS_SPACE_RESERVED)));
		}
		return sizeWithoutOutput + BYTES_OF_INTEGER + (SIZE_OF_EACH_OUTPUT * outputSize);
	}

	private void enlargeBuffer() {
		if (Constants.DEBUG) {
			System.out.println("BB Resizing: " + root.endOfBuffer + "/" + root.bb.capacity() + " to " + (root.bb.capacity() + 10 * MAX_CHUNK_SIZE));
		}
		root.bb.resizeByteBuffer(root.bb.capacity() + 10 * MAX_CHUNK_SIZE);
	}

	/*private ByteBuffer getRightByteBufferAndSetCorrectPosition(int pos) {
		if (pos >= root.bb.capacity()) {
			root.extBB.position( pos - root.bb.capacity() );
			return root.extBB;
		}
		
		root.bb.position(pos);
		return root.bb;
	}*/

	@Override
	public void extendAllAndAddOutput(char[] chars, Output output) {
		ByteBufferState state = (ByteBufferState)this;
		boolean doExtendAndAddOutput = false;
		for (int i = 0; i < chars.length; i++) {
			ByteBufferState s = (ByteBufferState)state.get(chars[i]);
			if (s != null) {
				state = s;
			}
			else {
				if (i == (chars.length - 1)) {
					doExtendAndAddOutput = true;
					break;
				}
				else {
					state = (ByteBufferState)state.extend(chars[i]);
//					AhoCorasickFactory.printTrieRecursive(root.bb, root.getStartPosition(), 6);
				}
			}
		}
		
		if (doExtendAndAddOutput) {
			state.extendWithOutput(chars[chars.length - 1], output);
//			AhoCorasickFactory.printTrieRecursive(root.bb, root.getStartPosition(), 6);
		}
		else {
			state.addOutput(output);
		}
	}

	private void extendWithOutput(char c, Output output) {
		if (root.endOfBuffer + MAX_CHUNK_SIZE >= root.bb.capacity()) {
			root.bb.resizeByteBuffer(root.bb.capacity() + 10 * MAX_CHUNK_SIZE);
		}

		int startPosOfCurrentState = root.endOfBuffer;
		int preallocatedSize = getPreallocatedSize();
		int keySize = keys().length;
		
		if (keySize < preallocatedSize) {
			//System.out.println("Extend at the same position:" + startPos);
			startPosOfCurrentState = startPos;
		}
		else {
			//System.out.println("Extend at the new position:" + startPos + "->" + newNodeStartPos);
		}
		root.bb.limit(root.bb.capacity());
		int startPosOfExtendedState = copyAndExtendStateWithAddOutputToChild(root, c, startPos, startPosOfCurrentState, output);
		this.startPos = startPosOfCurrentState;
	}

	@Override
	public void put(char key, State value) {
		super.put(key, value);
	}
	
	// Method need for search begin here
	public char[] keys() {
		int offset = OFFSET_TO_KEY_SIZE;
		char[] keys = new char[root.bb.getInt(startPos + offset)];
		offset = OFFSET_TO_KEYS;
		
		for (int i=0; i < keys.length; i++) {
			keys[i] = root.bb.getChar(startPos + offset + (i * SIZE_OF_EACH_KEY));
		}
		return keys;
	}

	@Override
	public ByteBufferState get(char key) {
		// Do your job
		int offset = OFFSET_TO_KEY_SIZE;
		
		root.bb.position(startPos + offset);
		int numOfKeys = root.bb.getInt();
		for (int i=0; i < numOfKeys; i++) {
			char c = root.bb.getChar();
			int childPos = getRealStartPosition(root, root.bb.getInt());
			int childStateId = root.bb.getInt(childPos);
			
			if (c == key)
				return new ByteBufferState(childStateId, root, childPos);
		}
		// End of job
		
		return null;
	}

	@Override
	public Set<Output> getOutputs() {
		int preallocatedKeySize = root.bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
		int offset = startPos + OFFSET_TO_KEYS + (preallocatedKeySize * SIZE_OF_EACH_KEY);
		int numOfOutput = root.bb.getInt(offset);
		offset+=4;
		
		if (numOfOutput == 0)
			return null;
		
		Set<Output> outputs = new HashSet<Output>();
		for (int i=0; i < numOfOutput; i++,offset+=SIZE_OF_EACH_OUTPUT) {
			outputs.add(Output.readFrom(root.bb, offset));
		}
		
		return outputs;
	}

	@Override
	public boolean hasOutput() {
		try {
			int preallocatedKeySize = root.bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
			int numOfOutput = root.bb.getInt(startPos + OFFSET_TO_KEYS + preallocatedKeySize * SIZE_OF_EACH_KEY);
			
			return numOfOutput > 0;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	
	
	// Static
	public static char[] keys(ByteBufferState.RootByteBufferState root, int startPos) {

		startPos = getRealStartPosition(root, startPos);
		// Do your job
		char[] keys = new char[root.bb.getInt(startPos + OFFSET_TO_KEY_SIZE)];
		int offset = startPos + OFFSET_TO_KEYS;
		
		for (int i=0; i < keys.length; i++,offset+=SIZE_OF_EACH_KEY) {
			keys[i] = root.bb.getChar(offset);
		}
		// End of job
		
		return keys;
	}
	
	public static int getChildStatePosition(ByteBufferState.RootByteBufferState root, int startPos, int childIndex) {
		// Do your job
		startPos = getRealStartPosition(root, startPos);
		int offset = OFFSET_TO_KEYS + (SIZE_OF_EACH_KEY*childIndex) + 2;
		return root.bb.getInt(startPos + offset);
		// End of job
	}
	
	public static int[] childrenPositions(ByteBufferState.RootByteBufferState root, int startPos) {
		// Do your job
		startPos = getRealStartPosition(root, startPos);
		int[] childPositions = new int[root.bb.getInt(startPos + OFFSET_TO_KEY_SIZE)];
		int offset = startPos + OFFSET_TO_KEYS;
		
		for (int i=0; i < childPositions.length; i++,offset+=SIZE_OF_EACH_KEY) {
			childPositions[i] = root.bb.getInt(offset + 2);
		}
		// End of job
		
		return childPositions;
	}
	
	static int getRealStartPosition(ByteBufferState.RootByteBufferState root, int startPos) {
		while (root != null && root.bb.getInt(startPos) == CMD_JUMP) {
			startPos = root.bb.getInt(startPos + 4);
		}
		
		return startPos;
	}
	
	public static Set<Output> getOutputs(ByteBufferState.RootByteBufferState root, int startPos) {
		startPos = getRealStartPosition(root, startPos);
		int preallocatedKeysSize = root.bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
		int offset = startPos + OFFSET_TO_KEYS + (preallocatedKeysSize * SIZE_OF_EACH_KEY);
		int numOfOutput = root.bb.getInt(offset);
		
		if (numOfOutput == 0)
			return null;
		
		offset += 4; // Add the num of outputs offset, and put it after numOfOuput==0 check is for performance
		
		Set<Output> outputs = new HashSet<Output>();
		for (int i=0; i < numOfOutput; i++,offset+=SIZE_OF_EACH_OUTPUT) {
			outputs.add(Output.readFrom(root.bb, offset));
		}
		
		return outputs;
	}

	public static boolean hasOutput(ByteBufferState.RootByteBufferState root, int startPos) {
		startPos = getRealStartPosition(root, startPos);
		int preallocatedKeysSize = root.bb.getInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE);
		int numOfOutput = root.bb.getInt(startPos + OFFSET_TO_KEYS + preallocatedKeysSize * SIZE_OF_EACH_KEY);
		
		return numOfOutput > 0;
	}

	public static void setChildStatePosition(ByteBuffer bb, int startPos, int childIndex, int childPos) {
		bb.putInt(startPos + OFFSET_TO_KEYS + (SIZE_OF_EACH_KEY*childIndex) + 2, childPos);
	}

	public static ByteBuffer childsBB(ByteBuffer bb, int startPos) {
		int remember = bb.position();
		
		// Do your job
		bb.position(startPos);
		
		ByteBuffer nb = bb.slice();
		// End of job
		
		bb.position(remember);
		return nb;
	}
	
	/**
	 * Copy state chunk to new position, including set jump command at start of original state
	 * 
	 * @param bb
	 * @param originalStartPos
	 * @param newStartPos
	 * @return endPosition after copied
	public static int copyState(ByteBuffer bb, int originalStartPos, int newStartPos) {
		int sizeOfState = getStateSize(bb, originalStartPos);
		byte[] buf = new byte[sizeOfState];
		bb.position(originalStartPos);
		bb.get(buf);
		
		bb.putInt(originalStartPos, 	CMD_JUMP);			// jump command
		bb.putInt(originalStartPos + 4, newStartPos);	// jump to this position
		
		bb.position(newStartPos);
		bb.put(buf);
		
		return newStartPos + sizeOfState;
	}
	 */

	/**
	 * Copy state chunk to new position, including add a new child state of "char c", then set jump command at start of original state
	 * @param bb
	 * @param originalStartPos
	 * @param newStartPos
	 * @return endPosition after copied
	 */
	public static int copyAndExtendState(RootByteBufferState root, char c, int originalStartPos, int newStartPos) {
		ByteBufferStateBean bean = ByteBufferStateBean.readFrom(root, originalStartPos);
		
		int sizeOfKeys = bean.keys.length + 1;
		char[] keys = new char[sizeOfKeys];
		int[] childrenPos = new int[sizeOfKeys];
		
		System.arraycopy(bean.keys, 0, keys, 0, bean.keys.length);
		System.arraycopy(bean.childStatePositions, 0, childrenPos, 0, bean.keys.length);
		
		keys[ sizeOfKeys - 1] = c;
		childrenPos[ sizeOfKeys - 1 ] = -1; // Not initialized
		
		bean.keys = keys;
		bean.childStatePositions = childrenPos;
		
		while (sizeOfKeys > bean.preallocatedKeySize) {
			int incSize = bean.depth >= ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH.length 
						? 1
						: ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[ bean.depth-1];
			
			bean.preallocatedKeySize += incSize;
		}
		
		// Start initailize new stat
		if (originalStartPos != newStartPos) {
			root.bb.putInt(originalStartPos, CMD_JUMP);			// jump command
			root.bb.putInt(originalStartPos + 4, newStartPos); // jump to this position
		}
		
		ByteBufferStateBean.writeTo(root, bean, newStartPos);
		
		int extendedStatePos = root.endOfBuffer;
		// Start initailize new state
		int extendedId = ++State._STATE_ID_SEED;
		byte newDepth = (byte)(bean.depth + 1);
		int preallocateSize = newDepth >= ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH.length 
				? 1
				: ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[ newDepth-1];
	
		root.bb.putInt(extendedStatePos, extendedId);
		root.bb.put   (extendedStatePos + OFFSET_TO_DEPTH, newDepth);
		root.bb.putInt(extendedStatePos + OFFSET_TO_PREALLOCATED_KEY_SIZE, preallocateSize);
		root.bb.putInt(extendedStatePos + OFFSET_TO_KEY_SIZE, 0); // no keys
		root.bb.putInt(extendedStatePos + OFFSET_TO_KEYS + preallocateSize * SIZE_OF_EACH_KEY, 0); // no outputs
		setChildStatePosition(root.bb, newStartPos, sizeOfKeys - 1, extendedStatePos);
		
		updateEndOfBuffer(root, extendedStatePos + getStateSize(root.bb, extendedStatePos));
		
		if (Constants.DEBUG) {
			ByteBufferStateBean extended = ByteBufferStateBean.readFrom(root, extendedStatePos);
			
			Assert.assertEquals(extended.id, extendedId);
			Assert.assertEquals(extended.depth, newDepth);
			Assert.assertEquals(extended.preallocatedKeySize, preallocateSize);
			Assert.assertTrue(Arrays.equals(extended.keys, new char[0]));
			Assert.assertNull(extended.outputs);
		}
		
		return extendedStatePos;
	}


	/**
	 * 
	 * @param bb
	 * @param c
	 * @param originalStartPos
	 * @param newStartPos
	 * @param outputInChild
	 * @return End of extended state
	 */
	public static int copyAndExtendStateWithAddOutputToChild(RootByteBufferState root, char c, int originalStartPos, int newStartPos, Output outputInChild) {

		ByteBufferStateBean bean = ByteBufferStateBean.readFrom(root, originalStartPos);
		
		int sizeOfKeys = bean.keys.length + 1;
		char[] keys = new char[sizeOfKeys];
		int[] childrenPos = new int[sizeOfKeys];
		
		System.arraycopy(bean.keys, 0, keys, 0, bean.keys.length);
		System.arraycopy(bean.childStatePositions, 0, childrenPos, 0, bean.keys.length);
		
		keys[ sizeOfKeys - 1] = c;
		childrenPos[ sizeOfKeys - 1 ] = -1; // Not initialized
		
		bean.keys = keys;
		bean.childStatePositions = childrenPos;
		
		while (sizeOfKeys > bean.preallocatedKeySize) {
			int incSize = bean.depth >= ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH.length 
						? 1
						: ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[ bean.depth-1];
			
			bean.preallocatedKeySize += incSize;
		}

		// Start initailize new stat
		if (originalStartPos != newStartPos) {
			root.bb.putInt(originalStartPos, CMD_JUMP);			// jump command
			root.bb.putInt(originalStartPos + 4, newStartPos); // jump to this position
		}
		
		ByteBufferStateBean.writeTo(root, bean, newStartPos);
		// Start initailize new state
		int extendedStatePos = root.endOfBuffer;
		int extendedId = ++State._STATE_ID_SEED;
		byte newDepth = (byte)(bean.depth + 1);
		int preallocateSize = newDepth >= ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH.length 
				? 1
				: ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[ newDepth-1];
	
		root.bb.putInt(extendedStatePos, extendedId);
		root.bb.put   (extendedStatePos + OFFSET_TO_DEPTH, newDepth);
		root.bb.putInt(extendedStatePos + OFFSET_TO_PREALLOCATED_KEY_SIZE, preallocateSize);
		root.bb.putInt(extendedStatePos + OFFSET_TO_KEY_SIZE, 0); // no keys
		
		int cursor = extendedStatePos + OFFSET_TO_KEYS + preallocateSize * SIZE_OF_EACH_KEY;
		
		root.bb.putInt(cursor, 1); 
		cursor += BYTES_OF_INTEGER;
		
		outputInChild.writeTo(root.bb, cursor);
		
		if (ENABLE_RESERVE_OUTPUT_SPACE) {
			cursor += SIZE_OF_EACH_OUTPUT * NUM_OF_OUTPUTS_SPACE_RESERVED;
		}
		else {
			cursor += SIZE_OF_EACH_OUTPUT;
		}
		
		setChildStatePosition(root.bb, newStartPos, sizeOfKeys - 1, extendedStatePos);
		updateEndOfBuffer(root, cursor);
		return extendedStatePos;
	}

	public static int addToEmpty = 0;
	public static int addToOne = 0;
	public static int addToMore = 0;
	/**
	 * 
	 * @param bb
	 * @param originalStartPos
	 * @param newStartPos
	 * @param outputInCurrent
	 * @return return the end cursor position
	 */
	public static int copyAndAddOutputToCurrent(RootByteBufferState root, int originalStartPos, int newStartPos, Output outputInCurrent) {
		ByteBufferStateBean bean = ByteBufferStateBean.readFrom(root, originalStartPos);
		Set<Output> outputs = bean.outputs == null ? new HashSet<Output>() : bean.outputs;
		
		if (Constants.DEBUG) {
			switch (outputs.size()) {
			case 0:
				addToEmpty++;
				break;
			case 1:
				addToOne++;
				break;
			default:
				addToMore++;
			}
		}
		
		outputs.add(outputInCurrent);
		
		// Start initailize new stat
		if (originalStartPos != newStartPos) {
			root.bb.putInt(originalStartPos, CMD_JUMP);			// jump command
			root.bb.putInt(originalStartPos + 4, newStartPos); // jump to this position
		}
		
		bean.outputs = outputs;
		int endPosition = ByteBufferStateBean.writeTo(root, bean, newStartPos);
		updateEndOfBuffer(root, endPosition);
		return newStartPos;
	}
	

	
	// Serialization
	public static int writeStateTo(State state, ByteBuffer bb, byte depth, int startPos) {
		Assert.assertTrue(state.id != 0);
		
		if (bb.limit() - MAX_CHUNK_SIZE <= startPos && bb.limit() - MAX_CHUNK_SIZE <= bb.capacity()) {
			bb.limit(bb.capacity());
		}
		bb.putInt(startPos, state.id);
		bb.put(startPos + ByteBufferState.OFFSET_TO_DEPTH, depth);
		
		int preallocateBlockSize = depth >= ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH.length 
					? 1
					: ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[depth-1];
		
		//System.out.println("Write State ID:" + id + " at " + startPos);
		char[] keys = state.keys();
		if (preallocateBlockSize < keys.length) {
			preallocateBlockSize = (int) (Math.ceil((float)keys.length / preallocateBlockSize) * preallocateBlockSize);
			
			
			if (preallocateBlockSize < keys.length)
				Assert.assertTrue(preallocateBlockSize >= keys.length);
		}
			
		bb.putInt(startPos + ByteBufferState.OFFSET_TO_PREALLOCATED_KEY_SIZE, preallocateBlockSize);
		bb.putInt(startPos + ByteBufferState.OFFSET_TO_KEY_SIZE, keys.length);
		//System.out.println("Key size:" + keys.length + " at " + (startPos + ByteBufferState.OFFSET_TO_KEY_SIZE));
		int cursor = startPos + ByteBufferState.OFFSET_TO_KEYS;
		
		for (int i=0; i < keys.length; i++, cursor+=ByteBufferState.SIZE_OF_EACH_KEY) {
			State child = state.get(keys[i]);
			//System.out.println("Key (" + keys[i] + ") at " + cursor + " child id=" + child.id);
			//Assert.assertTrue(child.id != 0);
			bb.putChar(cursor, keys[i]);
			bb.putInt(cursor+2, child.id);
			//Assert.assertEquals(child.id, bb.getInt(cursor+2));
		}
		
		/*cursor = startPos + ByteBufferState.OFFSET_TO_KEYS;
		System.out.print("[");
		for (int i=0; i < keys.length; i++, cursor+=ByteBufferState.SIZE_OF_EACH_KEY) {
			char cc = bb.getChar(cursor);
			int cid = bb.getInt(cursor+2);
			System.out.print(" " + cc + ":" + cid);
		}
		System.out.println("]");
		
		System.out.print(cursor);*/
		cursor = startPos + ByteBufferState.OFFSET_TO_KEYS + (preallocateBlockSize * ByteBufferState.SIZE_OF_EACH_KEY);
		//System.out.println("/" + cursor);
		
		if (state.hasOutput()) {
			Set<Output> outputs = state.getOutputs();
			bb.putInt(cursor, outputs.size());
			cursor+=4;
			int markCursor = cursor;
			
			for (Output output: outputs) {
				output.writeTo(bb, cursor);
				cursor+=ByteBufferState.SIZE_OF_EACH_OUTPUT;				
				
			}
			
			if (ENABLE_RESERVE_OUTPUT_SPACE) {
			if (outputs.size() < NUM_OF_OUTPUTS_SPACE_RESERVED) {
				cursor = markCursor + NUM_OF_OUTPUTS_SPACE_RESERVED * SIZE_OF_EACH_OUTPUT;
			}
			}
		}
		else {
			bb.putInt(cursor, 0);
			cursor+=4;
		}
		

		/*ByteBufferState.RootByteBufferState fakeRoot = new ByteBufferState.RootByteBufferState(bb, 0);
		ByteBufferStateBean bean = ByteBufferStateBean.readFrom(fakeRoot, startPos);
		System.out.println(bean);*/
		
		return cursor; // End of state
	}

	public static State readStateFrom(ByteBuffer bb, int startPos, TIntObjectHashMap<int[]> pendingRefPos, boolean movePositionToEndOfState) {
		State state = new State(bb.getInt(startPos));

		byte depth = bb.get(startPos + ByteBufferState.OFFSET_TO_DEPTH);
		int preallocatedKeySize = bb.getInt(startPos + ByteBufferState.OFFSET_TO_PREALLOCATED_KEY_SIZE);
		int sizeOfKeys = bb.getInt(startPos + ByteBufferState.OFFSET_TO_KEY_SIZE);
		int cursor = startPos + ByteBufferState.OFFSET_TO_KEYS;
		
		if (sizeOfKeys > 0) {
			char[] keys = new char[sizeOfKeys];
			State[] states = new State[sizeOfKeys];
			
			state.keys = keys;
			state.states = states;
			
			for (int i=0; i < sizeOfKeys; i++,cursor+=ByteBufferState.SIZE_OF_EACH_KEY) {
				keys[i] = bb.getChar(cursor);
				int childStateId = bb.getInt(cursor+2);
				if (pendingRefPos != null) {
					if (Constants.DEBUG) {
						System.out.println("Push into pendingRefPos: this(" + state.id + ") -> Child " + childStateId + " {" + startPos + ", " + i + "}");
					}
					pendingRefPos.put(childStateId, new int[] {startPos, i});
				}
			}
		}

		cursor = startPos + ByteBufferState.OFFSET_TO_KEYS + preallocatedKeySize * ByteBufferState.SIZE_OF_EACH_KEY;
		int sizeOfOutputs = bb.getInt(cursor); cursor+=4;
		
		if (sizeOfOutputs > 0) {
			for (int i=0; i < sizeOfOutputs; i++,cursor+=ByteBufferState.SIZE_OF_EACH_OUTPUT) {
				state.addOutput(Output.readFrom(bb, cursor));
			}
		}
		
		
		
/*		ByteBufferState.RootByteBufferState fakeRoot = new ByteBufferState.RootByteBufferState(bb, 0);
		ByteBufferStateBean bean = ByteBufferStateBean.readFrom(fakeRoot, startPos);
		System.out.println(bean);
*/		
		if (movePositionToEndOfState) {
			if (ENABLE_RESERVE_OUTPUT_SPACE) {
				bb.position(startPos + OFFSET_TO_KEYS 
						+ preallocatedKeySize * SIZE_OF_EACH_KEY 
						+ (sizeOfOutputs == 0 ? 0 : Math.max(sizeOfOutputs, NUM_OF_OUTPUTS_SPACE_RESERVED)) * SIZE_OF_EACH_OUTPUT);
			}
			else 
				bb.position(cursor);
		}
		
		return state;
	}
	
	static class ByteBufferStateBean {
		int id;
		byte depth;
		int preallocatedKeySize;
		char[] keys;
		int[] childStatePositions;
		Set<Output> outputs;
		
		@Override
		public String toString() {
			return "ByteBufferStateBean [id=" + id + ", depth=" + depth
					+ ", preallocatedKeySize=" + preallocatedKeySize
					+ ", keySize=" + keys.length
					+ ", keys=" + Arrays.toString(keys)
					+ ", childStatePositions="
					+ Arrays.toString(childStatePositions) + ", outputs="
					+ outputs + "]";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(childStatePositions);
			result = prime * result + depth;
			result = prime * result + id;
			result = prime * result + Arrays.hashCode(keys);
			result = prime * result
					+ ((outputs == null) ? 0 : outputs.hashCode());
			result = prime * result + preallocatedKeySize;
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ByteBufferStateBean other = (ByteBufferStateBean) obj;
			if (!Arrays.equals(childStatePositions, other.childStatePositions))
				return false;
			if (depth != other.depth)
				return false;
			if (id != other.id)
				return false;
			if (!Arrays.equals(keys, other.keys))
				return false;
			if (outputs == null) {
				if (other.outputs != null)
					return false;
			} else if (!outputs.equals(other.outputs))
				return false;
			if (preallocatedKeySize != other.preallocatedKeySize)
				return false;
			return true;
		}

		static ByteBufferStateBean readFrom(RootByteBufferState root, int startPos) {
			ByteBufferStateBean bean = new ByteBufferStateBean();
			
			int id = root.bb.getInt(startPos);
			ByteBufferState bs = new ByteBufferState(id, root, startPos);
			bean.id = id;
			bean.depth = root.bb.get(startPos + ByteBufferState.OFFSET_TO_DEPTH);
			bean.preallocatedKeySize = root.bb.getInt(startPos + ByteBufferState.OFFSET_TO_PREALLOCATED_KEY_SIZE);
			bean.keys = ByteBufferState.keys(root, startPos);
			bean.childStatePositions = ByteBufferState.childrenPositions(root, startPos);
			bean.outputs = ByteBufferState.getOutputs(root, startPos);
			
			return bean;
		}

		static int writeTo(RootByteBufferState root, ByteBufferStateBean bean, int startPos) {
			ByteBuffer bb = root.bb;
			
			bb.putInt(startPos, bean.id);
			bb.put(startPos + OFFSET_TO_DEPTH, bean.depth);
			bb.putInt(startPos + OFFSET_TO_PREALLOCATED_KEY_SIZE, bean.preallocatedKeySize);
			bb.putInt(startPos + OFFSET_TO_KEY_SIZE, bean.keys.length);
			//System.out.println("Key size:" + keys.length + " at " + (startPos + ByteBufferState.OFFSET_TO_KEY_SIZE));
			int cursor = startPos + OFFSET_TO_KEYS;
			
			for (int i=0; i < bean.keys.length; i++, cursor+=ByteBufferState.SIZE_OF_EACH_KEY) {
				bb.putChar(cursor, bean.keys[i]);
				bb.putInt(cursor+2, bean.childStatePositions[i]);
			}
			
			cursor = startPos + OFFSET_TO_KEYS + bean.preallocatedKeySize * SIZE_OF_EACH_KEY;
			
			if (bean.outputs != null && bean.outputs.size() > 0) {
				//System.out.println(bean);
				bb.putInt(cursor, bean.outputs.size());
				cursor+=4;
				
				int markCursor = cursor;
				
				for (Output output: bean.outputs) {
					output.writeTo(bb, cursor);
					cursor+=SIZE_OF_EACH_OUTPUT;
				}
				
				if (ENABLE_RESERVE_OUTPUT_SPACE) {
					if (bean.outputs.size() < NUM_OF_OUTPUTS_SPACE_RESERVED) {
						cursor = markCursor + NUM_OF_OUTPUTS_SPACE_RESERVED * SIZE_OF_EACH_OUTPUT;
					}
				}
			}
			else {
				bb.putInt(cursor, 0);
				cursor+=4;
			}

			updateEndOfBuffer(root, cursor);
			
			if (Constants.DEBUG) {
				ByteBufferStateBean validate = ByteBufferStateBean.readFrom(root, startPos);
				Assert.assertEquals(validate, bean);
			}
			return cursor;
		}
	}

}
