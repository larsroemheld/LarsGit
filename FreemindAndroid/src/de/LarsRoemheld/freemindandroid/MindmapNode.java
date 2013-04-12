/**
 * 
 */
package de.LarsRoemheld.freemindandroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;

/**
 * @author Lars
 *
 */
public class MindmapNode {
	private class xmlAttribute {
		String name;
		String value;
		
		public xmlAttribute(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
	
	private String text;
	private ArrayList<xmlAttribute> unusedAttributes;
	private ArrayList<MindmapNode> children;
	private MindmapNode parent; // TODO mäßige Lösung, da nicht multiple parents
	
	// Constructor: no Node without a valid children-list!!!
	public MindmapNode (MindmapNode parent) {
		this.parent = parent;
		this.children = new ArrayList<MindmapNode>();
		this.unusedAttributes = new ArrayList<xmlAttribute>();
	}
	
	public MindmapNode addChild(String text) {
		MindmapNode c = new MindmapNode(this);
		c.setText(text);
		
		this.children.add(c);
		
		return c;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
	public MindmapNode getParent() {
		return parent;
	}

	public void setParent(MindmapNode parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return this.getText();
	}
	
	public ArrayList<MindmapNode> getChildren() {
		return children;
	}
	public boolean hasChildren() {
		return !children.isEmpty();
	}
	
	// Deletes the node: this deletes all subsequent nodes and removes the association from this node's parent. 
	// The node is thus cleared for garbage collection
	private void delete_internal() {
		if (this.hasChildren()) {
			for (MindmapNode c : this.children) {
				c.delete_internal();
			}
		}
		
		this.parent = null;
		this.children.clear();
	}
	
	public void delete() {
		this.parent.children.remove(this);
		this.delete_internal();
	}

	
	private void searchInChildren_internal(String searchText, ArrayList<MindmapNode> results) {
		if (Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE).matcher(this.text).find());
			results.add(this);

		for (MindmapNode c : this.children) {
			c.searchInChildren_internal(searchText, results);
		}
	}

	public ArrayList<MindmapNode> searchInChildren(String searchText) {
		ArrayList<MindmapNode> results = new ArrayList<MindmapNode>();
		
		this.searchInChildren_internal(searchText, results);
		
		return results;
	}
	
	
	/*
	 * Reads the current node from the current position of the parser, recursively adding children as 
	 * reflected in xml structure.
	 * 
	 * return: false if some xml tags were skipped and thus ignored. true if all tags were properly read.
	 * XmlPullParser MUST be on a start_tag of "node".
	 */
	private void skipXmlTag(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }
	public boolean readFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
		boolean result = true;
		
		parser.require(XmlPullParser.START_TAG, null, null);

		if (parser.getName().equalsIgnoreCase("node")) {
			// Extract node text -- other attributes can be extracted here as well.
			this.setText(parser.getAttributeValue(null, "TEXT"));

			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (! parser.getAttributeName(i).equalsIgnoreCase("TEXT")) {
					this.unusedAttributes.add(
							new xmlAttribute(
									parser.getAttributeName(i),
									parser.getAttributeValue(i)
									));
				}
			}


			while (parser.next() != XmlPullParser.END_TAG) {
				// Scan for children -- this is done recursively, implying that the first end tag we find
				// belongs to us!

				if (parser.getEventType() == XmlPullParser.START_TAG) {
					if (parser.getName().equalsIgnoreCase("node")) {
						boolean r = this.addChild("").readFromXml(parser);
						result = (result && r);
					} else {
						result = false;
						skipXmlTag(parser);
					}
				}
			}
		}
		
		return result;
	}
	
	
	/*
	 * Writes the current node into the current position of the serializer, recursively adding children as 
	 * reflected in data structure
	 * 
	 * This implies that the serializer is in a valid environment (xml - settings, <map>, ...).
	 */
	public void writeToXml(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, "node");
		serializer.attribute(null, "TEXT", this.getText());
		
		for (xmlAttribute a : this.unusedAttributes) {
			if (! a.name.equalsIgnoreCase("TEXT")) {
				serializer.attribute(null, a.name, a.value);
			}
		}
		
		for (MindmapNode c : this.children) {
			c.writeToXml(serializer);
		}
		
		serializer.endTag(null, "node");
	}
}
