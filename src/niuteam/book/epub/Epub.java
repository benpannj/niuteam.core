package niuteam.book.epub;

import java.awt.print.Book;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import niuteam.book.core.CONST;
import niuteam.book.core.FileResource;
import niuteam.book.core.Resource;
import niuteam.book.core.StringResource;
import niuteam.book.core.ZipEntryResource;
import niuteam.util.IOUtil;
import niuteam.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Epub {
	private Book book;
//	private ZipFile zf = null;
	// 
	private Document docContainer=null;
	private OpfResource opf;
	private NcxResource ncx;
	
	public boolean isDirty(){return opf.isDirty();}
	
	public ZipFile readEpub(File epub) throws IOException {
//		private ZipFile zf = null;;
		ZipFile zf = new ZipFile(epub);
		ZipEntry ze;
		// mimetype
//		ZipEntry ze = new ZipEntry(CONST.FILE_ROOT);
		ze = zf.getEntry(CONST.FILE_ROOT);
		if (ze == null) {
			CONST.log.warn("not valid epub !  "+ epub.getAbsolutePath());
			return zf;
		}
		//
		ze = new ZipEntry(CONST.FILE_INFO);
		docContainer = XmlUtil.stream2doc(zf.getInputStream(ze));
		Element rootFileElement = (Element) ((Element) docContainer.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
		String opf_href = rootFileElement.getAttribute("full-path");
		// OPF file
		ze = new ZipEntry(opf_href);
		InputStream ins = zf.getInputStream(ze);
		Document docOpf= null;
		docOpf = XmlUtil.stream2doc(ins);
		if (docOpf == null) {
			ins = zf.getInputStream(ze);
			byte[] bs = IOUtil.toByteArray(ins);
			String s = new String(bs);
			s = s.replaceAll("&nbsp;", "");
			s = s.replaceAll("xsi:type", "opf:event");
			docOpf = XmlUtil.string2Document(s);
		}
		this.opf = new OpfResource();
		opf.readXml(opf_href, docOpf, zf);
		// NCX
		Document docNcx = null;
		String ncx_href = opf.getNcx();
		if (ncx_href != null) {
			ze = new ZipEntry(ncx_href);
			docNcx = XmlUtil.stream2doc(zf.getInputStream(ze));
		}
		if (docNcx == null){
			CONST.log.error("[ERROR] bad ncx file: "+ncx_href+", epub:  "+  epub.getAbsoluteFile() );
//			this.ncx = new NcxResource();
		} else {
			this.ncx = new NcxResource();
			ncx.readXml(docNcx);
		}
//		for (Enumeration entries = zf.entries();entries.hasMoreElements();) {
//			ze = (ZipEntry)entries.nextElement();
//			String name = ze.getName();
//			if (name.equals(CONST.FILE_ROOT)){
//				continue;
//			} else if (name.equals(CONST.FILE_INFO)){
//				continue;
//			} else if (name.equals(ncx_href)){
//				continue;
//			} else if (name.equals(opf_href)){
//				continue;
//			}
////			resultStream.putNextEntry(ze);
////			InputStream ins = zf.getInputStream(ze);
////			IOUtil.copy(ins, resultStream);
////			ins.close();
//			ZipEntryResource res = new ZipEntryResource(item_href);
//			res.loadEntry(zf, id, type);
//
//		}
		return zf;
	}
	public void writeEpub(File outFile) throws Exception {
		OutputStream out = new FileOutputStream(outFile);
		
		ZipOutputStream resultStream = new ZipOutputStream(out);
		// FILE_ROOT
		writeMimeType(resultStream);
		// FILE_INFO
		if (this.docContainer == null){
			resultStream.putNextEntry(new ZipEntry(CONST.FILE_INFO));
			InputStream ins = IOUtil.loadTemplate(CONST.FILE_INFO);
			IOUtil.copy(ins, resultStream);
			ins.close();
//			Document docContainer = DomUtil.stream2doc(Template.load(CONST.FILE_INFO));
//			writeXml(CONST.FILE_INFO, docContainer, resultStream);
		} else {
			writeXml(CONST.FILE_INFO, docContainer, resultStream);
		}
		// opf
		String opf_href = opf.getOpfHref();
		writeXml(opf_href, opf.getDoc(), resultStream);
		// ncx
		String ncx_href = opf.getNcx();
		writeXml(ncx_href, ncx.getDoc(), resultStream);
		// write opf items.
		this.opf.writeItem(resultStream);
		// close
		resultStream.close();
		out.close();
	}
	private void writeXml(String href, Document doc, ZipOutputStream resultStream)
			throws IOException {
		if(doc == null) {
			return;
		}
		try {
			resultStream.putNextEntry(new ZipEntry(href));
			XmlUtil.node2Stream(doc, resultStream);
			
		} catch(Exception e) {
			CONST.log.error(e.getMessage(), e);
		}
	}	
	public void create(String title, String auth, String lang){
		this.opf = new OpfResource();
		Document docOpf = XmlUtil.stream2doc(IOUtil.loadTemplate(CONST.FILE_OPF));
		opf.readXml(CONST.FILE_OPF, docOpf, null);
		opf.setMetadata(CONST.DCTags.title, title);
		opf.setMetadata(CONST.DCTags.language, lang);

		String format = "yyyyMMdd-HHmmss";
		SimpleDateFormat f = new SimpleDateFormat(format);
		String s_now = f.format(new Date());
		String bk_uid = "ID:"+s_now;
		// ISDB 13 bit ISBN 978-7-98181-728-6;
		opf.setMetadata(CONST.DCTags.identifier, bk_uid);
		
		// optional meta
		opf.setMetadata(CONST.DCTags.creator, auth);
		opf.setMetadata(CONST.DCTags.date, s_now.substring(0, 4));
		
		this.ncx = new NcxResource();
		Document docNcx = XmlUtil.stream2doc(IOUtil.loadTemplate(CONST.FILE_NCX));
		ncx.readXml(docNcx);
		ncx.setUid(bk_uid);
		ncx.setTitle(title);
		// 
	}

	public void setMetadata(String key, String val){
		this.opf.setMetadata(key, val);
	}
	

	// root mimetype
	private void writeMimeType(ZipOutputStream resultStream) throws IOException {
		ZipEntry mimetypeZipEntry = new ZipEntry(CONST.FILE_ROOT);
		mimetypeZipEntry.setMethod(ZipEntry.STORED);
		
		byte[] mimetypeBytes = CONST.MIME.EPUB.getBytes();
		mimetypeZipEntry.setSize(mimetypeBytes.length);
		mimetypeZipEntry.setCrc(calculateCrc(mimetypeBytes));
		resultStream.putNextEntry(mimetypeZipEntry);
		resultStream.write(mimetypeBytes);
	}	
	private long calculateCrc(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return crc.getValue();
	}

	public void addResource(Resource res) throws Exception{
		long size = res.getSize();
		if (size > CONST.HUGE_SIZE){
			List<String> list = res.split();
			int i = 0;
			for (String s : list){
				i++;
				
				String href = res.getId()+"_"+String.format("%03d", i)+".htm";
				CONST.log.info("huge size " + href);
				StringResource res_s = new StringResource(href, null);
				res_s.loadString(s);
				addResource(res_s);
			}
			// spilit
		}
		
		opf.addItem(res);
		ncx.addItem(res);
	}

	// add local file as item
	public void addItem(File f) throws Exception{
		if (!f.exists() ) return;
		String name = f.getName();
		FileResource res = new FileResource(name);
		res.loadFile(f);
		opf.addItem(res);
		ncx.addItem(res);
	}
	public void addString(String href, String title, String data) throws Exception{
		StringResource res = new StringResource(href, title);
		res.loadString(data);
		opf.addItem(res);
		ncx.addItem(res);
	}
	public void addEpub(File epub, String prefix) throws IOException {
		ZipFile zf = new ZipFile(epub);
		ZipEntry ze;
		// mimetype
//		ZipEntry ze = new ZipEntry(CONST.FILE_ROOT);
		ze = zf.getEntry(CONST.FILE_ROOT);
		if (ze == null) {
			CONST.log.warn("not valid epub !  "+ epub.getAbsolutePath());
			return;
		}
		ze = new ZipEntry(CONST.FILE_INFO);
		Document doc = XmlUtil.stream2doc(zf.getInputStream(ze));
		Element rootFileElement = (Element) ((Element) doc.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
		String opf_href = rootFileElement.getAttribute("full-path");
		// OPF file
		ze = new ZipEntry(opf_href);
		Document docOpf = XmlUtil.stream2doc(zf.getInputStream(ze));
		int pos = opf_href.lastIndexOf("/");
		String base_path = "";
		if (pos != -1){
			base_path = opf_href.substring(0, pos+1);
		}

		Element elmPkg = docOpf.getDocumentElement();
		// use a map to store item for spine sort.
		Hashtable<String, Resource> map = new Hashtable<String, Resource>();
		Element elmManifest = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"manifest").item(0);
		if (elmManifest == null){
			elmManifest = (Element) elmPkg.getElementsByTagName("manifest").item(0);	
		}
		for (Node nd = elmManifest.getFirstChild(); nd!=null; nd = nd.getNextSibling()){
			if (!(nd instanceof Element)) continue;
			Element elm = (Element)nd;
			String key = elm.getLocalName();
			if (!"item".equals(key)){
				CONST.log.warn(" not item ? "+ key);
			}
			String type = elm.getAttribute("media-type");
			String item_href = elm.getAttribute("href");
			String id = elm.getAttribute("id");

			if (item_href== null || item_href.length() <2){
				continue;
			}else if (item_href.endsWith("ncx")){
				continue;
			} else if (item_href.endsWith("opf")){
				continue;
			} else if (item_href.endsWith("css")){
				continue;
			} else if (item_href.endsWith("cover.jpg") || item_href.endsWith("coay.jpg")){
				continue;
			} else if (item_href.endsWith("shucang.xml")){
				continue;
			}
			if (opf.has(id) ){
				if (prefix != null){
					id = prefix + id;
				} else {
					CONST.log.warn(" has item ? "+ item_href);
					continue;
				}
			}
			// <item id="ncx" href="toc.ncx" media-type="text/xml" />
			ZipEntryResource res = new ZipEntryResource(id);
			res.loadEntry(zf, item_href, type, base_path);
			if (prefix != null){
				res.setHref( prefix + item_href);
			}
//			opf.addItem(res);
			map.put(id, res);
		}
		Element elmSpine = (Element) elmPkg.getElementsByTagNameNS(CONST.NS_OPF,"spine").item(0);
		if (elmSpine == null){
			elmSpine = (Element) elmPkg.getElementsByTagName("spine").item(0);	
		}
		String ncx_id = elmSpine.getAttribute("toc");
//		CONST.log.info("ncx_id:  {} ",  ncx_id );
		Node nd = elmSpine.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				if (!"itemref".equals(key)){
					CONST.log.warn(" not itemref ?  "+ key);
					continue;
				}
				String idref = elm.getAttribute("idref");
				Resource r = map.get(idref);
				if (r==null && prefix != null){
					idref = prefix + idref;
					r = map.get(idref);
				}
				// CONST.log.info("item:  {} ",  idref );
				if (r!= null){
					opf.addItem(r);
					map.remove(idref);
				}else {
					
					CONST.log.warn(" bad item "+idref );
				}
			}
			nd = nd.getNextSibling();
		}
		if (map.size() > 0){
			for (String key : map.keySet()){
				Resource r = map.get(key);
				opf.addItem(r);
			}
			map.clear();
		}

//		for (Enumeration entries = zf.entries();entries.hasMoreElements();) {
//			ze = (ZipEntry)entries.nextElement();
//			String name = ze.getName();
//			if (name.equals(CONST.FILE_ROOT)){
//				continue;
//			} else if (name.equals(CONST.FILE_INFO)){
//				continue;
//			} else if (name.endsWith("ncx")){
//				continue;
//			} else if (name.endsWith("opf")){
//				continue;
//			} else if (name.endsWith("css")){
//				continue;
//			}
//			if ( ze.isDirectory() ){
//				continue;
//			}
////			resultStream.putNextEntry(ze);
////			InputStream ins = zf.getInputStream(ze);
////			IOUtil.copy(ins, resultStream);
////			ins.close();
//			String item_href = name;
//			int pos = name.lastIndexOf('/');
//			String id = pos == -1? name : name.substring(pos+1);
//			String base_path ="";
//			String type = Resource.determineMediaType(item_href);
//			ZipEntryResource res = new ZipEntryResource(id);
//			res.loadEntry(zf, item_href, type, base_path);
//			opf.addItem(res);
//		}
	}	
	public void compact() throws Exception{
		CONST.log.info(" compact begin: ");
		opf.compact();
		if (ncx == null){
			this.ncx = new NcxResource();
			Document docNcx = XmlUtil.stream2doc(IOUtil.loadTemplate(CONST.FILE_NCX));
			ncx.readXml(docNcx);
//			ncx.setUid(opf.getBkUid());
//			ncx.setTitle(title);
		} else {
			ncx.compact();
		}
	}

}
