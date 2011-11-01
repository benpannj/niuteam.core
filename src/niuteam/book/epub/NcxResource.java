package niuteam.book.epub;

import niuteam.book.core.CONST;
import niuteam.util.DomUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NcxResource {
	public void readXml(Document doc){
		
		// ncx
		Element elmNcx = doc.getDocumentElement();
		// head
		Element elmMeta = (Element) elmNcx.getElementsByTagName("head").item(0);
		Node nd = elmMeta.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				String value = DomUtil.getTextContent(elm);
				CONST.log.info("meta:  {}  - {}", key, value );
			}
			nd = nd.getNextSibling();
		}
		// docTitle
		// navMap / navPoint/ navLabel / text
	}
}
