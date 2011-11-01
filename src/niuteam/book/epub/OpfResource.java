package niuteam.book.epub;

import niuteam.book.core.CONST;
import niuteam.book.core.Metadata.DCTags;
import niuteam.util.DomUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class OpfResource {
	private String bkid_val;
	private String ncx_href;
	public void readXml(Document doc){
		// package
		Element elmPkg = doc.getDocumentElement();
		String bkid_name = elmPkg.getAttribute("unique-identifier");
		if (bkid_name == null){
			bkid_name = CONST.BOOK_ID_ID;
			elmPkg.setAttribute("unique-identifier", bkid_name);
		}
		Element elmMeta = (Element) elmPkg.getElementsByTagName("metadata").item(0);
		Node nd = elmMeta.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				String value = DomUtil.getTextContent(elm);
				if (DCTags.identifier.equals(key)){
					// key attr
					String s = elm.getAttributeNS("", "id"); // CONST.NAMESPACE_OPF
					if (!bkid_name.equals(s)){
						CONST.log.info("meta:  id  - {}, {}", s, bkid_name );
						elm.setAttribute("id", bkid_name);
					}
					bkid_val = value;
//					CONST.log.info("meta:  id  - {}", attr );
				} else {
					CONST.log.info("meta:  {}  - {}", key, value );
					// datas.put(key, value);
					if (elm.hasAttributes()){
						CONST.log.info("has attrs   - {}", key );
					}
				}
			}
			nd = nd.getNextSibling();
		}
		// Manifest
		Element elmManifest = (Element) elmPkg.getElementsByTagName("manifest").item(0);
		nd = elmManifest.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				if (!"item".equals(key)){
					CONST.log.warn(" not item ? {}", key);
				}
				String type = elm.getAttribute("media-type");
				String href = elm.getAttribute("href");
				String id = elm.getAttribute("id");
				CONST.log.info("item:  {}  - {}", type, href );
				if (CONST.MIME_NCX.equals(type)){
					//
					ncx_href = href;
				}
			}
			nd = nd.getNextSibling();
		}
		
		Element elmSpine = (Element) elmPkg.getElementsByTagName("spine").item(0);
		String ncx_id = elmSpine.getAttribute("toc");
		CONST.log.info("ncx_id:  {} ",  ncx_id );
		nd = elmSpine.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				if (!"itemref".equals(key)){
					CONST.log.warn(" not itemref ? {} ", key);
				}
				String href = elm.getAttribute("idref");
				CONST.log.info("item:  {} ",  href );
			}
			nd = nd.getNextSibling();
		}
		Element elmGuide = (Element) elmPkg.getElementsByTagName("guide").item(0);
		if (elmGuide != null){
			
		}
	}
	public String getNcx(){return ncx_href;}
}
