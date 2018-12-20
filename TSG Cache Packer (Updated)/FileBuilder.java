import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.BufferUnderflowException;

import java.lang.IllegalArgumentException;

public class FileBuilder 
{
	private byte[] buffer;
	private int readerIndex = 0;
	private int writerIndex = 0;

	public void skipBytes(int skipped) {
		writerIndex += skipped;
	}

	public void seekInTo(int seek)
	{
		readerIndex += seek;
	}

	public void seek(int index)
	{
		readerIndex = index;
	}

	public int readableBytes() {
		return (buffer.length - readerIndex);
	}

	public int readerIndex() {
		return readerIndex;
	}

	public int writerIndex() {
		return writerIndex;
	}

	public int capacity() 
	{
		return buffer.length;
	}

	public byte[] array() {
		return buffer;
	}

	public FileBuilder() {
		buffer = new byte[16];
	}

	public FileBuilder(int length) {
		buffer = new byte[length];
	}

	public FileBuilder(byte[] buffer) {
		this.writerIndex = buffer.length;
		this.buffer = buffer;
	}
	
	public void writeIndexSizeShort(int seek, int size) {
		int oldIndex = writerIndex;
		writerIndex = seek;
		writeShort(size);
		writerIndex = oldIndex;	
	}

	public void writeIndexSizeInt(int seek, int size) {
		int oldIndex = writerIndex;
		writerIndex = seek;
		writeInt(size);
		writerIndex = oldIndex;	
	}

	public void writeBoolean(boolean b)
	{
		ensureCapacity(1);
		buffer[writerIndex++] = (byte)(b ? 1 : 0);
	}

	public void writeByte(byte data) {
		ensureCapacity(1);
		buffer[writerIndex++] = data;
	}
	
	public void writeBytes(FileBuilder contents)
	{
		int length = contents.writerIndex == 0 ? contents.buffer.length : contents.writerIndex;
		ensureCapacity(length);
		System.arraycopy(contents.array(), 0, buffer, writerIndex, length);
		writerIndex += length;
	}

	public void writeShort(int data) {
		ensureCapacity(2);
		buffer[writerIndex++] = (byte)(data >> 8);
		buffer[writerIndex++] = (byte)(data);
	}

	public void writeInt(int data) {
		ensureCapacity(4);
		buffer[writerIndex++] = (byte)(data >> 24);
		buffer[writerIndex++] = (byte)(data >> 16);
		buffer[writerIndex++] = (byte)(data >> 8);
		buffer[writerIndex++] = (byte)(data);
	}

	public void writeLong(long data) {
		ensureCapacity(8);
		buffer[writerIndex++] = (byte)(data >> 56);
		buffer[writerIndex++] = (byte)(data >> 48);
		buffer[writerIndex++] = (byte)(data >> 40);
		buffer[writerIndex++] = (byte)(data >> 32);
		buffer[writerIndex++] = (byte)(data >> 24);
		buffer[writerIndex++] = (byte)(data >> 16);
		buffer[writerIndex++] = (byte)(data >> 8);
		buffer[writerIndex++] = (byte)(data);
	}

	public void writeString(String s) {
		writeString(s.toCharArray());
	}

	public void writeString(char[] data) {
		ensureCapacity(data.length);
		for (int i = 0; i < data.length; i++)
			buffer[writerIndex++] = (byte)data[i];
		buffer[writerIndex++] = 0;
	}

	public boolean readBoolean()
	{
		ensureBounds(1);
		return buffer[readerIndex++] == 1;
	}

	public int readByte() {
		ensureBounds(1);
		return buffer[readerIndex++] & 255;
	}

	public byte readSignedByte() {
		ensureBounds(1);
		return buffer[readerIndex++];
	}

	public int readShort() {
		ensureBounds(2);
		return (readByte() << 8) | readByte();
	}

	public int readSignedShort() {
		ensureBounds(2);
		return (readSignedByte() << 8) | readByte();
	}

	public int readInt() {
		ensureBounds(4);
		return (readByte() << 24) | (readByte() << 16) | (readByte() << 8) | readByte();
	}

	public int readSignedInt() {
		ensureBounds(4);
		return (readSignedByte() << 24) | (readSignedByte() << 16) | (readSignedByte() << 8) | readByte();
	}

	public long readLong() {
		ensureBounds(8);
        	return ((0xffffffffL & (long)readInt()) << 32) + (0xffffffffL & (long)readInt());
	}

	public String readString() {
		StringBuilder sb = new StringBuilder();
		byte b;
		while((b = buffer[readerIndex++]) != 0) {
			ensureBounds(1);
			sb.append((char) b);
		}
		return sb.toString();
	}

	public void ensureBounds(int reads) {
		if (readerIndex + reads > writerIndex)
			throw new ArrayIndexOutOfBoundsException("ReaderIndex: " + readerIndex + " WriterIndex: " + writerIndex);
	}

	public void ensureCapacity(int reads) 
	{
		if(reads + writerIndex > buffer.length) 
		{
			byte[] temp = new byte[getNextPowerOfTwo(reads + writerIndex)];
			System.arraycopy(buffer, 0, temp, 0, writerIndex);
			buffer = temp;
		}
	}

	public FileBuilder slice()
	{
		FileBuilder builder = new FileBuilder(writerIndex);
		builder.writeBytes(this);
		return builder;
	}

	public static int getNextPowerOfTwo(int size)
	{
		int n = -1;
		while (size >> ++n > 0);
			return 1 << n;
	}

	public static FileBuilder read(String s, int seek, int length) 
	{
		byte[] bytes = new byte[length];
		try {
			RandomAccessFile file = new RandomAccessFile(s, "r");
			FileChannel channel = file.getChannel();
			MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, seek, length);
			buffer.get(bytes, 0, length);
		} catch (IOException io) {
			System.out.println("IO Error File Not Found: " + s);
		} catch (BufferUnderflowException bufe) {
			System.out.println("Read Out Bounds Exception: { Seek: " + seek + " , Length: " + length + " }");
		} catch (IllegalArgumentException iae) {
			System.out.println("Seek Out Bounds Exception: { Seek: " + seek + " , Length: " + length + " }");
		}
		return new FileBuilder(bytes);
	}

	public static FileBuilder read(File file) 
	{
		int length = (int)file.length();
		byte[] bytes = new byte[length];
		try 
		{
			DataInputStream data = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        		data.readFully(bytes,0,length);
			data.close();
		} catch (IOException io) {
			System.out.println("IO Error - Directory N/A: " + file + " , Length: " + length);
		}
		return new FileBuilder(bytes);
	}

	public void write(String s) 
	{
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(s));
        		dos.write(buffer, 0, writerIndex == 0 ? buffer.length : writerIndex);
			dos.close();
		} catch (IOException io) {
			System.out.println("IO Error - Directory N/A: " + s);
		}
	}

}