package niuteam.book.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import niuteam.util.IOUtil;

public class ZipEntryResource extends Resource{
	private ZipFile zf;
	private String base_path = "";
	private String old_href = "";
	
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
		if (ins == null){
			CONST.log.error("ERROR null: "+ old_href);
		}
		return ins;
	}
	public void cal() throws Exception{
		InputStream ins = getInputStream();
//		if (ins == null) return;
		f = new File("/tmp", this.href);
		File folder = f.getParentFile();
		if (!folder.exists()){
			folder.mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(f);
//		out = new ByteArrayOutputStream();
		IOUtil.copy(ins, fos);
		fos.flush();
	}
	public long getSize(){
		if (f != null)
			return f.length();
		else {
			ZipEntry ze = zf.getEntry(base_path + old_href); // new ZipEntry(base_path + old_href);
			return (int) ze.getSize();
		}
	}
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
			return null;
		}
		s = s.substring(pos+1);
		s = XhtmlDoc.html2txt(s);
		if (offset< 1 || offset >= s.length() ) {
			return s;
		} else {
		return s.substring(0, offset);
		}
	}
	public boolean mergeSameTitle(String d, int offset)  throws Exception {
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
