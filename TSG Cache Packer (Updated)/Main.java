import java.io.File;
import java.io.FileFilter;

public class Main
{
	private static final ExtensionsFilter FILTER = new ExtensionsFilter(new String[] {".png","$.txt"});

	private static byte state = 0;

	static final class ExtensionsFilter implements FileFilter 
	{
		private char[][] extensions;

		private ExtensionsFilter(String[] extensions)
		{
			int length = extensions.length;
			this.extensions = new char[length][];
			for (String s : extensions)
			{
				this.extensions[--length] = s.toCharArray();
			}
		}

		@Override
		public boolean accept(File file)
		{
			if (file.isDirectory())
				return true;
			char[] path = file.getPath().toCharArray();
			for (char[] extension : extensions)
			{
				if (extension.length > path.length)
				{
					continue;
				}
				int pStart = path.length - 1;
				int eStart = extension.length - 1;
				boolean success = true;
				for (int i = 0; i <= eStart; i++)
				{
					if ((path[pStart - i] | 0x20) != (extension[eStart - i] | 0x20))
					{
						success = false;
						break;
					}
				}
				if (success)
					return true;
			}
			return false;
		}
	}


	public static void main(String[] args) {
		File[] fonts = new File("./fonts/").listFiles(FILTER);
		File[] interfaceDirectories = new File("./interfaces/").listFiles(FILTER);
		File[] hovers = new File("./hovers/").listFiles(FILTER);
		File[][] interfaces = new File[interfaceDirectories.length][];
		for (int i = 0; i < interfaces.length; i++)
			interfaces[i] = interfaceDirectories[i].listFiles(FILTER);

		try 
		{
			Cache.packFiles(fonts, interfaces, hovers);
		} catch (Exception e) {
			System.out.println("PACKING ERROR - " + e.toString()); 
			e.printStackTrace(); 
		}
	}
}