package com.robbix.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class represents a node in an XML document. It can be searched
 * for values and attributes with XPath style methods or have other
 * sub-nodes extracted from it.
 * 
 * This class is essentially a wrapper for org.w3c.dom.Node, with
 * conveinence methods addded.
 */
public class RNode
{
	private Node root;
	
	/**
	 * Loads a org.w3c.dom.Document from the specified XML file, validating
	 * it against referenced schema if {@code validate} is true.
	 * 
	 * The returned XNode represents the root tag/element of the document and
	 * not the document itself, as in the org.w3c.dom api.
	 */
	public static RNode load(File xmlFile, boolean validate) throws IOException
	{
		DocumentBuilderFactory parserFactory = DocumentBuilderFactory.newInstance();
		parserFactory.setIgnoringComments(true);
		parserFactory.setIgnoringElementContentWhitespace(true);
		parserFactory.setValidating(validate);

		try
		{
			DocumentBuilder parser = parserFactory.newDocumentBuilder();
			parser.setErrorHandler(new ErrorHandler()
			{
				public void error(SAXParseException exc)
				throws SAXException
				{
					throw new SAXException(exc);
				}
				
				public void fatalError(SAXParseException exc)
				throws SAXException
				{
					throw new SAXException(exc);
				}
				
				public void warning(SAXParseException exc)
				throws SAXException
				{
					throw new SAXException(exc);
				}
			});
			
			Node doc = parser.parse(xmlFile);
			NodeList rootChildren = doc.getChildNodes();
			
			if (rootChildren.getLength() == 0)
				throw new FileFormatException("No root node in " + xmlFile);
			
			if (rootChildren.getLength() > 1)
				throw new FileFormatException("Multiple root elements in " + xmlFile);
			
			return new RNode(rootChildren.item(0));
		}
		catch (ParserConfigurationException e)
		{
			throw new Error(e);
		}
		catch (SAXException e)
		{
			throw new FileFormatException(xmlFile, e.getMessage());
		}
	}
	
	/**
	 * Does not validate against DTD or Schema by default.
	 */
	public static RNode load(File xmlFile) throws IOException
	{
		return load(xmlFile, false);
	}
	
	/*
	 * Private constructor to base a XNode on a org.w3c.dom.Node.
	 */
	private RNode(Node node)
	{
		this.root = node;
	}
	
	/**
	 * Returns this node's name as it appears in xml.
	 */
	public String getName()
	{
		return root.getNodeName();
	}
	
	/**
	 * Returns the underlying W3C node.
	 */
	public Node getW3CNode()
	{
		return root;
	}
	
	/**
	 * Returns the node that matches the given path from the given root node.
	 * Returns null if none do.
	 * 
	 * If multiple nodes match the path, the first one found will be returned.
	 * Which node that is with respect to the order in the file is not
	 * guaranteed.
	 */
	public RNode getNode(String... path) throws FileFormatException
	{
		Node currentNode = root;
		
		for (int p = 0; p < path.length; ++p)
		{
			NodeList children = currentNode.getChildNodes();
			boolean found = false;
			
			for (int c = 0; !found && c < children.getLength(); ++c)
			{
				Node child = children.item(c);
				
				if (child.getNodeName().matches(path[p]))
				{
					currentNode = child;
					found = true;
				}
			}
			
			if (!found)
				throw new IllegalArgumentException(
					"Node not found " + Arrays.toString(path) + " on " + root.getNodeName());
		}
		
		return new RNode(currentNode);
	}
	
	/**
	 * Returns the text content of the node that matches the given path from
	 * the given root node.
	 * 
	 * If multiple nodes match the path, the first one found will be returned.
	 * Which node that is with respect to the order in the file is not
	 * guaranteed.
	 */
	public String getValue(String... path) throws FileFormatException
	{
		RNode xnode = getNode(path);
		
		if (xnode == null)
			return null;
		
		if (!isTextNode(xnode.root))
			throw new FileFormatException("Node has child element");
		
		return xnode.root.getTextContent().trim();
	}
	
	/**
	 * Returns a list of nodes that match the given path from the given
	 * root node. Returns empty list if none do.
	 * 
	 * Currently, the implementation of this method only branches the search
	 * path on the second to last branch, so all nodes in the returned list
	 * will be immediate siblings.
	 */
	public List<RNode> getNodes(String... path)
	{
		ArrayList<RNode> results = new ArrayList<RNode>();
		Node currentNode = root;
		
		for (int p = 0; p < path.length - 1; ++p)
		{
			NodeList children = currentNode.getChildNodes();
			Node nextNode = null;
			
			for (int c = 0; c < children.getLength(); ++c)
			{
				Node child = children.item(c);
				
				if (child.getNodeName().matches(path[p]))
				{
					nextNode = child;
					break;
				}
			}
			
			if (nextNode == null)
				return results;
			
			currentNode = nextNode;
		}
		
		NodeList children = currentNode.getChildNodes();
		
		for (int c = 0; c < children.getLength(); ++c)
		{
			Node child = children.item(c);
			
			if (child.getNodeName().matches(path[path.length - 1]))
			{
				results.add(new RNode(child));
			}
		}
		
		return results;
	}

	/**
	 * Returns a list of values of the nodes that match the given path from
	 * the given root node. Returns empty list if none do.
	 */
	public List<String> getValues(String... path) throws FileFormatException
	{
		ArrayList<String> results = new ArrayList<String>();
		Node currentNode = root;
	
		for (int p = 0; p < path.length - 1; ++p)
		{
			NodeList children = currentNode.getChildNodes();
			Node nextNode = null;
			
			for (int c = 0; c < children.getLength(); ++c)
			{
				Node child = children.item(c);
				
				if (child.getNodeName().matches(path[p]))
				{
					nextNode = child;
					break;
				}
			}
			
			if (nextNode == null)
				return results;

			currentNode = nextNode;
		}
		
		NodeList children = currentNode.getChildNodes();
		
		for (int c = 0; c < children.getLength(); ++c)
		{
			Node child = children.item(c);
			
			if (child.getNodeName().matches(path[path.length - 1]))
			{
				if (!isTextNode(child))
					throw new FileFormatException("Node has child element");
				
				results.add(child.getTextContent().trim());
			}
		}
		
		return results;
	}
	
	public String getValue()
	{
		return root.getTextContent().trim();
	}
	
	public <E extends Enum<E>> E getEnumValue(Class<E> enumType) throws FileFormatException
	{
		String text = getValue();
		text = text.toUpperCase();
		text = text.replace(' ', '_');
		return Enum.valueOf(enumType, text);
	}
	
	public <E extends Enum<E>> List<E> getEnumValues(Class<E> enumType, String... path)
	throws FileFormatException
	{
		List<String> values = getValues(path);
		List<E> enumValues = new ArrayList<E>(values.size());
		
		for (String value : values)
		{
			value = value.toUpperCase();
			value = value.replace(' ', '_');
			enumValues.add(value == null ? null : Enum.valueOf(enumType, value));
		}
		
		return enumValues;
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Throws FileFormatException if attribute is not present.
	 * Returns empty string if
	 * attribute has empty string definition in the XML.
	 */
	public String getAttribute(String name) throws FileFormatException
	{
		NamedNodeMap attrs = root.getAttributes();
		
		if (attrs == null)
			throw new IllegalArgumentException("node not an element");
		
		Node attr = attrs.getNamedItem(name);
		
		if (attr == null)
			throw new FileFormatException(String.format(
				"Attribute \"%1$s\" expected on <%2$s>",
				name,
				root.getNodeName()
			));
		
		return attr.getTextContent();
	}

	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Returns defaultValue if attribute is not present. Returns empty string
	 * if attribute has empty string definition in the XML.
	 */
	public String getAttribute(String name, String defaultValue)
	{
		NamedNodeMap attrs = root.getAttributes();
		
		if (attrs == null)
			throw new IllegalArgumentException("node not an element");
		
		Node attr = attrs.getNamedItem(name);
		
		if (attr == null)
			return defaultValue;
		
		String attrString = attr.getTextContent();
		
		if (attrString == null || attrString.isEmpty())
			return defaultValue;
		
		return attrString;
	}

	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not an int.
	 */
	public int getIntAttribute(String name) throws FileFormatException
	{
		return Integer.parseInt(getAttribute(name));
	}

	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Returns defaultValue if attribute is not presentor is empty.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not an int.
	 */
	public int getIntAttribute(String name, int defaultValue)
	{
		String attrString = getAttribute(name, null);
		
		if (attrString == null || attrString.isEmpty())
			return defaultValue;
		
		return Integer.parseInt(attrString);
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a double.
	 */
	public double getFloatAttribute(String name) throws FileFormatException
	{
		return Double.parseDouble(getAttribute(name));
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Returns defaultValue if attribute is not present or is empty.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a double.
	 */
	public double getFloatAttribute(String name, double defaultValue)
	{
		String attrString = getAttribute(name, null);
		
		if (attrString == null || attrString.isEmpty())
			return defaultValue;
		
		return Double.parseDouble(attrString);
	}

	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a boolean.
	 */
	public boolean getBooleanAttribute(String name) throws FileFormatException
	{
		return Boolean.parseBoolean(getAttribute(name));
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Returns defaultValue if attribute is not present or is empty.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a boolean.
	 */
	public boolean getBooleanAttribute(String name, boolean defaultValue)
	{
		String attrString = getAttribute(name, null);
		
		if (attrString == null || attrString.isEmpty())
			return defaultValue;
		
		return Boolean.parseBoolean(attrString);
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a Direction.
	 */
	public Direction getDirectionAttribute(String name) throws FileFormatException
	{
		String text = getAttribute(name);
		Direction dir = Direction.getDirection(text);
		
		if (dir == null)
			throw new FileFormatException(text + " is not a valid direction");
		
		return dir;
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Returns defaultValue if attribute is not present or is empty.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a Direction.
	 */
	public Direction getDirectionAttribute(String name, Direction defaultValue)
	{
		String attrString = getAttribute(name, null);
		Direction dir = Direction.getDirection(attrString);
		
		if (dir == null)
			return defaultValue;
		
		return dir;
	}
	
	public <V extends Enum<V>> V getEnumAttribute(Class<V> enumType, String name)
	throws FileFormatException
	{
		String text = getAttribute(name);
		
		try
		{
			text = text.toUpperCase();
			text = text.replace(' ', '_');
			return Enum.valueOf(enumType, text);
		}
		catch (Exception e)
		{
			throw new FileFormatException(e.getMessage());
		}
	}
	
	public <V extends Enum<V>> V getEnumAttribute(Class<V> enumType, String name, V defaultValue)
	{
		String text = getAttribute(name, null);
		
		if (text == null)
			return defaultValue;
		
		try
		{
			text = text.toUpperCase();
			text = text.replace(' ', '_');
			return Enum.valueOf(enumType, text);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Color format is like HTML color codes: #RRGGBB, in hexidecimal.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a Color.
	 */
	public Color getColorAttribute(String name) throws FileFormatException
	{
		return Color.decode(getAttribute(name));
	}
	
	/**
	 * Gets the value of the attribute by the given name for the specified
	 * node.
	 * 
	 * Color format is like HTML color codes: #RRGGBB, in hexidecimal.
	 * 
	 * Returns defaultValue if attribute is not present or is empty.
	 * 
	 * @throws NumberFormatException If attribute is defined and has a value,
	 *                               but value is not a Color.
	 */
	public Color getColorAttribute(String name, Color defaultValue)
	{
		String attrString = getAttribute(name, null);
		
		if (attrString == null || attrString.isEmpty())
			return defaultValue;
		
		return Color.decode(attrString);
	}
	
	private static String[][] namePairs = {
		{"offsetX", "offsetY"},
		{"x", "y"}
	};
	
	/**
	 * Gets the values of Sprite offsets from typical attribute names.
	 * 
	 * e.g. "offsetX" and "offsetY"
	 *      "x" and "y"
	 */
	public Offset getOffsetAttributes()
	{
		for (String[] pair : namePairs)
		{
			try
			{
				return new Offset(
					getIntAttribute(pair[0]),
					getIntAttribute(pair[1])
				);
			}
			catch (FileFormatException e)
			{
				continue;
			}
		}
		
		return new Offset();
	}
	
	/**
	 * Returns true if this node has no child elements.
	 */
	private static boolean isTextNode(Node root)
	{
		NodeList children = root.getChildNodes();
		
		for (int c = 0; c < children.getLength(); ++c)
			if (children.item(c).getNodeType() == Node.ELEMENT_NODE)
				return false;
		
		return true;
	}
}
