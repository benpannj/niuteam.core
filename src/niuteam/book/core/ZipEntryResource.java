package niuteam.book.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import niuteam.util.IOUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ZipEntryResource extends Resource{
	private ZipFile zf;
	private String base_path = "";
	private String old_href = "";
	private String encoding = CONST.ENCODING;	
	// for compact use
//	private byte[] data = null;
//	private ByteArrayOutputStream out = null;
	private File f = null;
	
	public ZipEntryResource(String name){
		super(name);
	}
	public void loadEntry(ZipFile file, String href, String type, String path){
		this.zf = file;
		this.href = href;
		this.old_href = href;
		this.mediaType = type;
		this.base_path = path;
	}
	public OutputStream getOutputStream() throws Exception {
		if (f == null) cal();
		FileOutputStream fos = new FileOutputStream(f);
		return fos;
	}
	public InputStream getInputStream() throws Exception {
		
		if (f != null && f.exists() ) {
			return new FileInputStream(f);
		}
		InputStream ins = null;
		try {
			if (zf != null) {
				ZipEntry ze = new ZipEntry(base_path + old_href);
				ins = zf.getInputStream(ze);
				
//				zf.getEntry(old_href).getSize();
			}
		} catch(Exception e){
			CONST.log.error("ERROR: "+ old_href, e);
		}
		if (ins == null && zf!=null){
			CONST.log.error("ERROR null: " + base_path + old_href);
//			ZipEntry ze2 = zf.getEntry(base_path + old_href);
//			if (ze2 != null)
//			ins = zf.getInputStream(ze2);
//			ZipEntry ze3 = zf.getEntry(old_href);
			
//			InputStream ins3 = zf.getInputStream(ze3);
			
			for (Enumeration entries = zf.entries();entries.hasMoreElements();) {
				ZipEntry ze0 = (ZipEntry)entries.nextElement();
				String name = ze0.getName();
				CONST.log.info( name + ", " + (old_href .equals(name))  );
				if (old_href .equals(name)){
					ins = zf.getInputStream(ze0);
					break;
				}
			}
			
		}
		return ins;
	}
	private void cal() throws Exception{
		InputStream ins = getInputStream();
//		if (ins == null) return;
//		File folder = new File(CONST.tmp_folder);
//		if (folder.exists() && folder.isDirectory()){
//			
//		} else {
//			folder.mkdirs();
//		}
		f = new File(IOUtil.getTempFolder()+File.separator+"f", this.href);
		File folder = f.getParentFile();
		if (!folder.exists()){
			folder.mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(f);
//		out = new ByteArrayOutputStream();
		IOUtil.copy(ins, fos);
		fos.flush();
	}
	public void replaceCss() throws Exception {
		f = new File(IOUtil.getTempFolder()+File.separator+"f", this.href);
		File folder = f.getParentFile();
		if (!folder.exists()){
			folder.mkdirs();
		}
		InputStream ins = IOUtil.loadTemplate("OEBPS/main.css");
		FileOutputStream fos = new FileOutputStream(f);
		IOUtil.copy(ins, fos);
		fos.flush();
	}

	public long getSize(){
		if (!CONST.MIME.HTM.equals(getMediaType()) ){
			return 0;
		}
		if (f != null)
			return f.length();
		else {
			ZipEntry ze = zf.getEntry(base_path + old_href); // new ZipEntry(base_path + old_href);
			if (ze == null) {
				CONST.log.error("BAD " + base_path + old_href);
				return 0;
			}else {
				return (int) ze.getSize();
			}
		}
	}
//	public List<String> split() throws Exception {
//		if (f == null) cal();
//		String html = data();
//		String temp;
//		html = XhtmlDoc.cleanHtml(html);
//		int start = 0, end = 0;
//		int offset = 120000;
//		boolean more = true;
//		int len = html.length();
//		Writer fwu = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
//		end = html.indexOf("</p>", start+offset);
//		if (end != -1 && len-end >1000){
//			temp = html.substring(start, end+4);
//			fwu.write(temp);
//			fwu.write("</body></html>");
//			more = true;
//		} else {
////			temp = html.substring(start);
//			fwu.write(html);
//			more = false;
//		}
//		fwu.flush();
//		fwu.close();
//		StringBuffer buf = new StringBuffer();
//		List<String> list = new ArrayList<String>();
//		while (more){
//			buf.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
//			buf.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title></title>")
//			.append("<link href=\"../Styles/main.css\" rel=\"stylesheet\" type=\"text/css\" />")
//			.append("</head><body>");
//			start = end+4;
//			end = html.indexOf("</p>", start+offset);
//			if (end != -1 && len-end >1000){
//				buf.append( html.substring(start, end+4)).append("</body></html>");
//				more = true;
//			} else {
//				buf.append(html.substring(start));
//				more = false;
//			}
//			list.add(buf.toString());
//			buf.setLength(0);
//		}
//		return list;
//		
//		
//	}
	public void append(String d) throws Exception {
		if (f == null) cal();
		String html = data();
		html = XhtmlDoc.cleanHtml(html);
		
		Writer fwu = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
//		fwu.write("<html><head><title></title>");
//		fwu.write("<link href=\"../Styles/main.css\" rel=\"stylesheet\" type=\"text/css\" />");
//		fwu.write("</head><body>");
		int pos = html.lastIndexOf("</body>");
		if (pos >0){
			String ss = html.substring(0, pos);
			fwu.write(ss);
		} else {
			fwu.write(html);
		}
		if (d.length() > 2) {
			fwu.write("\r\n<p> - </p> \r\n");
	
			String b2 = XhtmlDoc.cleanHtml(d);
			
			b2 = XhtmlDoc.getStringBetween(b2, "<body", "</body>");
			pos = b2.indexOf(">");
			if (pos >= 0){
				b2 = b2.substring(pos+1);
			}
			fwu.write(b2);
		}
		fwu.write("</body></html>");
		fwu.flush();
		fwu.close();
	}
	public String data() throws Exception{
		if (f == null) cal();
		String encoding = "utf-8";
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(new FileInputStream(f), encoding), out );
		String html = out.toString();

//		byte[] b = out.toByteArray();
//		CONST.log.info("" +this.href +" : "+ new String(b));
		return html;
	}
	private String getTitle(String html, int offset){
		String s = XhtmlDoc.getStringBetween(html, "<h2", "</h2>");
		int pos = s.indexOf(">");
		if (pos < 0){
			s = XhtmlDoc.getStringBetween(html, "<title", "</title>");
			pos = s.indexOf(">");
			if (pos < 0){
				return null;
			}
		}
		
		s = s.substring(pos+1);
		s = XhtmlDoc.html2txt(s);
		if (offset< 1 || offset >= s.length() ) {
			return s;
		} else {
		return s.substring(0, offset);
		}
	}


	public boolean _mergeSameTitle(String d, int offset)  throws Exception {
		if (f == null) cal();
	
		String html = data();
		String title = getTitle(d, offset);
		if (title==null || !title.equals(getTitle(html, offset))){
			CONST.log.info("title: " + title);
			return false;
		}
		html = XhtmlDoc.cleanHtml(html);
		
		Writer fwu = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
		int pos = html.lastIndexOf("</body>");
		if (pos >0){
			String ss = html.substring(0, pos);
			fwu.write(ss);
		} else {
			fwu.write(html);
		}

		fwu.write("\r\n<p> - </p> \r\n");

		String b2 = XhtmlDoc.cleanHtml(d);
		
		
		
		b2 = XhtmlDoc.getStringBetween(b2, "<body", "</body>");
		pos = b2.indexOf(">");
		if (pos >= 0){
			b2 = b2.substring(pos+1);
		}
		fwu.write(b2);
		fwu.write("</body></html>");
		fwu.flush();
		fwu.close();
		return true;
	}
}
