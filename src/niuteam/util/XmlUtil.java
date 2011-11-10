package niuteam.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import niuteam.book.core.CONST;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

public class XmlUtil {
	protected static DocumentBuilderFactory documentBuilderFactory;
	private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	static {
		init();
	}
	private static void init() {
		XmlUtil.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilderFactory.setValidating(false);
	}
	

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
	public static int node2Stream(Node node, OutputStream out) throws Exception {
		if (node==null || out == null) return -1;
	
		try{
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
	        transformer.transform(new DOMSource(node), new StreamResult(out));
	        return 0;
		}
		catch(Exception e){
			throw e;
		}
	}	

	public static String getTextContent(Element elm) {
		if(elm == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		NodeList childNodes = elm.getChildNodes();
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
	public static Document string2Document(String src) {
		if (src == null||src.trim().length() == 0)return null;
		if(src.charAt(0) !='<'){
			int i = src.indexOf('<');
			if (i == -1) return null;
			src = src.substring(i,src.length());
		}
		StringReader sreader  = null;
		try{
		    DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
	        sreader = new StringReader(src);
			InputSource is = new InputSource(sreader);
			return builder.parse(is);
		}
		catch(Exception e){
			CONST.log.error("Parse xml error: ", e);
			return null;
		}
		finally
		{
			if (sreader!=null)sreader.close();
		}
	}
}
