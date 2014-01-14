package niuteam.book.epub;

import java.util.zip.ZipOutputStream;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NcxResource {
	private Document docNcx;
	private Element elmMeta, elmMetaUid, elmNav;
	private boolean dirty = false;
	
	public void readXml(Document doc){
		this.docNcx = doc;
		// ncx
		Element elmNcx = doc.getDocumentElement();
		String ns = elmNcx.getNamespaceURI();
		if (!CONST.NS_NCX.equals(ns)){
			CONST.log.info("bad name space!:  "+ ns);
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
//				String key = elm.getLocalName();
//				String value = XmlUtil.getTextContent(elm);
//				CONST.log.info("meta:  {}  - {}", key, value );
			}
			nd = nd.getNextSibling();
		}
		// docTitle
		// navMap / navPoint/ navLabel / text
		elmNav = (Element) elmNcx.getElementsByTagName("navMap").item(0);

	}
	public void setUid(String val){
		if (elmMetaUid != null){
			elmMetaUid.setAttribute("content", val);
		}
	}
	public void setTitle(String val){
		
	}
	public Document getDoc(){return docNcx;}
	public void compact() throws Exception{
		this.dirty = true;
		if (elmNav == null) return;
//		elmNav.getParentNode().removeChild(elmNav);
		
		Node nd = elmNav.getFirstChild();
		while (nd != null){
			Node n = nd;
			nd = nd.getNextSibling();
			elmNav.removeChild(n);
		}
		addNav("Main", null);
	// <navPoint id="navPoint-1" playOrder="1">
//	      <navLabel>
//	        <text>Main</text>
//	      </navLabel>
//		<content src="Text/c_00.htm"/>
//	    </navPoint>
//		String s = XmlUtil.node2String(docNcx);
//		CONST.log.info(" cpt "  + s);
	}
	private int seq = 1;
	private void addNav(String txt, String href){
		Element e_p = docNcx.createElement("navPoint");
		e_p.setAttribute("id", "id" + seq);
		e_p.setAttribute("playOrder", "" + seq);
		seq++;
		elmNav.appendChild(e_p);
		Element e_l = docNcx.createElement("navLabel");
		Element e_t = docNcx.createElement("text");
//		e_t.appendChild(docNcx.createTextNode(txt));
		e_t.setTextContent(txt);
		e_l.appendChild(e_t);
		e_p.appendChild(e_l);
		Element e_c = docNcx.createElement("content");
		if (href == null){
			href ="";
//			href = "bk_1.htm";
		}
		e_c.setAttribute("src", href);
		e_p.appendChild(e_c);
		
	}
	public void addItem(Resource res){
		if (CONST.MIME.HTM.equals( res.getMediaType() ) ) {
			dirty = true;
			String href = res.getHref();
			String title = res.getTitle();
			if (title == null){
				title = res.getId();
			}
			addNav(title, href);
		}
	}	
}
