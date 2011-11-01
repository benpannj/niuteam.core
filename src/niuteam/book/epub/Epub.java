package niuteam.book.epub;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import niuteam.book.core.Book;
import niuteam.book.core.CONST;
import niuteam.book.core.Metadata;
import niuteam.book.core.Resource;
import niuteam.util.DomUtil;
import niuteam.util.IOUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Epub {
	private Book book;
	public Book readEpub(File epub) throws IOException {
		book = new Book();
		ZipFile zf = new ZipFile(epub);
		ZipEntry ze;
		// mimetype
//		ZipEntry ze = new ZipEntry(CONST.FILE_ROOT);
		ze = zf.getEntry(CONST.FILE_ROOT);
		if (ze == null) {
			CONST.log.warn("not valid epub ! {} ", epub.getAbsolutePath());
			return null;
		}
		//
		ze = new ZipEntry(CONST.FILE_INFO);
		Document docContainer = DomUtil.stream2doc(zf.getInputStream(ze));
		Element rootFileElement = (Element) ((Element) docContainer.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
		String opf_href = rootFileElement.getAttribute("full-path");
		// OPF file
		String base_path = "";
		ze = new ZipEntry(opf_href);
		Document docOpf = DomUtil.stream2doc(zf.getInputStream(ze));
		OpfResource opf = new OpfResource();
		opf.readXml(docOpf);
		int pos = opf_href.lastIndexOf("/");
		if (pos != -1){
			base_path = opf_href.substring(0, pos+1);
		}
		// NCX
		String ncx_href = base_path + opf.getNcx();
		ze = new ZipEntry(ncx_href);
		Document docNcx = DomUtil.stream2doc(zf.getInputStream(ze));
		if (docNcx == null){
			CONST.log.error("no ncx file: {} ", ncx_href);
		} else {
		NcxResource ncx = new NcxResource();
		ncx.readXml(docNcx);
		}

		// String packageResourceHref = getPackageResourceHref(resources);
//		Resource packageResource = processPackageResource(packageResourceHref, result, resources);
//		result.setOpfResource(packageResource);
//		Resource ncxResource = processNcxResource(packageResource, result);
//		result.setNcxResource(ncxResource);
//		result = postProcessBook(result);
		return book;
	}

	public Book readEpub(ZipInputStream in, String encoding) throws IOException {
		book = new Book();
		readResources(in, encoding);
		// String packageResourceHref = getPackageResourceHref(resources);
//		Resource packageResource = processPackageResource(packageResourceHref, result, resources);
//		result.setOpfResource(packageResource);
//		Resource ncxResource = processNcxResource(packageResource, result);
//		result.setNcxResource(ncxResource);
//		result = postProcessBook(result);
		return book;
	}
	private void readResources(ZipInputStream in, String defaultHtmlEncoding) throws IOException {
		String opf_href = null;
		String ncx_href = null;
		for(ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in.getNextEntry()) {
			String name = zipEntry.getName();
			CONST.log.info(" name :  " + name );
			if(zipEntry.isDirectory()) {
				continue;
			} else if (CONST.FILE_ROOT.equals(name)){
				continue;
			} else if (CONST.FILE_INFO.equals(name)){
				try {
					Resource resource = new Resource(in, zipEntry.getName());
					Document document = DomUtil.getAsDocument(resource);
					Element rootFileElement = (Element) ((Element) document.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
					String result = rootFileElement.getAttribute("full-path");
//					CONST.log.info(" full path :  " + result );
					if (opf_href == null) {
						opf_href= result;
					} else if (opf_href.equals(result)){
						// do nothing
					} else {
						CONST.log.debug("not same ! opf name {} ,  {}", result, opf_href);
					}
				} catch (Exception e) {
					CONST.log.error(e.getMessage(), e);
				}
				
				continue;
			} else if (name.endsWith(".opf")){
				book.opf = new Resource(in, name);
				if (opf_href != null && !opf_href.equals(name)){
					CONST.log.debug("not same ! opf name {} ,  {}", name, opf_href);
//					book.opf.setHref(CONST.FILE_OPF);
				}
			} else if (name.endsWith(".ncx")){
				book.ncx = new Resource(in, name);
			} else {
				Resource resource = new Resource(in, name);
				book.resources.put(resource.getHref(), resource);
			}
		}
	}
	public Book create(String title, String auth, String lang){
		book = new Book();
		book.metadata.set(Metadata.DCTags.title, title);
		book.metadata.set(Metadata.DCTags.creator, auth);
		book.metadata.set(Metadata.DCTags.language, lang);
//		metadata.auth = auth;
//		metadata.title = title;
//		metadata.lang = lang;
		return book;
	}
	
	public void write(OutputStream out) throws IOException {
		ZipOutputStream resultStream = new ZipOutputStream(out);
		writeMimeType(resultStream);
		writeContainer(resultStream);
		// OEBPS/content.opf
		writeResource(book.opf, resultStream);
		//initTOCResource(book);
		// OEBPS/toc.ncx
		writeResource(book.ncx, resultStream);
		// htm and css
		// writePackageDocument(book, resultStream);
		for(Resource resource: book.resources.values()) {
			CONST.log.info( "" + resource);
			writeResource(resource, resultStream);
		}
		// close
		resultStream.close();
	}
	// root mimetype
	private void writeMimeType(ZipOutputStream resultStream) throws IOException {
		ZipEntry mimetypeZipEntry = new ZipEntry(CONST.FILE_ROOT);
		mimetypeZipEntry.setMethod(ZipEntry.STORED);
		byte[] mimetypeBytes = CONST.MIME_EPUB.getBytes();
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
	/**
	 * Writes the META-INF/container.xml file.
	 * 
	 * @param resultStream
	 * @throws IOException
	 */
	private void writeContainer(ZipOutputStream resultStream) throws IOException {
		resultStream.putNextEntry(new ZipEntry(CONST.FILE_INFO));
		Writer out = new OutputStreamWriter(resultStream);
		out.write("<?xml version=\"1.0\"?>\n");
		out.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n");
		out.write("\t<rootfiles>\n");
		out.write("\t\t<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n");
		out.write("\t</rootfiles>\n");
		out.write("</container>");
		out.flush();
	}
	/**
	 * Writes the resource to the resultStream.
	 * 
	 * @param resource
	 * @param resultStream
	 * @throws IOException
	 */
	private void writeResource(Resource resource, ZipOutputStream resultStream)
			throws IOException {
		if(resource == null) {
			return;
		}
		try {
			resultStream.putNextEntry(new ZipEntry(resource.getHref()));
			InputStream inputStream = resource.getInputStream();
			IOUtil.copy(inputStream, resultStream);
			inputStream.close();
		} catch(Exception e) {
			CONST.log.error(e.getMessage(), e);
		}
	}	
}
