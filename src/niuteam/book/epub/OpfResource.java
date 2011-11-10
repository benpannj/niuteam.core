package niuteam.book.epub;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.core.ZipEntryResource;
import niuteam.util.IOUtil;
import niuteam.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OpfResource {
	private String base_path = "";
	private String bkid_name = null;
	private String ncx_href = null;
	private String opf_href = null;

	private boolean dirty = false;
	// items
	private Hashtable<String, Resource> items = null;
	// xml 
	private Document docOpf;
	private Element elmMeta;
	private Element elmManifest;
	private Element elmSpine;
	
	public boolean isDirty(){return dirty;}
	
	public void readXml(String opf_href1, Document doc1, ZipFile zf){
		this.opf_href = opf_href1;
		int pos = opf_href.lastIndexOf("/");
		if (pos != -1){
			base_path = opf_href.substring(0, pos+1);
		}
		this.docOpf = doc1;
		items = new Hashtable<String, Resource>();

		// package
		Element elmPkg = this.docOpf.getDocumentElement();
		String ns = elmPkg.getNamespaceURI();
		if (!CONST.NS_OPF.equals(ns)){
			CONST.log.info("bad name space!: {} ", ns);
			elmPkg.setAttribute("xmlns", CONST.NS_OPF);
			if (elmPkg.hasAttribute("mlns")){
				elmPkg.removeAttribute("mlns");
				dirty = true;
			}
			try {
			String sss = XmlUtil.node2String(this.docOpf);
			this.docOpf = XmlUtil.string2Document(sss);
			elmPkg = this.docOpf.getDocumentElement();
			} catch (Exception e){
				
			}
		} else {
			String pre = elmPkg.getPrefix();
			if (pre !=null && pre.length() > 1) {
				try {
					String sss = XmlUtil.node2String(this.docOpf);
					sss = sss.replaceAll("<"+pre+":", "<");
					sss = sss.replaceAll("</"+pre+":", "</");
					// xmlns:opf=
					sss = sss.replaceAll("xmlns:"+pre+"=", "xmlns=");
					CONST.log.info(sss);
					
					this.docOpf = XmlUtil.string2Document(sss);
					elmPkg = this.docOpf.getDocumentElement();
				} catch (Exception e){
						
				}
//				elmPkg.setPrefix(null);
//				this.docOpf.
				dirty = true;
			}
		}
//		elmPkg.setAttribute(name, value)
		bkid_name = elmPkg.getAttribute("unique-identifier");
		if (bkid_name == null){
			bkid_name = CONST.BOOK_ID_ID;
			elmPkg.setAttribute("unique-identifier", bkid_name);
			dirty = true;
		}
		// metadata
		elmMeta = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"metadata").item(0);
		Node nd = elmMeta.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				String value = XmlUtil.getTextContent(elm);
				if (CONST.DCTags.identifier.equals(key)){
					// key attr
					String s = elm.getAttributeNS("", "id"); // CONST.NAMESPACE_OPF
					if (!bkid_name.equals(s)){
						CONST.log.info("meta:  id  - {}, {}", s, bkid_name );
						elm.setAttribute("id", bkid_name);
						dirty = true;
					}
					// bkid_val = value;
//					CONST.log.info("meta:  id  - {}", attr );
				} else {
					//CONST.log.info("meta:  {}  - {}", key, value );
					// datas.put(key, value);
					if (elm.hasAttributes()){
					//	CONST.log.info("has attrs   - {}", key );
					}
				}
			}
			nd = nd.getNextSibling();
		}
		// Manifest
		List l = new ArrayList();
		Element elmRemove = null;
		elmManifest = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"manifest").item(0);
		for ( nd = elmManifest.getFirstChild(); nd!=null; nd = nd.getNextSibling()){
			if (!(nd instanceof Element)) continue;
			Element elm = (Element)nd;
			String key = elm.getLocalName();
			if (!"item".equals(key)){
				CONST.log.warn(" not item ? {}", key);
			}
			String type = elm.getAttribute("media-type");
			String item_href = elm.getAttribute("href");
			String id = elm.getAttribute("id");

			// <item id="ncx" href="toc.ncx" media-type="text/xml" />
			
			if (CONST.MIME.NCX.equals(type)){
				//
				ncx_href = item_href;
			} else if ("text/xml".equals(type) && "toc.ncx".equals(item_href )){
				ncx_href = item_href;
				elm.setAttribute("media-type", CONST.MIME.NCX);
				dirty = true;
			}
			if (l.contains(item_href)){
				CONST.log.info("remove item:  {}  - {}",id , item_href );
//				elm.setAttribute("href", "");
				elm.removeAttribute("href");
				elmRemove = elm;
				dirty = true;
				//elmManifest.removeChild(elm);
				continue;
			}
			l.add(item_href);
			// add zip res
			ZipEntryResource res = new ZipEntryResource(id);
			res.loadEntry(zf, item_href, type, base_path);
			items.put(id, res);
			// check duplicate href
		}
		if ( elmRemove != null){
			elmManifest.removeChild(elmRemove);
		}
		l.clear();
		elmSpine = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"spine").item(0);
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
				// String idref = elm.getAttribute("idref");
				// CONST.log.info("item:  {} ",  idref );
			}
			nd = nd.getNextSibling();
		}
		Element elmGuide = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"guide").item(0);
		if (elmGuide != null){
			
		}
	}
	public String getNcx(){return base_path + ncx_href;}
	public String getOpfHref(){
		return opf_href;
	}
	public Document getDoc(){return docOpf;}
	
	public void addItem(Resource res){
		dirty = true;
		String id = res.getId();
		boolean exist = items.containsKey(id);
		if (exist){
			// remove old.
		}
		items.put(id, res);
		// 
		Element oldChild = null, newChild;
		// <item href="Text/c_00.htm" id="c_00.htm" media-type="application/xhtml+xml"/>
		newChild = docOpf.createElementNS(CONST.NS_OPF, "item");
		newChild.setAttribute("href", res.getHref());
		newChild.setAttribute("id", id);
		String type = res.getMediaType();
		newChild.setAttribute("media-type", type);
		this.elmManifest.appendChild(newChild);
		// <itemref idref="c_00.htm"/>
		if (CONST.MIME.HTM.equals( type)) {
		newChild = docOpf.createElementNS(CONST.NS_OPF, "itemref");
		newChild.setAttribute("idref", id);
		elmSpine.appendChild(newChild);
		}
	}
	public void setMetadata(String key, String val){
		dirty = true;
		boolean dc_meta = !CONST.DCTags.meta.equals(key);
		// no name space
		Element oldChild = null, newChild;
//		docOpf.createElementNS(namespaceURI, qualifiedName)
		newChild = docOpf.createElementNS(CONST.NS_DC, key);
		if (dc_meta) {
			newChild.setPrefix(CONST.PREFIX_DC);
			newChild.appendChild(docOpf.createTextNode(val));
			// <dc:date opf:event="publication">2011-08-30</dc:date>
		} else {
			newChild.setAttribute("name", "EXT");
			newChild.setAttribute("content", val);
		}
		if (CONST.DCTags.identifier.equals(key)){
			newChild.setAttribute("id", bkid_name);
			// also set ncx dtb:id
		}
		
		NodeList nl = elmMeta.getElementsByTagNameNS(CONST.NS_DC, key);
		if (nl != null && nl.getLength() > 0){
			oldChild = (Element)nl.item(0);
			elmMeta.replaceChild(newChild, oldChild);
		} else {
			elmMeta.appendChild(newChild);
		}
//		try {
//		CONST.log.info(DomUtil.node2String(docOpf) );
//		}catch (Exception e) {}
	}
	public void writeItem(ZipOutputStream resultStream) throws Exception{
		dirty = false;
		for (Node nd = elmManifest.getFirstChild(); nd != null; nd = nd.getNextSibling() ){
			if (! (nd instanceof Element)) continue;
			Element elm = (Element)nd;
			String key = elm.getLocalName();
			if (!"item".equals(key)){
				CONST.log.warn(" not item ? {}", key);
				continue;
			}
			if (!elm.hasAttribute("href")){
				CONST.log.warn(" not item  href ? {}", key);
				continue;
				
			}
			String id = elm.getAttribute("id");
			String type = elm.getAttribute("media-type");
			String item_href = elm.getAttribute("href");
			if (item_href.length() < 2) continue;
			// skip ncx
			if (ncx_href.equals(item_href)) continue;

			String item_path = base_path + item_href;

			CONST.log.info(" write item  {}, {}", id, item_href);
//				CONST.log.warn(" write item  {} {} {}", id, item_href, type);

			InputStream ins = null;
			Resource res = items.get(id);
			if (res == null){
			} else if (res instanceof ZipEntryResource){
//				String href =res.getHref();
//				ZipEntry ze = new ZipEntry(base_path + href);
//				ins = zf.getInputStream(ze);
				ins = res.getInputStream();
			} else {
				ins = res.getInputStream();
			}
			if (ins == null){
				// try load from template
				ins = IOUtil.loadTemplate(item_path);
			}
			if (ins != null) {
				resultStream.putNextEntry(new ZipEntry( item_path));
				IOUtil.copy(ins, resultStream);
				ins.close();
			} else {
				CONST.log.info("empty ins!  item:  {}  - {}", type, item_href );
			}
		}
	}
}
