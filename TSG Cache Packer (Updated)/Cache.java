import java.io.*;
import java.awt.image.*;
import java.awt.*;
import javax.imageio.*;


public class Cache 
{
	static FileBuilder game_contents;

	public static void packFiles(File[] fonts, File[][] interfaces, File[] hovers) throws IOException
	{
		game_contents = new FileBuilder(65536);
		game_contents.skipBytes(2); //We Come Back To Write Length.
		game_contents.writeByte((byte)fonts.length);
		game_contents.writeByte((byte)interfaces.length);
		game_contents.writeShort((short)hovers.length);

		int SRC = game_contents.writerIndex();

		int TYPES = 3; //TYPES = {font, interface, hover}.
		game_contents.skipBytes(4 * TYPES);

		game_contents.writeIndexSizeInt(SRC + (4 * 0), game_contents.writerIndex());
		game_contents.skipBytes(4 * fonts.length);

		game_contents.writeIndexSizeInt(SRC + (4 * 1), game_contents.writerIndex());
		game_contents.skipBytes(4 * interfaces.length);

		game_contents.writeIndexSizeInt(SRC + (4 * 2), game_contents.writerIndex());
		game_contents.skipBytes(4 * hovers.length);

		int seek = SRC + (4 * 3);

		game_contents.writeIndexSizeShort(0, seek - 2);

		for (File f : fonts) 
		{
			game_contents.writeIndexSizeInt(seek, game_contents.writerIndex());
			FileBuilder font = FileBuilder.read(f);
			FileBuilder fStream = getFontStream(f);
			
			game_contents.writeInt(font.capacity() + fStream.capacity() + 4);

			game_contents.writeInt(font.capacity());

			game_contents.writeBytes(font);

			game_contents.writeBytes(fStream);
			seek += 4;
		}

		int n = 0;

		for (File[] files : interfaces) 
		{
			File parseFile = getFileFromList(files, "$.TXT");
			FileParser parser = new FileParser(parseFile);
			FileAttribute attribute = null;
			int BEGIN = 0;
			int CHILDREN_COUNT = 0;

			while (true)
			{
				attribute = parser.poll();
				if (parser.endOfFile())
					break;
				if (checkAttribute(attribute = parser.poll(), 'S', "CHILD"))
					++CHILDREN_COUNT;
			}
			System.out.println("Children Count: " + CHILDREN_COUNT);
			parser.rewind();

			if (checkAttribute(attribute = parser.poll(), 'S', "PARENT")) 
			{
				game_contents.writeIndexSizeInt(seek, game_contents.writerIndex());

				BEGIN = game_contents.writerIndex();
				game_contents.skipBytes(4);
				
				FileBuilder parent = attributeToBuilder(parseFile, attribute);

				int capacity = parent.capacity();
				game_contents.writeInt(capacity);

				byte type = (byte)(attribute = parser.poll()).element();

				byte alignX = 0; 
				byte alignY = 0;
				short offsetX = 0;
				short offsetY = 0;

				if (capacity > 0)
				{
					game_contents.writeBytes(parent);

					alignX = (byte)(attribute = parser.poll()).element();
					alignY = (byte)(attribute = parser.poll()).element();

					offsetX = (short)(attribute = parser.poll()).element();
					offsetY = (short)(attribute = parser.poll()).element();
				}

				String[] colors = ((String)(attribute = parser.poll()).element()).split(",");			
	
				int color = (Integer.parseInt(colors[0]) << 0) |
					    (Integer.parseInt(colors[1]) << 8) |
					    (Integer.parseInt(colors[2]) << 16) |
					    (Integer.parseInt(colors[3]) << 24);

				byte value = (byte)(alignX | (alignY << 2) | (type << 4));

				game_contents.writeByte(value);

				if (capacity > 0)
				{
					game_contents.writeShort(offsetX);
					game_contents.writeShort(offsetY);
				}
				
				game_contents.writeInt(color);

				game_contents.writeByte((byte)CHILDREN_COUNT);
			}

			while (checkAttribute(attribute = parser.poll(), 'S', "CHILD")) 
			{
				String element = (String)attribute.element();

				FileBuilder child = attributeToBuilder(parseFile, attribute);

				byte type = (byte)(attribute = parser.poll()).element();

				byte alignX = (byte)(attribute = parser.poll()).element();
				byte alignY = (byte)(attribute = parser.poll()).element();

				int xIndex = ((byte)(attribute = parser.poll()).element()) & 0xF;
				int xSpaces = ((byte)(attribute = parser.poll()).element()) & 0xF;

				int yIndex = ((byte)(attribute = parser.poll()).element()) & 0xF;
				int ySpaces = ((byte)(attribute = parser.poll()).element()) & 0xF;

				short offsetX = (short)(attribute = parser.poll()).element();
				short offsetY = (short)(attribute = parser.poll()).element();

				int value = (short)(alignX | (alignY << 2) | (type << 4) | (xIndex << 6) | (xSpaces << 10) | (yIndex << 14) | (ySpaces << 18));
				
				game_contents.writeInt(value);
				
				if (type == 0)
				{
					int capacity = child.capacity();

					game_contents.writeInt(capacity);
					if (capacity > 0)
					{
						game_contents.writeBytes(child);
					}
				} 
				else if (type == 1) 
				{
					byte resourceId = (byte)(attribute = parser.poll()).element();
					byte scaleFactor = (byte)(attribute = parser.poll()).element();
					
					game_contents.writeByte(resourceId);
					game_contents.writeByte(scaleFactor);
					game_contents.writeString(element);
				}

				String[] colors = ((String)(attribute = parser.poll()).element()).split(",");			
	
				int color = (Integer.parseInt(colors[0]) << 0) |
					    (Integer.parseInt(colors[1]) << 8) |
					    (Integer.parseInt(colors[2]) << 16) |
					    (Integer.parseInt(colors[3]) << 24);

				int hoverId = (short)(attribute = parser.poll()).element();

				game_contents.writeShort(offsetX);
				game_contents.writeShort(offsetY);

				game_contents.writeInt(color);
				game_contents.writeShort(hoverId);
			}
			seek += 4;	
			game_contents.writeIndexSizeInt(BEGIN, game_contents.writerIndex() - BEGIN - 4);
		}

		for (File h : hovers) 
		{
			game_contents.writeIndexSizeInt(seek, game_contents.writerIndex());
			FileBuilder hover = FileBuilder.read(h);

			game_contents.writeInt(hover.capacity() + 4 + 4);
			game_contents.writeInt(hover.capacity());

			game_contents.writeBytes(hover);

			int color = (255 << 0) | (255 << 8) | (255 << 16) | (255 << 24);
			game_contents.writeInt(color);

			seek += 4;
		}

		testFileContents();
	}

	private static boolean checkAttribute(FileAttribute attribute, char type, String check) 
	{
		if (attribute == null)
			return false;
		if (attribute.type() == type) 
		{
			if (attribute.name().length < check.length())
				return false;
			for (int i = 0; i < check.length(); ++i) 
			{
				if (check.charAt(i) != attribute.name()[i])
					return false;	
			}
			return true;
		}
		return false;
	}

	private static File getFileFromList(File[] files, String name) throws IOException
	{
		for (File f : files) 
		{
			if (f.getName().equalsIgnoreCase(name)) 
			{
				return f;	
			}
		}
		return null;
	}

	private static FileBuilder attributeToBuilder(File parser, FileAttribute attribute)
	{
		String path = parser.getPath();
		path = path.substring(0, path.length() - parser.getName().length());
		path = path + (String)attribute.element();
		return FileBuilder.read(new File(path));
	}

	private static FileBuilder getFontStream(File file)
	{
		FileBuilder stream = new FileBuilder(4096);
		BufferedImage image = getImage(file);
		int height = 0;
		for (int y = 1; y < image.getHeight(); y++)
		{
			Color rgba = new Color(image.getRGB(1, y), true);
			int color_value = rgba.getRGB() | rgba.getAlpha();
			if (-65281 == color_value)
			{
				height = y;
				break;
			}
		}
		int start = 0;
		int h = 1;
		int boxes = 0;
		int gridX = 0;
		int gridY = 0;
		int idx = 0;
		short[] positions = new short[256];
		byte[] widths = new byte[256];
		for (int x = 1; x < image.getWidth(); x++)
		{
			Color rgba = new Color(image.getRGB(x, h), true);
			int color_value = rgba.getRGB() | rgba.getAlpha();
			if (-65281 == color_value)
			{
				if (x - start == 1)
				{					
					if ((h + height) < image.getHeight())
					{
						start = 0;
						h += height;
						x = 1;
						gridX = 0;
						++gridY;
						continue;
					} else {
						break;
					}
				}
				positions[idx] = (short)(start + 1);
				widths[idx] = (byte)((x - 1) - (start));
				++gridX;
				++boxes;
				start = x;
				++idx;
				if (x + 1 == image.getWidth())
				{	
					if ((h + height) < image.getHeight())
					{
						start = 0;
						h += height;
						x = 1;
						gridX = 0;
						++gridY;
						continue;
					} else {
						break;
					}					
				}
			}
		}
		stream.writeByte((byte)0x20);
		stream.writeByte((byte)height);
		stream.writeByte((byte)boxes);
		for (int i = 0; i < boxes; ++i)
		{
			stream.writeShort(positions[i]);
			stream.writeByte(widths[i]);
		}
		return stream.slice();
	}

	private static BufferedImage getImage(File file)
	{
		BufferedImage image = null;
		try {
			image = ImageIO.read(file);
		} catch (IOException ioe) {
			System.out.println("[ERROR]: File -> " + file + " <- Doesn't Exist.");
		}
		return image;
	}

	private static void testFileContents()
	{
		game_contents.write("./output/cache.dat");
		testContents();
	}

	public static void testContents() 
	{
		int checksum = (int)System.nanoTime();
		System.out.println("\n\nTesting Cache Contents....");
		FileBuilder instream = FileBuilder.read(new File("./output/cache.dat"));
		
		int readableBytes = instream.readShort();
		StringBuilder display = new StringBuilder();

		byte header1 = (byte)instream.readByte();
		byte header2 = (byte)instream.readByte();
		short header3 = (short)instream.readShort();

		int position1 = instream.readInt();
		int position2 = instream.readInt();
		int position3 = instream.readInt();

		display.append("\nReadable Bytes: ").append(readableBytes);

		display.append("\nFont Resource Header Size: ").append(header1);
		display.append("\nInterface Resource Header Size: ").append(header2);
		display.append("\nHover Resource Header Size: ").append(header3);

		display.append("\nFont Begin Position: ").append(position1);
		display.append("\nInterface Begin Position: ").append(position2);
		display.append("\nHover Begin Position: ").append(position3);
		
		for (int i = 0; i < header1; i++) 
		{
			display.append("\nFont[").append(i).append("]: ");
			
			instream.seek((i * 4) + position1);
			int seekPos = instream.readInt();

			display.append(seekPos).append(", Readable Data: ");

			instream.seek(seekPos);

			int len = instream.readInt();

			display.append(len);
		}

		for (int i = 0; i < header2; i++) 
		{
			display.append("\nInterface[").append(i).append("]: ");
			
			instream.seek((i * 4) + position2);
			int seekPos = instream.readInt();

			display.append(seekPos).append(", Readable: ");

			instream.seek(seekPos);

			int len = instream.readInt();

			display.append(len).append(", Parent Len: ");

			int val = instream.readInt();

			display.append(val).append(", Data { ");

			instream.seekInTo(val);

			display.append(instream.readByte()).append(",");

			if (val > 0)
			{

				display.append(instream.readShort()).append(",");

				display.append(instream.readShort()).append(",");
			}

			display.append(instream.readInt());

			display.append("}, Child Count: ").append(instream.readByte());
		}

		for (int i = 0; i < header3; i++) 
		{
			display.append("\nHover[").append(i).append("]: ");
			
			instream.seek((i * 4) + position3);
			int seekPos = instream.readInt();

			display.append(seekPos).append(", Readable Data: ");

			instream.seek(seekPos);

			int len = instream.readInt();

			display.append(len);
		}

		System.out.println(display.toString());
	
		StringBuilder sb = new StringBuilder();
		int sum = checksum * instream.capacity();

		sb.append("\n\n    private static uint[] CRC_TABLE =\n    {\n        0x");
		sum = readableBytes*sum;
		sb.append(Integer.toHexString(sum));

		sum = header1*sum;
		sb.append(", 0x").append(Integer.toHexString(sum));

		sum = header2*sum;
		sb.append(", 0x").append(Integer.toHexString(sum));

		sum = header3*sum;
		sb.append(", 0x").append(Integer.toHexString(sum));

		sum = position1*sum;
		sb.append(", 0x").append(Integer.toHexString(sum));

		sum = position2*sum;
		sb.append(", 0x").append(Integer.toHexString(sum));

		sum = position3*sum;
		sb.append(", 0x").append(Integer.toHexString(sum));

		sb.append("\n    };\n    private static uint CHECKSUM = 0x");
		sb.append(Integer.toHexString(checksum));
		sb.append(";\n\n");
		sb.append("Cache Size: " + instream.capacity());
		System.out.println(sb.toString());
	}
}