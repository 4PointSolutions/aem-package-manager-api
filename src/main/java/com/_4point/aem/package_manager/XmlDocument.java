package com._4point.aem.package_manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlDocument {
	private final Document xmlDoc;
	private final XPath xpathFactory;
	
	private XmlDocument(Document xmlDoc, XPath xpathFactory) {
		this.xmlDoc = xmlDoc;
		this.xpathFactory = xpathFactory;
	}

	public static XmlDocument initializeXmlDoc(byte[] bytes) {
		try {
			XPath xPathFactory = XPathFactory.newInstance().newXPath();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(bytes));
			doc.getDocumentElement().normalize();
			return new XmlDocument(doc,xPathFactory);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new XmlDocumentException(String.format("Failed to create XmlDataContext ... %s", e.getMessage()),e);
		}
	}

	public Optional<String> getString(String xpath) {
		return getOneThing(xpath, this::mapNodeToString);
	}	

	public List<String> getStrings(String xpath) {
		return getManyThings(xpath, this::mapNodeToString);
	}

	public Optional<XmlDocument> getDoc(String xpath) {
		return getOneThing(xpath, this::mapNodeToDoc);
	}
	
	public List<XmlDocument> getDocs(String xpath) {			
		return getManyThings(xpath, this::mapNodeToDoc);
	}

	private <T> Optional<T> getOneThing(String xpath, BiConsumer<Node, Consumer<T>> nodeMapper) {
		List<T> things = getManyThings(xpath, nodeMapper);
		if (things.size() > 1) {
        	//Multiple matches for the same xpath (i.e. repeated sections)9i
        	throw new IllegalArgumentException(String.format("Failed to parse xml path %s to a single entry (Found %d entries).", xpath, things.size()));
        }
		return things.size() == 1 ? Optional.of(things.get(0)) : Optional.empty();
		
	}
	
	private <T> List<T> getManyThings(String xpath, BiConsumer<Node, Consumer<T>> nodeMapper) {
		try {
			NodeList nodes = getNodeListByXpath(xpath);	        
	        return IntStream.range(0, nodes.getLength())
	        	     		.mapToObj(nodes::item)
	        	     		.mapMulti(nodeMapper)
	        	     		.toList();
		} catch (XPathExpressionException e) {			
			throw new IllegalArgumentException(String.format("Failed to parse xml path %s. Error message: %s", xpath, e.getMessage()),e);
		} 		
	}

	private void mapNodeToDoc(Node node, Consumer<XmlDocument> consumer) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			consumer.accept(elementToDoc(node));	
		}
	}

	private XmlDocument elementToDoc(Node element) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document newDocument = builder.newDocument();
			Node importNode = newDocument.importNode(element, true);
			newDocument.appendChild(importNode);
			return new XmlDocument(newDocument, xpathFactory);
		} catch (DOMException | ParserConfigurationException e) {
			throw new IllegalArgumentException(String.format("Error creating new XmlDocument. Error message: %s", e.getMessage()),e);
		}
	}
	private void mapNodeToString(Node node, Consumer<String> consumer) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			consumer.accept(((Element)node).getTextContent());	
		} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			consumer.accept(((Attr)node).getTextContent());
		}
	}

	private NodeList getNodeListByXpath(String xpath) throws XPathExpressionException {
		XPathExpression expr = xpathFactory.compile(xpath);
		return (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
	}

	@SuppressWarnings("serial")
	public static class XmlDocumentException extends RuntimeException {

		private XmlDocumentException() {
		}

		private XmlDocumentException(String message, Throwable cause) {
			super(message, cause);
		}

		private XmlDocumentException(String message) {
			super(message);
		}

		private XmlDocumentException(Throwable cause) {
			super(cause);
		}
	}
}
