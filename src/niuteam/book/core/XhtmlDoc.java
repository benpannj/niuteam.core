package niuteam.book.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import niuteam.util.IOUtil;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class XhtmlDoc {
	private String html;
	private String content;
	private String title;
	public String getHtml(){
		return html;
	}
	public XhtmlDoc(){
		html = null;
	}
	public XhtmlDoc(File f, String encoding) throws Exception{
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(new FileInputStream(f), encoding), out );
		html = out.toString();
	}
	public void removeDup222(File fout)throws Exception{
		StringBuilder buf = new StringBuilder(html);
		int pos = buf.indexOf("<p>");
		int cur = 0;
		while (pos > 0){
			char c = buf.charAt(pos);
			cur = -1;
			pos++;
			char n = buf.charAt(pos);
			while (n!='<'){
				if (n==c){
					buf.deleteCharAt(pos-1);
				}else{
					pos++;
				}
				c = n;
				n = buf.charAt(pos);
			}
			cur = pos;
			pos = buf.indexOf("<p>", pos);
			// merge para
			if (c == '。' || c == '”' || c == '！' || pos -cur >10){
				// end.
				pos+=3;
			} else if (pos >0){
				CONST.log.debug("del " + buf.substring(cur, pos+3) + (pos -cur));
				buf.delete(cur, pos+3);
				pos = cur;
			} else {
				// pos < 0 , return
			}			
		}
		Writer fwu = new OutputStreamWriter(new FileOutputStream(fout), "utf-8");
		fwu.write(buf.toString() );
		fwu.flush();
		fwu.close();
	}
	public void mergeTmpl(File fout) throws Exception{
		InputStream ins = IOUtil.loadTemplate(CONST.TMPL_HTM);
		StringWriter out2 = new StringWriter();
		IOUtil.copy(new InputStreamReader(ins, "utf-8"), out2 );


		StringBuffer buf = new StringBuffer(out2.toString());

		if (title == null) {
			title = fout.getName();
		}
		str_rpl(buf, "<h2>", "</h2>", "<h2>"+title + "</h2>");
		if (content == null){
			content = html;
		}
		str_rpl(buf, "<p>", "</p>", "<p>"+content + "</p>");

		// clean html
		// 
		str_rpl(buf, "&nbsp;", null, " ");
		str_rpl(buf, "<link", "</link>", "");
		str_rpl(buf, "<b>", null, " ");
		str_rpl(buf, "</b>", null, " ");

		html = buf.toString();

		// String strTarget = new String (chapterText.getBytes(Charset.forName("utf-8")), "utf-8");
//		File fu = new File("/tmp", "test_h2_utf8.htm" );
//		CONST.log.info("" + fu.getAbsolutePath());
		Writer fwu = new OutputStreamWriter(new FileOutputStream(fout), "utf-8");
		fwu.write(html);
		fwu.flush();
		fwu.close();
	}
	public void analyzeTxt222(String t){
		title = t;
		//content = txt2htm(html, title);
	}
	public void analyzeTitle(String open, String close, String df){
		title = getStringBetween(html, open, close);
		if (title == null || title.length() == 0){
			title = df;
		}
	}
	public void analyzeContent(String open, String close){
		content = getStringBetween(html, open, close);
		// <td class="style4">
		if (content == null || content.length() == 0){
			content = getStringBetween(html, "<td class=\"style4\">", "</td>");
		}
		if (content == null || content.length() == 0){
			content = html;
		}
		
		content = cleanHtml(content);
		//  ALIGN="CENTER"
//		content = regex_rpl(content, "<[/]?(font|link|script|span|xml|del|ins|[ovwxp]:\\w+)[^>]*?>", "");
//		content = regex_rpl(content, "<([^>]*)(?:class|align|lang|style|link|size|face|[ovwxp]:\\w+)=(?:'[^']*'|\"\"[^\"\"]*\"\"|[^\\s>]+)([^>]*)>", "<$1$2>");
//		content = regex_rpl(content, "\\s>", ">");

	}
	public void analyzeContentMulti(String open, String close, boolean parag){
		if (html == null) return;
		if (open == null){
			content = html;
			return;
		}
		int startIndex = html.indexOf(open);
		if (startIndex <0){
			content = html;
			return ;
		}
		int start_len = open.length();
		startIndex +=start_len;
		int endIndex = 0;
		if (close != null){
			endIndex = html.indexOf(close, startIndex);
		}
		if (endIndex < startIndex){
			content = html.substring(startIndex);
			return;
		}
		StringBuffer buf = new StringBuffer();
		while (endIndex > startIndex) {
			if (parag) buf.append("<p>");
			buf.append(html.substring(startIndex, endIndex) );
			if (parag) buf.append("</p>");
			startIndex = html.indexOf(open, endIndex);
			if (startIndex == -1) break;
			endIndex = html.indexOf(close, startIndex);
		}
		
		content = buf.toString();
		content = cleanHtml(content);
		//  ALIGN="CENTER"
//		content = regex_rpl(content, "<[/]?(font|link|script|span|xml|del|ins|[ovwxp]:\\w+)[^>]*?>", "");
//		content = regex_rpl(content, "<([^>]*)(?:class|align|lang|style|link|size|face|[ovwxp]:\\w+)=(?:'[^']*'|\"\"[^\"\"]*\"\"|[^\\s>]+)([^>]*)>", "<$1$2>");
//		content = regex_rpl(content, "\\s>", ">");

	}
	public String downloadUrlContent(String urlStr, String encoding)
			throws Exception {
		URL url = new URL(urlStr);
		URLConnection urlc = url.openConnection();
		urlc.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2) Gecko/20100115 Firefox/3.6");
		urlc.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		urlc.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.5");
		urlc.setRequestProperty("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
		urlc.setConnectTimeout(5000);
//		urlc.connect();
		StringBuilder sb = new StringBuilder(4096);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				urlc.getInputStream(), encoding));
		String line;
		while ((line = in.readLine()) != null) {
			sb.append(line).append('\n');
		}
		in.close();
//		System.out.println(urlStr);
		html = sb.toString().trim();
		return html;
	}	
	public static String html2txt(String s) {
		if (s != null) {
			return s.replaceAll("<.*?>", "");
//			return s.replaceAll("<script.*?>", "");
		}
		return "";
	}
	public static String txt2htm2222(String s, String tl) {
		if (s == null) return null;
		int l_t = tl==null? 0: tl.length();
		StringBuilder sb = new StringBuilder(4096);
		StringBuilder rest = new StringBuilder(4096);
		BufferedReader in = new BufferedReader(new StringReader(s) );
		String line;
		sb.append("<p>");
		rest.append("<p>");
		try {
			int cur_len = 0;
			while ((line = in.readLine()) != null) {
				String line_t = line.trim();
				int l = line_t.length();
				if (l == 0) continue;
				if (line_t.startsWith("----") && line_t.endsWith("----")){
					rest.append("<div>");
					rest.append(line_t);
					for (int i = 0; i < 3; i++){
						// skip next 3 line for book
						line = in.readLine();
						rest.append(line);
					}
					rest.append("</div>");
					continue;
				}
				if (l_t>0 && line_t.contains(tl) && (l-l_t>0 && l-l_t <6)){
					rest.append("<div>").append(line_t).append("</div>");
					continue;
				}
				cur_len += l;
				String end = "。";
				sb.append(line_t);
				if (line_t.endsWith(end)||line_t.endsWith("”") ||line_t.endsWith("！") || cur_len >1024) {
					sb.append("</p>").append("<p>");
					cur_len = 0;
				}
			}
			sb.append("</p>");
			in.close();
			rest.append("</p>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sb.append(rest);
//		System.out.println(urlStr);
		return sb.toString().trim();
	}
//	public static void cleanHtml(StringBuffer buf, String[] keep, String rpl){
//		if (buf == null || keep == null) return;
//		int pos = 0;
//		int len = buf.length();
//		while (pos< len){
//			char c = buf.charAt(pos);
//			if (c == '<'){
//				
//			}
//		}
////		int patt_len = (patt_end==null)? 0 : patt_end.length();
////		int rpl_len = (rpl == null) ? 0 : rpl.length();
//		pos = buf.indexOf("<");
//		while (pos > 0){
//			int end = -1;
//			// find end pos
//			if (patt_len == 0){
//				end = pos++;
//			} else {
//				end = buf.indexOf(patt_end, pos);
//				if (end > pos)
//					end += patt_len;
//			}
//			if (end >0){
//				// replace or del
//				if (rpl_len > 0){
//					buf.replace(pos, end, rpl);
//					end = pos + rpl_len;
//				} else {
//					buf.delete(pos, end);
//					end = pos;
//				}
//				// next start
//				pos = buf.indexOf(patt_start, end);
//			} else {
//				pos = -1; 
//			}
//		}
//	}
	public static void str_rpl(StringBuffer buf, String patt_start, String patt_end, String rpl){
		int pos = -1;
		if (buf == null || patt_start == null) return;
		int patt_len = (patt_end==null)? 0 : patt_end.length();
		int rpl_len = (rpl == null) ? 0 : rpl.length();
		pos = buf.indexOf(patt_start);
		while (pos > 0){
			int end = -1;
			// find end pos
			if (patt_len == 0){
				end = pos+patt_start.length();
			} else {
				end = buf.indexOf(patt_end, pos);
				if (end > pos)
					end += patt_len;
			}
			if (end >0){
				// replace or del
				if (rpl_len > 0){
					buf.replace(pos, end, rpl);
					end = pos + rpl_len;
				} else {
					buf.delete(pos, end);
					end = pos;
				}
				// next start
				pos = buf.indexOf(patt_start, end);
			} else {
				pos = -1; 
			}
		}
	}
	public void str_insert(StringBuffer buf, String patt_start, String patt_end, String rpl){
		if (rpl == null || rpl.length() == 0) return;
		int pos = -1;
		pos = buf.indexOf(patt_start);
//		int patt_len = patt_end.length();
		while (pos > 0){
			int end = buf.indexOf(patt_end, pos);
			if (end >0){
				buf.insert(end-1, rpl);
				end += rpl.length();
				pos = buf.indexOf(patt_start, end);
			} else {
				pos = -1; 
			}
		}
	}
	private static  String regex_rpl(String src, String patt, String rpl){
		Pattern pattern = Pattern.compile(patt, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(src);
		return matcher.replaceAll(rpl);
	}
	public static String cleanHtml(String html){
		
//		Whitelist epub = new Whitelist();
//		epub.addTags("p","pre","br","li","ul","h1","h2","h3","html","head","link","body");
//		String content = Jsoup.clean(html, epub);
//		if (content != null)
//			return content;
		
		String s;
		s = regex_rpl(html, "&nbsp;", " ");
//		s = regex_rpl(s, "\\s+", " ");
		s = regex_rpl(s, "\\s>", ">");
		StringBuffer buf = new StringBuffer(s);
		str_rpl(buf, "<!--", "-->", "");
		str_rpl(buf, "<svg ", "</svg>", "");
		str_rpl(buf, "<script ", "</script>", "");
//		str_rpl(buf, "<link", "</link>", "");
		str_rpl(buf, "<style", "</style>", "");
		str_rpl(buf, "<td ", ">", "<td>");
		str_rpl(buf, "<tr ", ">", "<tr>");
		str_rpl(buf, "<p ", ">", "<p>");
		str_rpl(buf, "<a ", ">", "<a>");
		str_rpl(buf, "<div ", ">", "<div>");
		
		s = buf.toString();
		// "input", "font", "textarea", "br","table","tr","td","tbody","a", "form"
		// "link", "script"
		s = regex_rpl(s, "<[/]?(font|script|strong|span|xml|del|ins|[ovwxp]:\\w+)[^>]*?>", "");
//		s = regex_rpl(s, "<[/]?(script\\w+)[^>]*?>", "");

		s = regex_rpl(s, "<([^>]*)(?:class|style|link|size|face|[ovwxp]:\\w+)=(?:'[^']*'|\"\"[^\"\"]*\"\"|[^\\s>]+)([^>]*)>", "<$1$2>");
		
//		s = regex_rpl(s, "<([^>]*)(?:class|lang|style|size|face|[ovwxp]:\\w+)=(?:'[^']*'|\"\"[^\"\"]*\"\"|[^\\s>]+)([^>]*)>", "<$1$2>");
//		s = regex_rpl(s, "\\s+", " ");
		s = regex_rpl(s, "\\s>", ">");

		return s ;
		    
//	    // start by completely removing all unwanted tags 
//	    html = Regex.Replace(html, @"<[/]?(font|span|xml|del|ins|[ovwxp]:\w+)[^>]*?>", "", RegexOptions.IgnoreCase);
		// 1. match an open tag character < 
		// 2. and optionally match a close tag sequence </  (because we also want to remove the closing tags) 
		// 3. match any of the list of unwanted tags: font,span,xml,del,ins 
		// 4. a pattern is given to match any of the namespace tags, anything beginning with o,v,w,x,p, followed by a : followed by another word 
		// 5. match any attributes as far as the closing tag character > 
		// 6. the replace string for this regex is "", which will completely remove the instances of any matching tags. 
		// 7. note that we are not removing anything between the tags, just the tags themselves
		
//	    // then run another pass over the html (twice), removing unwanted attributes 
//	    html = Regex.Replace(html, @"<([^>]*)(?:class|lang|style|size|face|[ovwxp]:\w+)=(?:'[^']*'|""[^""]*""|[^\s>]+)([^>]*)>","<$1$2>", RegexOptions.IgnoreCase); 
//	    html = Regex.Replace(html, @"<([^>]*)(?:class|lang|style|size|face|[ovwxp]:\w+)=(?:'[^']*'|""[^""]*""|[^\s>]+)([^>]*)>","<$1$2>", RegexOptions.IgnoreCase); 
		//1. match an open tag character < 
		//2. capture any text before the unwanted attribute (This is $1 in the replace expression) 
		//3. match (but don't capture) any of the unwanted attributes: class, lang, style, size, face, o:p, v:shape etc. 
		//3. there should always be an = character after the attribute name 
		//4. match the value of the attribute by identifying the delimiters. these can be single quotes, or double quotes, or no quotes at all. 
		//5. for single quotes, the pattern is: ' followed by anything but a ' followed by a ' 
		//6. similarly for double quotes.  
		//7. for a non-delimited attribute value, i specify the pattern as anything except the closing tag character > 
		//8. lastly, capture whatever comes after the unwanted attribute in ([^>]*) 
		//9. the replacement string <$1$2> reconstructs the tag without the unwanted attribute found in the middle. 
		//10. note: this only removes one occurence of an unwanted attribute, this is why i run the same regex twice.  For example, take the html fragment: <p class="MSO Normal" style="Margin-TOP:3em">  
		//   the regex will only remove one of these attributes.  Running the regex twice will remove the second one.  
		//	    return html;
	}
	public static String getStringBetween(String src, String startText,
			String endText) {
		if (src == null || startText == null){
			return src;
		}
		int startIndex = src.indexOf(startText);
		if (startIndex <0){
			return "";
		}
		int endIndex = src.indexOf(endText, startIndex);
		if (endIndex > startIndex) {
			return src.substring(startIndex + startText.length(), endIndex);
		}else {
			return src.substring(startIndex + startText.length());
		}
	}
	public static String getStringLastBetween(String src, String startText,
			String endText) {
		if (src != null && src.contains(startText)) {
			int startIndex = src.lastIndexOf(startText);
			int endIndex = src.indexOf(endText, startIndex);
			if (endIndex > startIndex) {
				return src.substring(startIndex + startText.length(), endIndex);

			}
		}
		return "";

	}

	public static String[] getChapterIds(String bookHtml, String HtmlBookId) {
		java.util.List<String> chapterList = new ArrayList<String>();
		String startText = "<a href=\"c_" + HtmlBookId + "_";
		String endText = ".html\"";
		// <A href="/Code/Java/XML/Deepprintofnodelist.htm">
		String chapterId = null;
		while ((chapterId = getStringBetween(bookHtml, startText, endText))
				.length() > 0) {
			System.out.println("chapterId==" + chapterId);
			chapterList.add(chapterId);
			bookHtml = bookHtml.substring(bookHtml.indexOf(startText)
					+ startText.length());
		}

		return chapterList.toArray(new String[0]);
	}
//	public static void addChapter(String HtmlBookId,
//			String chapterId) throws Exception {
//		String chapterUrl = "http://book.com/book/chapter_" + HtmlBookId + "_"
//				+ chapterId + ".html";
//		String chapterHtml = downloadUrlContent(chapterUrl, "utf-8");
//		String chapterTitle = getStringLastBetween(chapterHtml,"<h1>", "</h1>");
//		String chapterText = getStringLastBetween(chapterHtml, "","");
//		chapterText = chapterText.replaceAll("</p><p>", "\n");
//		chapterText = chapterText.replaceAll("<p>", "");
//		chapterText = html2txt(chapterText.replaceAll("</p>", "")).trim();
//		String chapterTextArr[] = chapterText.split("\n");
//		addChapter(HtmlBookId, chapterId, chapterTitle, chapterTextArr);
//
//	}

	/**
	 * 根据章节内容添加章节
	 * 
	 * @param epub
	 * @param chapterId
	 * @param title
	 * @param texts
	 */
	public static void addChapter(String HtmlBookId,
			String chapterId, String title, String[] texts) {
		if (texts == null || texts.length < 1) {
			System.out.println("warn: " + HtmlBookId + "|" + chapterId + "|"
					+ title + " texts is empty");
			return;
		}
		if (title == null || title.length() < 1) {
			System.out.println("warn: " + HtmlBookId + "|" + chapterId + "|"
					+ title + " title is empty");
			return;
		}
//		NCXResource toc = epub.getTOC();
//		TOCEntry rootTOCEntry = toc.getRootTOCEntry();
		String chapterFile = "/" + chapterId
				+ ".html";
		System.out.println("addChapter " + chapterFile + "|" + chapterId + "|"
				+ title);
//		OPSResource chapter1 = epub.createOPSResource(chapterFile);
//		epub.addToSpine(chapter1);
//		OPSDocument chapter1Doc = chapter1.getDocument();
//		TOCEntry chapter1TOCEntry = toc.createTOCEntry(title,chapter1Doc.getRootXRef());
//		rootTOCEntry.add(chapter1TOCEntry);
//		Element body1 = chapter1Doc.getBody();
//		Element header1 = chapter1Doc.createElement("h1");
//		header1.add(title);
//		body1.add(header1);
//		{// 添加原文来源：
//			String chapterUrl = "http://book.com/book/chapter_" + HtmlBookId
//					+ "_" + chapterId + ".html";
//			Element paragraph1 = chapter1Doc.createElement("p");
//			paragraph1.add("原文来源：" + chapterUrl);
//			body1.add(paragraph1);
//		}
//		for (int i = 0; texts != null && i < texts.length; i++) {
//			Element paragraph1 = chapter1Doc.createElement("p");
//			paragraph1.add(texts[i]);
//			body1.add(paragraph1);
//		}

	}
	
	public static List<String> split2222(File f) throws Exception {
		String temp;
		String encoding = "utf-8";
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(new FileInputStream(f), encoding), out );
		String html = out.toString();
		
		html = XhtmlDoc.cleanHtml(html);
		int start = 0, end = 0;
		int offset = 100000;
		boolean more = true;
		int len = html.length();
		Writer fwu = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
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
}
