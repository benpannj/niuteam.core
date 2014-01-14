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
	public static String determineMediaType(String file){
		String href = file.toLowerCase();
		if (href.endsWith(".png")) {
			return CONST.MIME.PNG;
		} else if (href.endsWith(".gif")){
			return CONST.MIME.GIF;
		} else if (href.endsWith(".jpg")){
			return CONST.MIME.JPG;
		} else if (href.endsWith(".css")){
			return CONST.MIME.CSS;
		} else if (href.endsWith(".txt") || href.endsWith(".html")  || href.endsWith(".htm")){
			return CONST.MIME.HTM;
		}else {
			CONST.log.info("check type file: ---  "+ href );
		}
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
		elms_l = doc_l.select("p");
		cleanElms(elms_l);

		String s = XhtmlDoc.cleanHtml(doc_l.html());
		
//		Whitelist epub = new Whitelist();
//		epub.addTags("p","pre","br","li","ul","h1","h2","h3","html","head","link","body");
//        Cleaner cleaner = new Cleaner(epub);
//        cleaner.
//        Document clean = cleaner.clean(doc_l);
//      String s = clean.html();
//		Whitelist wl = new Whitelist();
//		wl.addTags("p","span","pre","code","ul","li");//
//		wl.addTags("img").addAttributes("img","src","alt","real_src");
//		cnt = Jsoup.clean(cnt, wl);
        

        Writer fwu = new OutputStreamWriter(getOutputStream(), encoding);
		fwu.write(s);
		fwu.flush();
		fwu.close();
		return true;
	}	

	private String cleanElms(Elements elms_l) {
		String[] bad_ss = new String[]{
//			"大中华文化知识宝库","一 中华·民族","二 地理·资源","二 地理·资原","三 政治·党派","四 王朝·职官","五 司法·军事","六 经济·货币"
//			,"七 商业·外贸","八 农林·牧渔","九 交通·邮政","十 科技·医药","十一 教育·人才","十二 图书·报刊","十三 思想·伦理","十四 宗教·神祇","十五 语言·文字"
//			,"十六 文学·戏曲","十七 乐舞·书画","十八 文物·工艺","十九 名胜·名城","二十 建筑·园林","二十一 岁时·节庆","二十二 风俗·礼仪","二十三 人口·姓名"
//			,"二十四 称谓·亲属","二十五 婚姻·丧葬","二十六 服饰·饮食","二十七 日用·收藏","二十八 体育·读书"	
		};
		String title_l = null;
		StringBuilder buf = new StringBuilder();
		Element elm = null;
		Element elm_last = null; 
		for (Element elm_l : elms_l){
			title_l = elm_l.text().trim();
			if (title_l.length() == 0){
				elm_l.remove(); continue;
			}
			// remove bad string in text.
			for (String bad_str : bad_ss) {
				int pos = title_l.indexOf(bad_str);
				if (pos < 1) continue;
				int len = bad_str.length() + pos;
				int start = pos-1;
				while (start >=0 && Character.isDigit(title_l.charAt(start))){
					start--;
				}
				start++;
				if (start != pos) {
					String s = title_l.substring(start, len);
					String s2 = title_l.substring(0, start)+title_l.substring(len);
					elm_l.text(s2);
					title_l = s2;
					buf.append( s ).append(" ");
				}
			}
			// concat param
			char c = title_l.charAt(title_l.length()-1);
			boolean is_end = c =='。' || c=='！' || c=='」' || c=='”';
			boolean is_title = title_l.length()<10 && !title_l.contains("，") && !title_l.contains("。");
			if (is_title){
				if (buf.length() > 0){
					buf.append("---").append(title_l);
					elm_l.text( buf.toString());
				}
				buf.setLength(0);
				elm_last = elm_l;
				continue;
			} else if (is_end){
				buf.append(title_l);
				elm_l.text( buf.toString());
				buf.setLength(0);
				elm_last = elm_l;
				// end 
			} else {
				buf.append(title_l);
				elm_l.remove();
			}
		}
		if (buf.length() > 2){
			if (elm_last != null){
				buf.insert(0, elm_last.text());
				elm_last.text( buf.toString());
			} else {
				CONST.log.info(" .. "+ buf.toString());
			}
		}
		return buf.toString();
	}

}
