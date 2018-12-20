/*
* @Author - Jeremy Trifilo (Digistr).
*/

public class FileAttribute 
{
	private Object element = null;

	private char[] name;
	private char type = '\0';


	protected FileAttribute(Object element, char type, char[] name)
	{
		this.element = element;
		this.type = type;
		this.name = name;
	}

	public Object element() {
		return element;
	}

	public char[] name()
	{
		return name;
	}

	public char type()
	{
		return type;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\nChar Name: ");
		sb.append(name);
		sb.append(", Element: ");
		sb.append(element);
		sb.append(", Type: ");
		sb.append(type);
		return sb.toString();
	}
}