package niuteam.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class DomUtil {
	protected static DocumentBuilderFactory documentBuilderFactory;
	private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	public static Document stream2doc(InputStream ins) {
		try{
//			documentBuilderFactory.setNamespaceAware(true);
		    DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
//		    factory.setAttribute(name, value);
			return builder.parse(ins);
		}
		catch(Exception e){
			CONST.log.error("Parse xml error: ", e);
			return null;
		}
		// 	InputStream
	}
	public static EntityResolver entityResolver = new EntityResolver() {
		
		private String previousLocation;
		
		@Override
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			String resourcePath;
			if (systemId.startsWith("http:")) {
				URL url = new URL(systemId);
				resourcePath = "dtd/" + url.getHost() + url.getPath();
				previousLocation = resourcePath.substring(0, resourcePath.lastIndexOf('/'));
			} else {
				resourcePath = previousLocation + systemId.substring(systemId.lastIndexOf('/'));
			}
			
			if (this.getClass().getClassLoader().getResource(resourcePath) == null) {
				throw new RuntimeException("remote resource is not cached : [" + systemId + "] cannot continue");
			}

			InputStream in = DomUtil.class.getClassLoader().getResourceAsStream(resourcePath);
			return new InputSource(in);
		}
	};	
	static {
		init();
	}
	private static void init() {
		DomUtil.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilderFactory.setValidating(false);
	}
	
	public static XmlSerializer createXmlSerializer(OutputStream out) throws UnsupportedEncodingException {
		return createXmlSerializer(new OutputStreamWriter(out, CONST.ENCODING));
	}
	
	public static XmlSerializer createXmlSerializer(Writer out) {
		XmlSerializer result = null;
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setValidating(true);
			result = factory.newSerializer();
			result.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			result.setOutput(out);
		} catch (Exception e) {
			CONST.log.error("When creating XmlSerializer: " + e.getClass().getName() + ": " + e.getMessage());
		}
		return result;
	}
	
	public DocumentBuilderFactory getDocumentBuilderFactory() {
		return documentBuilderFactory;
	}

	/**
	 * Creates a DocumentBuilder that looks up dtd's and schema's from epublib's classpath.
	 * 
	 * @return
	 */
	public static DocumentBuilder createDocumentBuilder() {
		DocumentBuilder result = null;
		try {
			result = documentBuilderFactory.newDocumentBuilder();
			//result.setEntityResolver(entityResolver);
		} catch (ParserConfigurationException e) {
			CONST.log.error(e.getMessage());
		}
		return result;
	}
	
	/**
	 * Reads the given resources inputstream, parses the xml therein and returns the result as a Document
	 * 
	 * @param resource
	 * @param documentBuilderFactory
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Document getAsDocument(Resource resource) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException {
		DocumentBuilder documentBuilder = createDocumentBuilder();
		InputSource inputSource = getInputSource(resource);
		if (inputSource == null) {
			return null;
		}
		Document result = documentBuilder.parse(inputSource);
		return result;
	}
	/**
	 * Gets the contents of the Resource as an InputSource in a null-safe manner.
	 * 
	 */
	public static InputSource getInputSource(Resource resource) throws IOException {
		if (resource == null) {
			return null;
		}
		Reader reader = resource.getReader();
		if (reader == null) {
			return null;
		}
		InputSource inputSource = new InputSource(reader);
		return inputSource;
	}	
	/**
	 * Gets the first element that is a child of the parentElement and has the given namespace and tagName
	 * 
	 * @param parentElement
	 * @param namespace
	 * @param tagName
	 * @return
	 */
	public static Element getFirstElementByTagNameNS(Element parentElement, String namespace, String tagName) {
		NodeList nodes = parentElement.getElementsByTagNameNS(namespace, tagName);
		if(nodes.getLength() == 0) {
			return null;
		}
		return (Element) nodes.item(0);
	}
	public static List<String> getElementsTextChild(Element parentElement, String namespace, String tagname) {
		NodeList elements = parentElement.getElementsByTagNameNS(namespace, tagname);
		List<String> result = new ArrayList<String>(elements.getLength());
		for(int i = 0; i < elements.getLength(); i++) {
			result.add(getTextContent((Element) elements.item(i)));
		}
		return result;
	}
	public static String getTextContent(Element parentElement) {
		if(parentElement == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		NodeList childNodes = parentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if ((node == null) ||
					(node.getNodeType() != Node.TEXT_NODE)) {
				continue;
			}
			result.append(((Text) node).getData());
		}
		return result.toString().trim();
	}

	public static String node2String(Node node) throws Exception {
		if (node == null)
			return null;
		StringWriter out = null;

		try {
			Transformer transformer = transformerFactory.newTransformer();
			out = new StringWriter();
			transformer.transform(new DOMSource(node), new StreamResult(out));
			return out.toString();
		} catch (Exception e) {
			throw e;
		} finally {
			if (out != null)
				out.close();
		}
	}

}
