package com.intumit.android.search.util;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.intumit.android.search.Constants;

public class ByteBufferUtils {
	public static int DEFAULT_BYTE_BUFFER_SIZE = 20 * 1024 * 1024;

	public static void saveByteBufferToFile(ByteBuffer bb, File file) {
		try {
			bb.position(0);
			FileChannel channel = new FileOutputStream(file).getChannel();
			channel.write(bb);
			channel.close();
		} catch (Exception e) {
			if (Constants.DEBUG)
				System.err.println(e.getMessage());
		}
	}

	public static ByteBuffer resizeByteBuffer(ByteBuffer original, int newSize) {
		ByteBuffer newOne = ByteBuffer.allocateDirect(newSize);
		original.rewind();// copy from the beginning

		int backupLimit = original.limit();
		if (newSize < original.capacity() && newSize < backupLimit)
			original.limit(newSize);

		newOne.put(original);
		original.rewind();
		newOne.flip();

		original.limit(backupLimit);
		return newOne;
	}

}
