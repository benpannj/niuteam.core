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
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import niuteam.book.core.CONST;
import niuteam.book.core.FileResource;
import niuteam.book.core.StringResource;
import niuteam.book.core.ZipEntryResource;
import niuteam.util.IOUtil;
import niuteam.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Epub {
	private Book book;
//	private boolean dirty = true;
	// 
	private Document docContainer=null;
	private OpfResource opf;
	private NcxResource ncx;
	
	public boolean isDirty(){return opf.isDirty();}
	
	public void readEpub(File epub) throws IOException {
//		private ZipFile zf = null;;
		ZipFile zf = new ZipFile(epub);
		ZipEntry ze;
		// mimetype
//		ZipEntry ze = new ZipEntry(CONST.FILE_ROOT);
		ze = zf.getEntry(CONST.FILE_ROOT);
		if (ze == null) {
			CONST.log.warn("not valid epub ! {} ", epub.getAbsolutePath());
			return;
		}
		//
		ze = new ZipEntry(CONST.FILE_INFO);
		docContainer = XmlUtil.stream2doc(zf.getInputStream(ze));
		Element rootFileElement = (Element) ((Element) docContainer.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
		String opf_href = rootFileElement.getAttribute("full-path");
		// OPF file
		ze = new ZipEntry(opf_href);
		Document docOpf = XmlUtil.stream2doc(zf.getInputStream(ze));
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
			CONST.log.error("[ERROR] bad ncx file: {}, epub:  {} ", ncx_href,  epub.getAbsoluteFile() );
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


	// add local file as item
	public void addItem(File f) throws Exception{
		if (!f.exists() ) return;
		String name = f.getName();
		FileResource res = new FileResource(name);
		res.loadFile(f);
		opf.addItem(res);
	}
	public void addString(String href, String data) throws Exception{
		StringResource res = new StringResource(href);
		res.loadString(data);
		opf.addItem(res);
	}
	
}
