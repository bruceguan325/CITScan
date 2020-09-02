package com.intumit.android.search.fuzzy;

import java.nio.Buffer;

import com.intumit.android.search.util.ByteBufferUtils;

public class ByteBuffer {
	java.nio.ByteBuffer bb;

	ByteBuffer(int size, boolean direct) {
		bb = direct ? java.nio.ByteBuffer.allocateDirect(size)
				: java.nio.ByteBuffer.allocate(size) ;
	}
	
	ByteBuffer(java.nio.ByteBuffer slice) {
		bb = slice;
	}

	public void resizeByteBuffer(int newSize) {
		bb = ByteBufferUtils.resizeByteBuffer(bb, newSize);
	}

	public final int capacity() {
		return bb.capacity();
	}

	public final Buffer clear() {
		return bb.clear();
	}

	public java.nio.ByteBuffer compact() {
		return bb.compact();
	}

	public int compareTo(java.nio.ByteBuffer otherBuffer) {
		return bb.compareTo(otherBuffer);
	}

	public java.nio.ByteBuffer duplicate() {
		return bb.duplicate();
	}

	public final Buffer flip() {
		return bb.flip();
	}

	public byte get() {
		return bb.get();
	}

	public java.nio.ByteBuffer get(byte[] dst, int dstOffset, int byteCount) {
		return bb.get(dst, dstOffset, byteCount);
	}

	public java.nio.ByteBuffer get(byte[] dst) {
		return bb.get(dst);
	}

	public byte get(int index) {
		return bb.get(index);
	}

	public char getChar() {
		return bb.getChar();
	}

	public char getChar(int index) {
		return bb.getChar(index);
	}

	public double getDouble() {
		return bb.getDouble();
	}

	public double getDouble(int index) {
		return bb.getDouble(index);
	}

	public float getFloat() {
		return bb.getFloat();
	}

	public float getFloat(int index) {
		return bb.getFloat(index);
	}

	public int getInt() {
		return bb.getInt();
	}

	public int getInt(int index) {
		return bb.getInt(index);
	}

	public long getLong() {
		return bb.getLong();
	}

	public long getLong(int index) {
		return bb.getLong(index);
	}

	public short getShort() {
		return bb.getShort();
	}

	public short getShort(int index) {
		return bb.getShort(index);
	}

	public final boolean hasRemaining() {
		return bb.hasRemaining();
	}

	public final int limit() {
		return bb.limit();
	}

	public final Buffer limit(int newLimit) {
		return bb.limit(newLimit);
	}

	public final int position() {
		return bb.position();
	}

	public final Buffer position(int newPosition) {
		return bb.position(newPosition);
	}

	public java.nio.ByteBuffer put(byte b) {
		return bb.put(b);
	}

	public java.nio.ByteBuffer put(byte[] src, int srcOffset, int byteCount) {
		return bb.put(src, srcOffset, byteCount);
	}

	public final java.nio.ByteBuffer put(byte[] src) {
		return bb.put(src);
	}

	public java.nio.ByteBuffer put(java.nio.ByteBuffer src) {
		return bb.put(src);
	}

	public java.nio.ByteBuffer put(int index, byte b) {
		return bb.put(index, b);
	}

	public java.nio.ByteBuffer putChar(char value) {
		return bb.putChar(value);
	}

	public java.nio.ByteBuffer putChar(int index, char value) {
		return bb.putChar(index, value);
	}

	public java.nio.ByteBuffer putDouble(double value) {
		return bb.putDouble(value);
	}

	public java.nio.ByteBuffer putDouble(int index, double value) {
		return bb.putDouble(index, value);
	}

	public java.nio.ByteBuffer putFloat(float value) {
		return bb.putFloat(value);
	}

	public java.nio.ByteBuffer putFloat(int index, float value) {
		return bb.putFloat(index, value);
	}

	public java.nio.ByteBuffer putInt(int index, int value) {
		return bb.putInt(index, value);
	}

	public java.nio.ByteBuffer putInt(int value) {
		return bb.putInt(value);
	}

	public java.nio.ByteBuffer putLong(int index, long value) {
		return bb.putLong(index, value);
	}

	public java.nio.ByteBuffer putLong(long value) {
		return bb.putLong(value);
	}

	public java.nio.ByteBuffer putShort(int index, short value) {
		return bb.putShort(index, value);
	}

	public java.nio.ByteBuffer putShort(short value) {
		return bb.putShort(value);
	}

	public final int remaining() {
		return bb.remaining();
	}

	public final Buffer reset() {
		return bb.reset();
	}

	public final Buffer rewind() {
		return bb.rewind();
	}

	public ByteBuffer slice() {
		return new ByteBuffer(bb.slice());
	}

	public static ByteBuffer allocateDirect(int size) {
		return new ByteBuffer(size, true);
	}

	public static ByteBuffer allocate(int size) {
		return new ByteBuffer(size, false);
	}
	
	
}
