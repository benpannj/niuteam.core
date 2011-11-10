package niuteam.book.epub;

import java.util.zip.ZipOutputStream;

import niuteam.book.core.CONST;
import niuteam.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NcxResource {
	private Document docNcx;
	private Element elmMeta, elmMetaUid;
	private boolean dirty = false;
	
	public void readXml(Document doc){
		this.docNcx = doc;
		// ncx
		Element elmNcx = doc.getDocumentElement();
		String ns = elmNcx.getNamespaceURI();
		if (!CONST.NS_NCX.equals(ns)){
			CONST.log.info("bad name space!: {} ", ns);
			elmNcx.setAttribute("xmlns", CONST.NS_NCX);
			if (elmNcx.hasAttribute("mlns")){
				elmNcx.removeAttribute("mlns");
				dirty = true;
			}
			try {
			String sss = XmlUtil.node2String(this.docNcx);
			this.docNcx = XmlUtil.string2Document(sss);
			elmNcx = this.docNcx.getDocumentElement();
			} catch (Exception e){
				
			}
		} else {
			String pre = elmNcx.getPrefix();
			if (pre !=null && pre.length() > 1) {
				try {
					String sss = XmlUtil.node2String(this.docNcx);
					sss = sss.replaceAll("<"+pre+":", "<");
					sss = sss.replaceAll("</"+pre+":", "</");
					// xmlns:opf=
					sss = sss.replaceAll("xmlns:"+pre+"=", "xmlns=");
					CONST.log.info(sss);
					
					this.docNcx = XmlUtil.string2Document(sss);
					elmNcx = this.docNcx.getDocumentElement();
				} catch (Exception e){
						
				}
//				elmPkg.setPrefix(null);
//				this.docOpf.
				dirty = true;
			}
			
		}
		// head
		elmMeta = (Element) elmNcx.getElementsByTagName("head").item(0);
		Node nd = elmMeta.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				if (elm.hasAttribute("name")){
					String n = elm.getAttribute("name");
					if ("dtb:uid".equals(n)){
						elmMetaUid = elm;
					}
				}
				String key = elm.getLocalName();
				String value = XmlUtil.getTextContent(elm);
				CONST.log.info("meta:  {}  - {}", key, value );
			}
			nd = nd.getNextSibling();
		}
		// docTitle
		// navMap / navPoint/ navLabel / text
	}
	public void setUid(String val){
		if (elmMetaUid != null){
			elmMetaUid.setAttribute("content", val);
		}
	}
	public void setTitle(String val){
		
	}
	public Document getDoc(){return docNcx;}
}
