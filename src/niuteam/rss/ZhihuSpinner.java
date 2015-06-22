package niuteam.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.epub.Epub;
import niuteam.util.IOUtil;
import niuteam.util.WebSpinner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.Json;
import org.json.util.XML;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class ZhihuSpinner {
	private String user = "Ben Pan";
	// private Epub bk_all = new Epub();
	private File tmp_folder;
	private SimpleDateFormat fmt_yyyymmdd = null;
	//private SimpleDateFormat fmt_eeedmy = null;
	private String yymmdd = null;
	private int count = 0;
	private long newest_dt = 0;

	public void init()throws Exception{
		Date date_now = new Date();
		fmt_yyyymmdd = new SimpleDateFormat("yyyyMMdd");//HHmmss
	   // fmt_eeedmy = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		
//		long now = date_now.getTime(); // System.currentTimeMillis();
		yymmdd = fmt_yyyymmdd.format(date_now);
		//bk_all.create(yymmdd, user,"zh");
		
		tmp_folder = new File(IOUtil.getTempFolder()+"/f", yymmdd);
		if (tmp_folder.exists()) {
		} else {
			tmp_folder.mkdirs();
		}
	}
	// merge epub from web url
	public void genEpub() throws Exception {
		File f = new File("etc/book.zhihu.json");
		if (!f.exists()){
			CONST.log.error("no conf: " + f.getAbsolutePath());
			return;
		}
		// today
		init();
		boolean dirty = false;
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(new FileInputStream(f)), out );
		JSONObject json = new JSONObject(out.toString());
		user = json.optString("user");
		JSONArray ary = json.getJSONArray("rss");
		for (int i = 0, len = ary.length(); i < len; i++){
			JSONObject j = ary.getJSONObject(i);
			String id = j.optString("id");
			if (id.length() < 1) continue;
			try{
//				if (!"blogjava".equals(id)){
//					continue;
//				}
				//bk_all.addString(id+".htm", title, "<p>"+title+ ", " + url+"</p>");
				// build rss book
				boolean d = readRss(id, j);
				// check update date
				if (d){
					// j.put("last_dt", Long.valueOf(last_dt));
					dirty = true;
				}
			}catch(Exception e){
				CONST.log.error("", e);
			}
		}
		if (dirty){
			FileOutputStream output = new FileOutputStream(f);
			output.write(json.toString(2).getBytes());
			output.flush();
			output.close();
			//File outFile =  new File(IOUtil.getTempFolder(), "ALL."+yymmdd+".epub");
			//if (outFile.exists()){
			//	File destFile =  new File(IOUtil.getTempFolder(), "ALL."+yymmdd+System.currentTimeMillis()+".epub");
			//	outFile.renameTo(destFile );
			//}
			//bk_all.writeEpub(outFile);
			
		}
	}	
	public boolean readRss(String id, JSONObject json) throws Exception{
		String title = json.optString("title");
		String url = json.optString("url");
		long last_dt = json.optLong("last_dt", 0);

		CONST.log.info("load .. " + id + ", " + title + ", " + url);

//		File tmp_folder = new File(IOUtil.getTempFolder()+"/f", id);
//		if (tmp_folder.exists()) {
//		} else {
//			tmp_folder.mkdirs();
//		}
		// long newest_dt = last_dt;
		File outFile =  new File(IOUtil.getTempFolder(), "zhihu."+id+".epub");
		File destFile = null;
		Epub bk = new Epub();
		if (outFile.exists()){
			destFile =  new File(IOUtil.getTempFolder(), "zhihu."+id+yymmdd+count+".epub");
			count++;
			outFile.renameTo(destFile );
			bk.readEpub(destFile);
		} else {
			bk.create(title, user,"zh");
		}
		for (int i = 0; i < 100000; i++){
			int size = downZhuanlan(id, i*10, bk, last_dt);
			if (size == 0){
				break;
			}
		}
		boolean dirty = newest_dt > last_dt;
		if (dirty){
			bk.writeEpub(outFile);
			CONST.log.info(" Merge ok, send out. --"+ outFile.getAbsolutePath() + ","+ (newest_dt-last_dt) );
//			checkEpub(outFile.getAbsolutePath());
			json.put("last_dt", Long.valueOf(newest_dt));
		} else if (destFile!= null){
			destFile.renameTo(outFile);
		}
		return dirty;
	}

    public String analyzeZhuanlan(Document doc) throws Exception{
//    	org.jsoup.nodes.Document doc = Jsoup.parse(cnt);
    	
    	Element elm_hdr = doc.select("header").first();
    	String title = elm_hdr.select(".entry-title").text();
    	org.jsoup.nodes.Element t_img = elm_hdr.select(".entry-title-image").first().select("img").first();
    	img(t_img, null);
//    	String t_img = elm_hdr.select(".entry-title-image").first().select("img").attr("src");
    	String t_time = elm_hdr.select("div.entry-meta").first().select("time").first().attr("title");
    	elm_hdr.remove();

    	Document d_new = new Document("");
    	d_new.appendElement("h2").appendText(title);
    	d_new.appendElement("p").appendText(t_time);
		d_new.appendElement("p").appendChild(t_img);
    	
    	Element elm_sec = doc.select("section.entry-content").first();
    	elm_sec.tagName("p");
    	elm_sec.removeAttr("class");
    	elm_sec.removeAttr("ng-bind-html");
//    	org.jsoup.nodes.Node n = elm_sec.
//    	Elements nds = elm_sec.getAllElements();
//    	for (Iterator<Element> it = nds.iterator(); it.hasNext();){
//    		org.jsoup.nodes.Element elm = it.next();
//    		Elements es = elm.getAllElements();
//    		if (es.size() > 1){
//    			CONST.log.debug("deep " + elm.toString()) ;
//    		}
//   			String t = elm.tagName();
//   			if ("img".equals(t)){
//   				String img_src = elm.attr("data-original");
//   				String img_nm = img_src.substring(img_src.lastIndexOf('/')+1);
//   				String file_name = img_nm;
//   				d_new.appendElement("p").appendElement("img").attr("src", file_name);
//   			} else {
//   				d_new.appendChild(elm);
//   			}
//    	}
//    	elm_sec.
    	org.jsoup.select.Elements elm_imgs = elm_sec.select("img");
		for (Iterator<Element> itor = elm_imgs.iterator(); itor.hasNext(); ){
			org.jsoup.nodes.Element elm_img = itor.next();
			img(elm_img, null);
//			elm_img.attributes().removeAttr("");
		}
		d_new.appendChild(elm_sec);
//		return doc.outerHtml();
		return d_new.html();
	}
    private Element img(Element elm_img, Epub book) throws Exception{
    	
		String img_src = elm_img.attr(elm_img.hasAttr("data-original")? "data-original":"src");
		elm_img.removeAttr("class");
		String img_nm = img_src.substring(img_src.lastIndexOf('/')+1);
//		String file_name = "i"+String.format("%03d", count)+img_nm;
		String file_name = img_nm;
		if (CONST.MIME.HTM.equals( Resource.determineMediaType(file_name) ) ){
			file_name = file_name+".jpg";
		}
		
		elm_img.attr("src", file_name);
		//CONST.log.info("skip img " + img_src);
		
		//String file_name = img_src+".jpg";
		
		count++;
		
//		fmt_yyyymmdd.format(dt_pub)+"."+"i"+String.format("%03d", count)
//		if ("*.*".equals(filter_img) || file_name.endsWith(filter_img) ){
			File f_img = new File(tmp_folder, file_name);
//			CONST.log.debug("img: " + f_img.getAbsolutePath() );
			if (!f_img.exists()) {
				WebSpinner.down(img_src, f_img);
			}
			elm_img.attr("src", file_name);
//			bk.addItem(f_img);
			book.addItem(f_img);
//		}else{
//			CONST.log.info("skip img " + img_src);
//		}

//    	Jsoup.clean(bodyHtml, whitelist)
//    	elm.attributes().iterator();
//		Whitelist wl = new Whitelist();
//		wl.addTags("p","span","pre","code","ul","li");//
//		wl.addTags("img").addAttributes("img","src","alt","real_src");
//		cnt = Jsoup.clean(cnt, wl);
    	return elm_img;
    }
//    public String analyzeZhuanlan(String s){
//    	StringBuilder buf = new StringBuilder();
//    	int start = 0;
//    	int pos = s.indexOf("<h1", start);
//    	start = s.indexOf('>', pos);
//    	int end = s.indexOf("</h1>", pos);
//    	buf.append("<h3>").append(s, start+1, end).append("</h3>").append("\n");
//    	// section
//    	pos = s.indexOf("<section");
//    	
//    	return buf.toString();
//    }
	public String downZhuanlan(String name) throws Exception {
		Epub bk_all = new Epub();
		bk_all.create(yymmdd, user,"zh");
		for (int i = 0; i < 100000; i++){
			int size = downZhuanlan(name, i*10, bk_all, 0);
			if (size == 0){
				break;
			}
		}
		File outFile =  new File(IOUtil.getTempFolder(), "ALL."+yymmdd+".epub");
		if (outFile.exists()){
			File destFile =  new File(IOUtil.getTempFolder(), "ALL."+yymmdd+System.currentTimeMillis()+".epub");
			outFile.renameTo(destFile );
		}
		bk_all.writeEpub(outFile);
		return outFile.getAbsolutePath();
	}
	public int downZhuanlan(String name, int offset,Epub book, long last_dt) throws Exception {
		int size = 0;
//		long newest_dt = last_dt;
		// http://zhuanlan.zhihu.com/api/columns/agBJB/posts
		// http://zhuanlan.zhihu.com/api/columns/agBJB/posts?limit=10&offset=10
		// http://zhuanlan.zhihu.com/api/columns/agBJB/posts/20074303
		String url = "http://zhuanlan.zhihu.com/api/columns/"+name+"/posts";
		if (offset > 0){
			url += "?limit=10&offset="+offset;
		}
		Response request = Jsoup.connect(url).referrer(url)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
		.execute();
		String body = request.body();
		if (body.charAt(0)== '['){
			// array 
			JSONArray ary = new JSONArray(body);
			size = ary.length();
			for (int i = 0; i < size; i++){
				JSONObject j = ary.getJSONObject(i);
				String id = j.getString("slug");
				long dt = Long.valueOf(id);
				if (dt <= last_dt){
					size =0;
					break;
				}
				if (offset == 0 && i == 0){
					newest_dt = dt; // rec first
				}
				String title = j.getString("title");
				Document doc = new Document("");
				doc.appendElement("h2").attr("id", id).appendText(title );
				doc.appendElement("p").appendText( j.getString("publishedTime"));
				String t_img = j.getString("titleImage");
				if ( t_img!= null && t_img.length()> 2) {
				Element img = doc.createElement("img").attr("src", t_img);
				try {
				img(img, book);
				}catch(Exception e){
					CONST.log.debug("ERR: "  + id + ", "+ title +","+ img.html() , e);
				}
				doc.appendElement("p").appendChild(img);
				}
				// or http://zhuanlan.zhihu.com/api/columns/agBJB/posts/20074303
				String cnt = j.getString("content");
				int len = cnt.length();
				int pos = 0;
				int start = cnt.indexOf("<img");
				int end = 0;
				while (start != -1){
					if (pos < start){
						doc.appendElement("p").appendText( cnt.substring(pos, start ) );
					}
					end = cnt.indexOf('>', start);
					String s_img = cnt.substring(start, end+1);
					int i_org = s_img.indexOf("data-original=\"");
					if (i_org > 0){
						i_org += "data-original=\"".length();
					} else{
						i_org = s_img.indexOf("src=\"") + "src=\"".length();
					}
					int e_org = s_img.indexOf("\"", i_org);
					if (e_org > i_org){
//						CONST.log.debug("" + s_img);
						String url_img = s_img.substring(i_org, e_org);
						Element e_img = doc.createElement("img").attr("src",url_img);
						img(e_img, book);
						doc.appendElement("p").appendChild(e_img);
					} else {
						CONST.log.debug("" + s_img);
					}
					pos = end+1;
					// next
					start = cnt.indexOf("<img", pos);
				}
				if (pos <len){
					doc.appendElement("p").appendText( cnt.substring(pos, len-1) );
				}
				// CONST.log.debug(doc.html() );
				
				book.addString(id, title, doc.html());
			}
		} else {
			CONST.log.debug(body);
			size = 0;
		}
		//JSONObject j = WebSpinner.downJson(url);
		
		return size;
	}
}
