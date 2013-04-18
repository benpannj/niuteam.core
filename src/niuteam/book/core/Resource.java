package niuteam.book.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import niuteam.util.IOUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public abstract class Resource {
	private String id;
	protected String href;
	protected String mediaType;

	protected String title;

	protected Resource(String id){
		this.id = id;
	}
	public void setHref(String s) {
		this.href = s;
	}
	public static String determineMediaType(String href){
		if (href.endsWith(".png")) {
			return CONST.MIME.PNG;
		} else if (href.endsWith(".gif")){
			return CONST.MIME.GIF;
		} else if (href.endsWith(".jpg")){
			return CONST.MIME.JPG;
		} else if (href.endsWith(".css")){
			return CONST.MIME.CSS;
		}
		CONST.log.info("check type file: --- {} ", href );
		return CONST.MIME.HTM;
	}

	/**
	 * Gets the contents of the Resource as an InputStream.
	 * 
	 * @return The contents of the Resource.
	 * 
	 * @throws IOException
	 */
	public abstract InputStream getInputStream() throws Exception ;
	public abstract OutputStream getOutputStream() throws Exception;
	public abstract long getSize();
	
	public List<String> split() throws Exception {
		long size = getSize();
		if ( size <CONST.TINY_SIZE) return null;
		String encoding = CONST.ENCODING;
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(getInputStream(), encoding), out );
		String html = out.toString();
		String temp;
		int start = 0, end = 0;
		int offset = 120000;
		boolean more = true;
		int len = html.length();
		// 
		OutputStream outs = getOutputStream();
		Writer fwu = new OutputStreamWriter(outs, "utf-8");
		end = html.indexOf("</p>", start+offset);
		if (end != -1 && len-end >1000){
			temp = html.substring(start, end+4);
			fwu.write(temp);
			fwu.write("</body></html>");
			more = true;
		} else {
//			temp = html.substring(start);
			fwu.write(html);
			more = false;
		}
		fwu.flush();
		fwu.close();
		StringBuffer buf = new StringBuffer();
		List<String> list = new ArrayList<String>();
		while (more){
			buf.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
			buf.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title></title>")
			.append("<link href=\"../Styles/main.css\" rel=\"stylesheet\" type=\"text/css\" />")
			.append("</head><body>");
			start = end+4;
			end = html.indexOf("</p>", start+offset);
			if (end != -1 && len-end >1000){
				buf.append( html.substring(start, end+4)).append("</body></html>");
				more = true;
			} else {
				buf.append(html.substring(start));
				more = false;
			}
			list.add(buf.toString());
			buf.setLength(0);
		}
		return list;
		
		
	}
	
	
	public String getId() {
		return id;
	}

	/**
	 * The location of the resource within the contents folder of the epub file.
	 * 
	 * Example:<br/>
	 * images/cover.jpg<br/>
	 * content/chapter1.xhtml<br/>
	 * 
	 * @return
	 */
	public String getHref() {
		return href;
	}
	/**
	 * This resource's mediaType.
	 * 
	 * @return
	 */
	public String getMediaType() {
		return mediaType;
	}
	public String getTitle() {
		return title;
	}
//	public void setTitle(String title) {
//		this.title = title;
//	}

	
	/**
	 * Gets the hashCode of the Resource's href.
	 * 
	 */
	public int hashCode() {
		return href.hashCode();
	}
	
	/**
	 * Checks to see of the given resourceObject is a resource and whether its href is equal to this one.
	 * 
	 */
	public boolean equals(Object resourceObject) {
		if (! (resourceObject instanceof Resource)) {
			return false;
		}
		return href.equals(((Resource) resourceObject).getHref());
	}
	
	public String toString() {
		return IOUtil.toString("id", id,
				"mediaType", mediaType,
				"href", href);
	}
	public boolean mergeSameTitle(Resource d, int offset)  throws Exception {
		boolean merged = false;
		String encoding = CONST.ENCODING;
		Document doc_l = Jsoup.parse(getInputStream(), encoding,"");
		Document doc_r = Jsoup.parse(d.getInputStream(), encoding,"");
		Elements elms_l = doc_l.select("h3");
		String title_l = null; String title_r = null;
		if (elms_l.size() > 0){
			title_l = innerMergeSameTitle(elms_l);
			Elements elms_r = doc_r.select("h3");
			if (elms_r.size() > 0 ) {
				title_r = innerMergeSameTitle(elms_r);
				if (title_r.startsWith(title_l)){
					merged = true;
				}
			}
		}
		if (!merged){
			long len = this.getSize() + d.getSize();
			if (len < CONST.HUGE_SIZE){
				merged = true;
			}
		}
		if (merged){
			String body_r = doc_r.body().html();
//			String html = body_r.substring(6, body_r.length()-13);
			doc_l.body().append(body_r);
			//.appendChild(doc_r.body().children());
		}
//		doc_l.title();
		Writer fwu = new OutputStreamWriter(getOutputStream(), encoding);
		fwu.write(doc_l.html());
		fwu.flush();
		fwu.close();
		return merged;
	}
	private String innerMergeSameTitle(Elements elms_l) {
		String title_l = null;
		StringBuilder buf = new StringBuilder();
		this.title = title_l;
		//  （7）
		Element elm = null;
		for (Element elm_l : elms_l){
			title_l = elm_l.text().trim();
			int len = title_l.length();
			char c_end = title_l.charAt(len-1);
			int pos = -1;
			if (c_end == ')'){
				pos = title_l.lastIndexOf('(');
			}else if (c_end == '）'){
				pos = title_l.lastIndexOf('（');
			}
			if (pos != -1){
				String s = title_l.substring(pos+1, len-1);
				boolean b = true;
				for (int i = 0; i < s.length(); i++){
					char c = s.charAt(i);
					if (c >= '0' && c <= '9'){
					}else{
						b= false;
						break;
					}
				}
				if (b && !"1".equals(s) ){
					buf.append( elm_l.html());
					elm_l.remove();
					elm_l = null;
				} else{
					title_l = title_l.substring(0, pos);
				}
			}
			if (elm != null && elm_l !=null){
				elm = elm_l;
			}
		}
		if (buf.length() > 2){
			if (elm != null){
				elm.attr("_benpan_", buf.toString());
			}
		CONST.log.info(" .. "+ buf.toString());
		}
		return title_l;
	}
	public boolean clean() throws Exception {
		String encoding = CONST.ENCODING;
		Document doc_l = Jsoup.parse(getInputStream(), encoding,"");
		Elements elms_l = doc_l.select("h3");
		if (elms_l.size() > 0){
			innerMergeSameTitle(elms_l);
		}
		elms_l = doc_l.select("h2");
		if (elms_l.size() > 0){
			innerMergeSameTitle(elms_l);
		}
		String s = XhtmlDoc.cleanHtml(doc_l.html());
		
//		Whitelist epub = new Whitelist();
//		epub.addTags("p","pre","br","li","ul","h1","h2","h3","html","head","link","body");
//        Cleaner cleaner = new Cleaner(epub);
//        Document clean = cleaner.clean(doc_l);
//        String s = clean.html();

        Writer fwu = new OutputStreamWriter(getOutputStream(), encoding);
		fwu.write(s);
		fwu.flush();
		fwu.close();
		return true;
	}	
}
