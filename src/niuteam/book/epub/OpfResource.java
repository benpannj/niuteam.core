package niuteam.book.epub;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.core.StringResource;
import niuteam.book.core.ZipEntryResource;
import niuteam.util.IOUtil;
import niuteam.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class OpfResource {
	private String base_path = "";
	private String bkid_name = null;
	private String ncx_href = null;
	private String opf_href = null;
	private ZipFile zf=null;
	private boolean dirty = false;
	// items
	private Hashtable<String, Resource> items = null;
	private List<String> zf_items = null;
	// xml 
	private Document docOpf;
	private Element elmMeta;
	private Element elmManifest;
	private Element elmSpine, elmGuide;
	
	public boolean isDirty(){return dirty;}
	
	public void readXml(String opf_href1, Document doc1, ZipFile zf_epub){
		this.zf = zf_epub;
		this.opf_href = opf_href1;
		int pos = opf_href.lastIndexOf("/");
		if (pos != -1){
			base_path = opf_href.substring(0, pos+1);
		}
		this.docOpf = doc1;
		items = new Hashtable<String, Resource>();
		if (zf != null) {
			zf_items = new ArrayList<String>();
			for (Enumeration entries = zf.entries();entries.hasMoreElements();) {
				ZipEntry ze = (ZipEntry)entries.nextElement();
				String name = ze.getName();
				if ( ze.isDirectory() ){
					continue;
				}
				if (name.equals(CONST.FILE_ROOT)){
					continue;
				} else if (name.equals(CONST.FILE_INFO)){
					continue;
				} else if (name.equals(ncx_href)){
					continue;
				} else if (name.equals(opf_href)){
					continue;
				}else if (name.equals("iTunesArtwork") || name.equals("iTunesMetadata.plist")){
					continue;
				}else if (name.equals("shucang.xml")){
					// ADD 20130120
					continue;
				}else if (name.endsWith(".ttf")){
					continue;
				}

				if (base_path!=null && base_path.length()>0){
					pos = name.indexOf(base_path);
					if (pos != -1){
						name = name.substring(pos+base_path.length());
					}
				}
				zf_items.add(name);
			}
		}
		// package
		Element elmPkg = this.docOpf.getDocumentElement();
		String ns = elmPkg.getNamespaceURI();
		if (!CONST.NS_OPF.equals(ns)){
			CONST.log.info("bad name space!:  "+ ns);
			if (elmPkg.hasAttribute("mlns")){
				elmPkg.removeAttribute("mlns");
			}
			elmPkg.setAttribute("xmlns", CONST.NS_OPF);
			dirty = true;
			try {
			String sss = XmlUtil.node2String(this.docOpf);
			this.docOpf = XmlUtil.string2Document(sss);
			elmPkg = this.docOpf.getDocumentElement();
			ns = elmPkg.getNamespaceURI();
			if (!CONST.NS_OPF.equals(ns)){
				CONST.log.info("still bad name space!:  "+ sss);
				int pos2 = sss.indexOf("package xmlns=\"\"");
				if (pos2!=-1){
					pos2 += "package xmlns=\"\"".length()-1;
					String new_s = sss.substring(0, pos2) + CONST.NS_OPF + sss.substring(pos2);
					this.docOpf = XmlUtil.string2Document(new_s);
					elmPkg = this.docOpf.getDocumentElement();
					ns = elmPkg.getNamespaceURI();
					CONST.log.info("final bad name space!: "+ ns);
				}
			}
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
		boolean meta_id= false;
		elmMeta = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"metadata").item(0);
		if (elmMeta == null){
			elmMeta = (Element) elmPkg.getElementsByTagName("metadata").item(0);	
		}
		Node nd = elmMeta.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				String value = XmlUtil.getTextContent(elm);
				if (CONST.DCTags.identifier.equals(key)){
					// key attr
					meta_id = true;
					String s = elm.getAttributeNS("", "id"); // CONST.NAMESPACE_OPF
					if (!bkid_name.equals(s)){
						CONST.log.info("meta:  id  - "+s+", "+ bkid_name );
						elm.setAttribute("id", bkid_name);
						dirty = true;
					}
					// bkid_val = value;
//					CONST.log.info("meta:  id  - {}", attr );
				} else if (CONST.DCTags.date.equals(key)){
					if (value == null || value.length()<4){
						elm.setTextContent("2011");
					}
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
		if (!meta_id){
			String bk_uid = "ID:";
			setMetadata(CONST.DCTags.identifier, bk_uid);
		}
		// Manifest
		List l = new ArrayList();
		Element elmRemove = null;
		int count = 0;
		elmManifest = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"manifest").item(0);
		if (elmManifest == null){
			elmManifest = (Element) elmPkg.getElementsByTagName("manifest").item(0);	
		}
		for ( nd = elmManifest.getFirstChild(); nd!=null; nd = nd.getNextSibling()){
			if (!(nd instanceof Element)) continue;
			Element elm = (Element)nd;
			String key = elm.getLocalName();
			if (!"item".equals(key)){
				CONST.log.warn(" not item ? "+ key);
			}
			String type = elm.getAttribute("media-type");
			String item_href = elm.getAttribute("href");
			String id = elm.getAttribute("id");

			// <item id="ncx" href="toc.ncx" media-type="text/xml" />
			// <item id="ncx" href="fb.ncx" media-type="application/xhtml+xml"/>
			
			if (CONST.MIME.NCX.equals(type)){
				//
				ncx_href = item_href;
			} else if ("text/xml".equals(type) && "toc.ncx".equals(item_href )){
				ncx_href = item_href;
				elm.setAttribute("media-type", CONST.MIME.NCX);
				dirty = true;
			} else if ("ncx".equals(id) && "fb.ncx".equals(item_href )){
				ncx_href = item_href;
				elm.setAttribute("media-type", CONST.MIME.NCX);
				dirty = true;
			} else {
				
			}
			if (item_href == null || item_href.length() == 0){
				elm.removeAttribute("href");
				elmRemove = elm;
				dirty = true;
				continue;
			}else if (l.contains(item_href)){
				CONST.log.info("remove item:  "+id+"  - "+ item_href );
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
			
			// ADD for long file path
			if (CONST.MIME.HTM.equals(type ) && item_href.length()>26){
				int i = item_href.lastIndexOf("/");
				StringBuffer buf = new StringBuffer();
				if (i > 0){
					buf.append(item_href.substring(0, i+1));
				}
				buf.append("bk_").append(count).append(".htm");
				String n_href = buf.toString();// "bk_"+count +".htm"; // +item_href.substring(item_href.length() -13 );
				res.setHref(n_href);
				elm.setAttribute("href", n_href);
			}
			items.put(id, res);
//			String f_path = base_path + item_href;
			if (zf_items!= null && zf_items.contains(item_href) ){
				zf_items.remove(item_href);
			}
			// check duplicate href
			count++;
		}
		if ( elmRemove != null){
			elmManifest.removeChild(elmRemove);
		}
		l.clear();
		elmSpine = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"spine").item(0);
		if (elmSpine == null){
			elmSpine = (Element) elmPkg.getElementsByTagName("spine").item(0);	
		}
		String ncx_id = elmSpine.getAttribute("toc");
//		CONST.log.info("ncx_id:  {} ",  ncx_id );
		nd = elmSpine.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				if (!"itemref".equals(key)){
					CONST.log.warn(" not itemref ?  "+ key);
				}
				// String idref = elm.getAttribute("idref");
				// CONST.log.info("item:  {} ",  idref );
			}
			nd = nd.getNextSibling();
		}
		elmGuide = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"guide").item(0);
		if (elmGuide != null){
			
		}
		if (zf_items!= null) {
			for (String s : zf_items){
				String id = s;
				ZipEntryResource res = new ZipEntryResource(id);
				res.loadEntry(zf, s, Resource.determineMediaType(s), base_path);
				addItem(res);
//				CONST.log.info("More : " + s);
			}
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
			CONST.log.error("ERROR: exist: " + id);
			// res.setId();
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
		List l = new ArrayList();
		for (Node nd = elmManifest.getFirstChild(); nd != null; nd = nd.getNextSibling() ){
			if (! (nd instanceof Element)) continue;
			Element elm = (Element)nd;
			String key = elm.getLocalName();
			if (!"item".equals(key)){
				CONST.log.warn(" not item ? "+ key);
				continue;
			}
			if (!elm.hasAttribute("href")){
				CONST.log.warn(" not item  href ? "+ key);
				continue;
				
			}
			String id = elm.getAttribute("id");
			String type = elm.getAttribute("media-type");
			String item_href = elm.getAttribute("href");
			if (item_href.length() < 2) continue;
			// skip ncx
			if (ncx_href.equals(item_href)) continue;

			String item_path = base_path + item_href;

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
				ZipEntry ze = new ZipEntry( item_path);
				try {
				resultStream.putNextEntry(ze);
				IOUtil.copy(ins, resultStream);
				} catch (Exception e) {
					CONST.log.error("BAD write item  "+id+", "+ item_href +","+ e.getMessage());
					
				}
				ins.close();
			} else {
				CONST.log.info("empty ins!  item:  "+type+"  - "+ item_href );
			}
		}
	}
	
	public void compact() throws Exception{
		dirty = true;
		Resource prev_res = null;
		List l = new ArrayList();
		for (Node nd = elmSpine.getFirstChild(); nd != null; nd = nd.getNextSibling() ){
			if (! (nd instanceof Element)) continue;
			Element elm = (Element)nd;
			//<itemref idref="id250829" />
			String key = elm.getLocalName();
			String id = elm.getAttribute("idref");
			if ("itemref".equals(key) && id != null && id.length()>1){
			} else {
				CONST.log.warn(" not itemref ? "+ key);
				l.add(elm);
				continue;
			}
//			if ("page".equals(id)){
//				CONST.log.warn("  itemref {}", id);
//			}
			Resource res = items.get(id);
			if (res == null){
				l.add(elm);
			} else{
				long size = res.getSize();
				if (size == 0) continue;
				if (res instanceof ZipEntryResource){
					ZipEntryResource zres = (ZipEntryResource)res;
					zres.clean();
					size = zres.getSize();
				}else if (res instanceof StringResource){
					prev_res = null;
					continue;
				}
				if (size > CONST.HUGE_SIZE){
					List<String> list = res.split();
					int i = 0;
					for (String s : list){
						i++;
					
						String sid = res.getId()+"_"+String.format("%03d", i);
						CONST.log.info("huge size " + sid + ", " + size);
						StringResource res_s = new StringResource(sid, null);
						res_s.loadString(s);
						addItem(res_s);
					}
					prev_res = null;
				} else if (prev_res == null) {
					prev_res = res;
				} else if (res.getId().startsWith("_test_")){
					prev_res = res;
				} else {
					int offset = 5;
					if (prev_res.mergeSameTitle(res, offset)){
						l.add(elm);
						items.remove(id);
					} else {
						prev_res = res;
					}
					if (prev_res.getSize() > 120000) {
						prev_res = null;
					}
				}
			}
		}
		if (prev_res != null){
//			prev_res.append("");
		}
		for (Iterator i = l.iterator(); i.hasNext();){
			Element e = (Element)i.next();
			elmSpine.removeChild(e);
		}
		l.clear();
		for (Node nd = elmManifest.getFirstChild(); nd != null; nd = nd.getNextSibling() ){
			if (! (nd instanceof Element)) continue;
			Element elm = (Element)nd;
			String item_href = elm.getAttribute("href");
			String id = elm.getAttribute("id");
			String type = elm.getAttribute("media-type");
			if (CONST.MIME.HTM.equals(type) ){
				if ( items.get(id) == null ){
					l.add(elm);
				}
			}else if (CONST.MIME.JPG.equals(type)){
				if (item_href.endsWith("coay.jpg") || item_href.endsWith("cover.jpg")){
					// cover.jpg
					l.add(elm);
					items.remove(id);
				}
				// coay.jpg
			}else if (CONST.MIME.CSS.equals(type)){
				// replace css with main.css
				Resource r = items.get(id);
				if (r instanceof ZipEntryResource){
					((ZipEntryResource) r).replaceCss();
				}
			}else {
				
			}
		}		
		for (Iterator i = l.iterator(); i.hasNext();){
			Element e = (Element)i.next();
			elmManifest.removeChild(e);
		}
		if (elmGuide != null) {
		Node nd = elmGuide.getFirstChild();
		while (nd != null){
			Node n = nd;
			nd = nd.getNextSibling();
			elmGuide.removeChild(n);
		}
		}
		
	}
	public boolean has(String id){
		return items.containsKey(id);
	}
	
	public void checkExist(){
		for (Enumeration entries = zf.entries();entries.hasMoreElements();) {
			ZipEntry ze = (ZipEntry)entries.nextElement();
			String name = ze.getName();
//			ze.isDirectory()
			if (name.equals(CONST.FILE_ROOT)){
				continue;
			} else if (name.equals(CONST.FILE_INFO)){
				continue;
			} else if (name.equals(ncx_href)){
				continue;
			} else if (name.equals(opf_href)){
				continue;
			}
	//		resultStream.putNextEntry(ze);
	//		InputStream ins = zf.getInputStream(ze);
	//		IOUtil.copy(ins, resultStream);
	//		ins.close();
//			ZipEntryResource res = new ZipEntryResource(item_href);
//			res.loadEntry(zf, id, type);
		}
	}
}
