package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import com.intumit.android.search.Constants;
import com.intumit.android.search.fuzzy.ByteBufferState.RootByteBufferState;
import com.intumit.android.search.util.ByteBufferUtils;
import com.intumit.hithot.HitHotLocale;

public class AhoCorasickFactory {
	
	public static Logger log = Logger.getLogger(AhoCorasickFactory.class.getName());
	
	/**
	 * 每個語言只會有一個 AhoCorasick instance
	 * 如果傳入的語言不存在，或者沒有設定 WikiRankIndex（如果沒 forest cache），就會丟出 Exception
	 * 
	 * TODO 這裡並不 thread safe，目前是靠 InitServlet確保都啟用完成才進入 Server
	 * 
	 * @param locale
	 * @return
	public static AhoCorasick getInstance(HitHotLocale locale) {
		if (instances.containsKey(locale))  {
			return instances.get(locale);
		}
		AhoCorasick newOne = null;
		if (locale.isCjk())
			newOne = new AhoCorasick(Character.MAX_VALUE);
		else 
			newOne = new AhoCorasick(128);
		instances.put(locale, newOne);
		
		return newOne;
	}
	 */
	
	/**
	 * 
	 * 
	 * TODO 這裡並不 thread safe，目前是靠 InitServlet確保都啟用完成才進入 Server
	 * 
	 * @param locale
	 * @return
	 */
	public static StateAhoCorasick newStateAhoCorasick(HitHotLocale locale) {
		return newStateAhoCorasick(locale, false);
	}
	
	/**
	 * 
	 * 
	 * TODO 這裡並不 thread safe，目前是靠 InitServlet確保都啟用完成才進入 Server
	 * 
	 * @param locale
	 * @return
	 */
	public static StateAhoCorasick newStateAhoCorasick(HitHotLocale locale, boolean buildReverseMap) {
		StateAhoCorasick newOne = null;
		if (locale.isCjk())
			newOne = new StateAhoCorasick(Character.MAX_VALUE, buildReverseMap);
		else 
			newOne = new StateAhoCorasick(128, buildReverseMap);
		
		return newOne;
	}

	public static ByteBufferAhoCorasick newByteBufferAhoCorasick(HitHotLocale locale) {
		return newByteBufferAhoCorasick(locale, false);
	}
	
	/**
	 * 
	 * 
	 * TODO 這裡並不 thread safe，目前是靠 InitServlet確保都啟用完成才進入 Server
	 * 
	 * @param locale
	 * @return
	 */
	public static ByteBufferAhoCorasick newByteBufferAhoCorasick(HitHotLocale locale, boolean buildReverseMap) {
		ByteBufferAhoCorasick newOne = null;
		if (locale.isCjk())
			newOne = new ByteBufferAhoCorasick(Character.MAX_VALUE, buildReverseMap);
		else 
			newOne = new ByteBufferAhoCorasick(128, buildReverseMap);
		
		return newOne;
	}
	/**
	 * 從 kernel/forest 當中找尋 cache 檔案（就是把 object serialized）
	 * 
	 * @param locale
	 * @param aho
	 * @return true if there is at least one cache file.
	 */
	/*public static boolean loadTreeFromFile(File file, AhoCorasick aho) {
		FileInputStream fin = null; 
		BufferedInputStream bis = null;
		ObjectInputStream oin = null;
		long  start = System.currentTimeMillis();
		HashMap<String, Set<Output>> tmp = null;
		
		if (Constants.DEBUG) log.info("Tring to loading tree from file");
		try{
			fin = new FileInputStream(file);
			bis = new BufferedInputStream(fin);
			oin =new ObjectInputStream(bis);
			tmp = (HashMap<String, Set<Output>>) oin.readObject();
			if (Constants.DEBUG) log.info("Loading from "+ file.getName() +" ("+tmp.size() +")");
			
			for (Map.Entry<String, Set<Output>> entry: tmp.entrySet()) {
				aho.addString(entry.getKey(), entry.getValue());
			}
			
			aho.prepare();
		}catch (IOException e){
			if (Constants.DEBUG) log.log(Level.WARNING, "Error on loading:" + file.getName(),e);
		}catch (Exception e){
			if (Constants.DEBUG) log.log(Level.WARNING, e.getMessage());
		}finally{
			IOUtils.closeWhileHandlingException(oin);
			IOUtils.closeWhileHandlingException(bis);
			IOUtils.closeWhileHandlingException(fin);
		}

		if (Constants.DEBUG) log.info("Loading tree from file successful in " + (System.currentTimeMillis() - start) + " ms." );
		return true;
	}

	private static void clearMap(HashMap<String, Integer> data) {
		data.clear();
		System.gc();
	}
	
	public static void saveTreeToFile(File file, AhoCorasick aho) {
		HashMap<String, Set<Output>> keywords = new HashMap<String, Set<Output>>();
		
		FileOutputStream fo = null;
		ObjectOutputStream oo = null;
		BufferedOutputStream bos = null;
		
		try{
			List<FuzzySearchResult> allPaths = aho.dumpAllPaths();
			for (FuzzySearchResult path: allPaths) {
				keywords.put(path.pathToString(), path.getOutputs());
			}
			if (Constants.DEBUG) log.info("Tring to saving tree to file:" + file.getName() );
			fo = new FileOutputStream(file);
			bos = new BufferedOutputStream(fo);
			oo = new ObjectOutputStream(bos);
			oo.writeObject(keywords);
			oo.flush();
			oo.reset();
			if (Constants.DEBUG) log.info("Saving tree to " + file.getName() + " OK. " + file.exists() + ":" + (file.length() / 1024f) + "kb");
		}catch(Throwable e){
			if (Constants.DEBUG) log.log(Level.WARNING, e.getMessage());
		}finally{
			IOUtils.closeWhileHandlingException(oo);
			IOUtils.closeWhileHandlingException(bos);
			IOUtils.closeWhileHandlingException(fo);
		}
	}*/
	
	//public static TIntIntHashMap stateId2Position = new TIntIntHashMap();

	public static ByteBuffer loadTreeFromFileIntoByteBufferAhoCorasick(File file, ByteBufferAhoCorasick aho) {
		ByteBuffer bb = ByteBuffer.allocateDirect((int) file.length());
		
		try {
			TIntObjectHashMap<int[]> pendingRefPos = new TIntObjectHashMap<int[]>();
			FileInputStream is = new FileInputStream(file);
			FileChannel fc = is.getChannel();
			int read = fc.read(bb.bb);
			bb.position(0);
			//ByteBufferAhoCorasick.dumpByteBuffer(bb, read);
			
			int size = deserializeByteBufferAhoCorasick(bb, aho);
			aho.prepare();
		}
		catch (Exception e) {
			if (Constants.DEBUG) e.printStackTrace();
		}
		
		return bb;
	}
	
	public static int deserializeByteBufferAhoCorasick(ByteBuffer bb, ByteBufferAhoCorasick aho) {

		try {
			bb.resizeByteBuffer(bb.limit());
			TIntObjectHashMap<int[]> pendingRefPos = new TIntObjectHashMap<int[]>();
			//bb = channel.map(MapMode.READ_ONLY, 0, 10*1024*1024);

			int markStartPos = bb.position();
			aho.entryCount = bb.getInt(markStartPos);
			aho.maxDocId = bb.getInt(markStartPos+4);
			State._STATE_ID_SEED = bb.getInt(markStartPos+8);
			
			int endOfBuffer = bb.getInt(markStartPos+ByteBufferState.OFFSET_TO_VAR_END_OF_BUFFER);
			
			int rootPos = markStartPos + ByteBufferState.SIZE_OF_INDEX_HEADER;
			int rootId = bb.getInt(rootPos);
			int currPos = rootPos;
			
			while (bb.hasRemaining()) {
				State state = ByteBufferState.readStateFrom(bb, currPos, pendingRefPos, true);
				
				if (Constants.DEBUG) {
					if (state.getOutputs() != null) {
						for (Output o: state.getOutputs()) {
							Assert.assertTrue(o.getDocId() >= 0);
							if (o.getDocId() > aho.getMaxDocId()) {
								Assert.assertTrue(o.getDocId() <= aho.getMaxDocId());
							}
						}
					}
				}
				
				if (pendingRefPos.containsKey(state.id)) {
					int[] ref = pendingRefPos.remove(state.id);
					ByteBufferState.setChildStatePosition(bb, ref[0], ref[1], currPos);
				}
				/*else {
					System.out.println(state.id + " has no ref??");
				}*/
				currPos = bb.position();
			}
			
			
			if (Constants.DEBUG) {
				if (pendingRefPos.size() > 0) {
					System.out.println(pendingRefPos);
					Assert.assertTrue(pendingRefPos.size() == 0);
				}
			}
			
			//printTrieRecursive(bb, rootPos, 0);
			ByteBufferState.RootByteBufferState root = new ByteBufferState.RootByteBufferState(rootId, bb, rootPos, currPos);
			aho.setRoot(root);
			
			bb.position(markStartPos);
			bb.limit(currPos);
			bb.compact();
			
			return currPos;
		}
		catch (Exception e) {
			if (Constants.DEBUG) e.printStackTrace();
		}
		
		return -1;
	}
	
	/*public static void loadTreeFromFileBB(File file, AhoCorasick aho) {
		ByteBuffer bb = ByteBuffer.allocateDirect(ByteBufferUtils.DEFAULT_BYTE_BUFFER_SIZE);
		
		try {
			TIntObjectHashMap<Object[]> pendingRef = new TIntObjectHashMap<Object[]>();
			FileInputStream is = new FileInputStream(file);
			FileChannel fc = is.getChannel();
			int read = fc.read(bb);
			bb.limit(read);
			bb.position(0);
			//bb = channel.map(MapMode.READ_ONLY, 0, 10*1024*1024); // Dunno why this will cause NoWriteableChannel in android

			aho.entryCount = bb.getInt();
			aho.maxDocId = bb.getInt();
			State.ID = bb.getInt();
			
			State root = State.readFrom(bb, pendingRef);
			aho.root = root;
			
			while (bb.hasRemaining()) {
				State.readFrom(bb, pendingRef);
			}
			
			aho.prepare();
		}
		catch (Exception e) {
			if (Constants.DEBUG) e.printStackTrace();
		}
	}*/

	public static ByteBuffer serailizeToByteBuffer(AhoCorasick aho) {
		ByteBuffer bb = ByteBuffer.allocateDirect(ByteBufferUtils.DEFAULT_BYTE_BUFFER_SIZE);
		
		try {
			bb.putInt(0, aho.entryCount);
			bb.putInt(4, aho.maxDocId);
			bb.putInt(8, State._STATE_ID_SEED);
			
			if (Constants.DEBUG) {
				System.out.println("Aho entryCount:" + aho.entryCount + " / Max Doc Id: " + aho.maxDocId + " / State seed: " + State._STATE_ID_SEED);
			}
			
			byte depth = 1;
			int cursor = ByteBufferState.SIZE_OF_INDEX_HEADER;
			cursor = ByteBufferState.writeStateTo(aho.getRoot(), bb, depth, cursor);
			cursor = serializeNode(aho, bb, aho.getRoot(), depth, cursor, new char[0]);
			
			if (Constants.DEBUG) {
				if (aho instanceof ByteBufferAhoCorasick) {
					int endOfBuffer = ((ByteBufferAhoCorasick)aho).getBBRoot().endOfBuffer;
					
					System.out.println("Cursor / EndOfBuffer = " + cursor + " / " + endOfBuffer + " (" + (cursor == endOfBuffer ? "" : "Not ") + "Equal)");
				}
			}
			
			bb.putInt(ByteBufferState.OFFSET_TO_VAR_END_OF_BUFFER, cursor);
			bb.position(cursor);
			bb.flip();
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				e.printStackTrace();
				log.log(Level.SEVERE, e.getMessage());
			}
		}
		
		return bb;
	}
	
	static int serializeNode(AhoCorasick aho, ByteBuffer bb, State parentState, byte currentDepth, int cursor, char[] path) {
		if (currentDepth > AhoCorasick.MAX_DEPTH)
			throw new RuntimeException("Out of acceptable length:" + AhoCorasick.MAX_DEPTH + "/" + path.length + "["+ new String(path) + "]");
		
		char[] keys = parentState.keys();
		
		for (char key: keys) {
			State state = parentState.get(key);
			Assert.assertTrue(state.id != 0);
			
			int startPos = cursor;

			if (bb.capacity() - ByteBufferState.MAX_CHUNK_SIZE <= startPos) {

				if (Constants.DEBUG) {
					System.out.println("BB Resizing: " + bb.capacity() + "/" + bb.capacity() + " to " + (bb.capacity() + 10 * ByteBufferState.MAX_CHUNK_SIZE));
				}
				bb.resizeByteBuffer(bb.capacity() + 10 * ByteBufferState.MAX_CHUNK_SIZE);
			}
			cursor = ByteBufferState.writeStateTo(state, bb, currentDepth, cursor);
			
			if (Constants.DEBUG) {
				State s = ByteBufferState.readStateFrom(bb, startPos, null, false);
				System.out.println("State:" + state.id + " {" + startPos + ", " + "X");
				
				Assert.assertEquals(s.id, state.id);
				if (!Arrays.equals(s.keys(), state.keys())) {
					Assert.assertTrue(Arrays.equals(s.keys(), state.keys()));
				}
				
				if (s.getOutputs() == null && state.getOutputs() == null) {
				}
				else {
					Assert.assertTrue(s.getOutputs().containsAll(state.getOutputs()));
					
					for (Output o: s.getOutputs()) {
						Assert.assertTrue(o.getDocId() >= 0);
						if (o.getDocId() > aho.getMaxDocId()) {
							Assert.assertTrue(o.getDocId() <= aho.getMaxDocId());
						}
					}
				}
			}
		}	
		
		for (char key: keys) {
			State state = parentState.get(key);
			char[] newPath = Arrays.copyOf(path, path.length + 1);
			
			if (newPath.length != currentDepth)
				throw new RuntimeException("Incorrect path/depth: " + new String(newPath) + "/" + currentDepth);
			newPath[newPath.length - 1] = key;
			cursor = serializeNode(aho, bb, state, (byte)(currentDepth+1), cursor, newPath);
		}
		
		return cursor;
	}

	public static void saveTreeToFileBB(File file, AhoCorasick aho) {
		ByteBuffer bb = serailizeToByteBuffer(aho);
		
		try {
			FileChannel channel = new FileOutputStream(file).getChannel();
			channel.write(bb.bb);
			bb.clear();
			channel.close();
		}
		catch (Exception e) {
			if (Constants.DEBUG) log.log(Level.SEVERE, e.getMessage());
		}
	}
	
	/*static int recursiveDumpNode(FileChannel channel, ByteBuffer bb, State startFromHere, int cursor, char[] path) {
		char[] keys = startFromHere.keys();
		
		for (char key: keys) {
			State state = startFromHere.get(key);
			cursor = state.writeTo(bb, cursor);
			
			if (bb.remaining() < (bb.capacity() / 2)) {
				bb.flip();
				try {
					channel.write(bb);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				bb.clear();
			}
		}	
		
		for (char key: keys) {
			State state = startFromHere.get(key);
			char[] newPath = Arrays.copyOf(path, path.length + 1);
			newPath[newPath.length - 1] = key;
			cursor = recursiveDumpNode(channel, bb, state, cursor, newPath);
		}
		
		return cursor;
	}*/
	
	public static StringBuffer statistics(AhoCorasick aho) {
		StringBuffer sb = new StringBuffer();
		sb.append("Entry count:" + aho.getEntryCount() + "\n");
		sb.append("Max doc id:" + aho.getMaxDocId() + "\n");
		sb.append("SEED ID:" + State._STATE_ID_SEED + "\n");
		sb.append("Size of ReverseMap cound:" + aho.reverseMap.size() + "\n");
		sb.append("Is prepared:" + aho.isPrepared() + "\n");
		
		return sb;
	}

	
	static void printTrieRecursive(ByteBuffer bb, int startPos, int depth) {
		
		ByteBuffer childsBB = ByteBufferState.childsBB(bb, startPos);
		int numOfKeys = childsBB.getInt(ByteBufferState.OFFSET_TO_KEY_SIZE);
		
		int offset = ByteBufferState.OFFSET_TO_KEYS;
		
		for (int i = 0; i < numOfKeys; i++, offset+=ByteBufferState.SIZE_OF_EACH_KEY) {
			for (int k=0; k < depth; k++) System.out.print(" - ");
			char c = childsBB.getChar(offset);
			System.out.println(" " + c + " ");
		
			printTrieRecursive(bb, childsBB.getInt(offset+2), depth + 1);
		}
	}
}
