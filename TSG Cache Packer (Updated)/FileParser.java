/*
* @Author - Jeremy Trifilo (Digistr).
*/

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

public class FileParser
{
	private final byte[] STORAGE;

	private int currentPosition = 0;
	private int currentLine = 1;
	private boolean endOfFile = false;

	public FileParser(String file) throws IOException
	{
	      	FileInputStream in = new FileInputStream(file);
      		in.read(STORAGE = new byte[in.available()], 0, STORAGE.length);
		in.close();
	}

	public FileParser(File file) throws IOException
	{
	      	FileInputStream in = new FileInputStream(file);
      		in.read(STORAGE = new byte[in.available()], 0, STORAGE.length);
		in.close();
	}

	public void rewind() 
	{
		currentPosition = 0;
		currentLine = 1;
		endOfFile = false;
	}

	public int getCurrentPosition() 
	{
		return currentPosition;
	}
	
	public boolean endOfFile()
	{
		return endOfFile;
	}

	public FileAttribute poll() throws IOException
	{
		int start = 0, end = 0;

		if (currentPosition == STORAGE.length) 
		{
			endOfFile = true;
			return null;
		}
		
		for (int n = currentPosition; n < STORAGE.length; ++n)
		{
			if (STORAGE[n] == '\n')
				++currentLine;
			else if (STORAGE[n] == '<') 
			{
				start = n + 1;
				break;
			}
		}

		if (start == 0) 
		{
			endOfFile = true;
			return null;
		}

		for (int n = start; n < STORAGE.length; ++n)
		{
			if (STORAGE[n] == '\n')
				++currentLine;
			else if (STORAGE[n] == '>') 
			{ 
				end = n;
				break;
			}
		}

		if (end < start)
			throw new IOException("Error Parsing Element At Line: " + currentLine);
		char type = '\0';
		Object element = null;
		char[] name = new char[end - start];
		for (int i = 0; i < name.length; ++i)
			name[i] = (char)STORAGE[start + i];

		for (int n = start; n < STORAGE.length; ++n)
		{
			if (STORAGE[n] == '<') {
				start = end + 1;
				end = n;
				break;
			}
		}
	
		if (STORAGE[end + 1] == '/' && STORAGE[end + 3] == '>')
		{
			type = (char)STORAGE[end + 2];
		} else {
			throw new IOException("Error Parsing Element At Line: " + currentLine);
		}

		if (type == 'S')
		{
			for (int i = start; i < start + (end - start); i++)
			{
				if (STORAGE[i] == '\\')
				{
					System.out.println("NEW LINE");
					STORAGE[i] = '\n';
				}
			}
			element = new String(STORAGE, start, end - start);
		} else {
			long value = 0;
			byte negative = 0;
			if (STORAGE[start] == '-')
				negative = 1;
			for (int n = start + negative; n < end; n++) 
			{
				if (STORAGE[n] >= '0' && STORAGE[n] <= '9')
					value = value * 10 + STORAGE[n] - 48;
			}
			if (negative == 1)
				value = -value;
			switch (type)
			{
				case 'B':
					element = (byte) value;
					break;
				case 'W':
					element = (short) value;
					break;
				case 'I':
					element = (int) value;
					break;
				default:
					element = value;
					break;					
			}
			
		}
		currentPosition = end + 4;
		return new FileAttribute(element, type, name);
	}
}