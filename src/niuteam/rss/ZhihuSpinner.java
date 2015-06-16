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
	private Epub bk_all = new Epub();
	private File tmp_folder;
	private SimpleDateFormat fmt_yyyymmdd = null;
	private SimpleDateFormat fmt_eeedmy = null;
	private String yymmdd = null;
	private int count = 0;

	public void init()throws Exception{
		Date date_now = new Date();
		fmt_yyyymmdd = new SimpleDateFormat("yyyyMMdd");//HHmmss
	    fmt_eeedmy = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		
//		long now = date_now.getTime(); // System.currentTimeMillis();
		yymmdd = fmt_yyyymmdd.format(date_now);
		bk_all.create(yymmdd, user,"zh");
		
		tmp_folder = new File(IOUtil.getTempFolder()+"/f", yymmdd);
		if (tmp_folder.exists()) {
		} else {
			tmp_folder.mkdirs();
		}
	}
	// merge epub from web url
	public void rss2epub() throws Exception {
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
				String title = j.optString("title");
				String url = j.optString("url");
				long old_last_dt = j.optLong("last_dt", 0);
	
				CONST.log.info("load .. " + id + ", " + title + ", " + url);
//				if (!"blogjava".equals(id)){
//					continue;
//				}
				bk_all.addString(id+".htm", title, "<p>"+title+ ", " + url+"</p>");
				// build rss book
				long last_dt = readRss(url, id, title, old_last_dt, j);
				// check update date
				if (last_dt > old_last_dt){
					j.put("last_dt", Long.valueOf(last_dt));
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
			File outFile =  new File(IOUtil.getTempFolder(), "ALL."+yymmdd+".epub");
			if (outFile.exists()){
				File destFile =  new File(IOUtil.getTempFolder(), "ALL."+yymmdd+System.currentTimeMillis()+".epub");
				outFile.renameTo(destFile );
			}
			bk_all.writeEpub(outFile);
			
		}
	}	
	public long readRss(String url,String id, String title, long last_dt, JSONObject json_config) throws Exception{
		File tmp_folder = new File(IOUtil.getTempFolder()+"/f", id);
		if (tmp_folder.exists()) {
		} else {
			tmp_folder.mkdirs();
		}
		long newest_dt = last_dt;
		File outFile =  new File(IOUtil.getTempFolder(), id+yymmdd+".epub");
		Epub bk = new Epub();
		if (outFile.exists()){
			File destFile =  new File(IOUtil.getTempFolder(), id+yymmdd+count+".epub");
			count++;
			outFile.renameTo(destFile );
			bk.readEpub(destFile);
		} else {
			bk.create(title, user,"zh");
		}
//		File f = new File("/tmp/i21st.html");
		Document doc;
		doc = Jsoup.connect(url) //.timeout(30000)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
				.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
				.header("Accept", "text/html")
//				.data("wd", "Java")
				  .timeout(15000)
		.get();
		Element elm_link = doc.select(".bloglist").first().select("a[href]").first();
		String href0 = elm_link.attr("href");
		json_config.put("nexturl", href0);
		//json_config.put("nexttl", elm_link.text());
		String ext = json_config.optString("ext");
		String filter = json_config.optString("filter");
		String filter_img = json_config.optString("img");
		
		while (json_config.has("nexturl")){
			String link = json_config.optString("nexturl");
			String name;
			json_config.remove("nexturl");
		    int pos = link.lastIndexOf('/');
			if (pos > 0){
				name = link.substring(pos+1);
			}else {
				name = link;
			}
			//String dt = j.optString("pubDate"); //// Thu, 1 Nov 2012 16:13:06
		    //Date dt_pub = fmt_eeedmy.parse(dt);
			//long l = dt_pub.getTime();
		    CONST.log.info(" - "  + link + ", "+name+", " + newest_dt);
			String href = name;
			count++;

		    // href
			File f_html = new File(tmp_folder, href);

			String cnt;
		    
			if (link.endsWith(filter)){
				// CONST.log.info("read more " + link);
		    	cnt = readHtml(link, json_config);
		    	if ("ERR".equals(cnt)){
		    		Thread.sleep(30000);
		    		cnt = readHtml(link, json_config);
		    	}
		    }else{
		    	// need this?
		    	CONST.log.info("skip " + link);
		    	continue;
		    }
			// write file
			FileOutputStream output = new FileOutputStream(f_html);
			output.write(cnt.getBytes());
			output.flush();
			output.close();
			
			
			Whitelist wl = new Whitelist();
			wl.addTags("p","span","pre","code","ul","li");//
			wl.addTags("img").addAttributes("img","src","alt","real_src");
			cnt = Jsoup.clean(cnt, wl);

			InputStream ins = IOUtil.loadTemplate("OEBPS/Text/c_00.htm");
			Document docT = Jsoup.parse(ins, "utf-8","");
			String ti = json_config.optString("t_name")+" "+json_config.optString("t_time");
			json_config.remove("t_name");
			docT.select("h2").first().html(ti);
			//.appendElement("p").text(json_config.optString("t_time"));
			json_config.remove("t_time");
			docT.select("div").first().html(link);
			docT.select("p").first().html(cnt);

			try{
			Elements elm_imgs = docT.select("img");
			if (filter_img.length() > 1 && elm_imgs!=null  &&  elm_imgs.size() > 0 ){
				for (Iterator<Element> itor = elm_imgs.iterator(); itor.hasNext(); ){
					org.jsoup.nodes.Element elm_img = itor.next();
					String img_src = elm_img.attr("real_src");
					String img_nm = img_src.substring(img_src.lastIndexOf('/')+1);
					String file_name = "i"+String.format("%03d", count)+img_nm;
					if (CONST.MIME.HTM.equals( Resource.determineMediaType(file_name) ) ){
						file_name = file_name+".jpg";
					}
					
					//String file_name = img_src+".jpg";
					
					count++;
					
//					fmt_yyyymmdd.format(dt_pub)+"."+"i"+String.format("%03d", count)
					if ("*.*".equals(filter_img) || file_name.endsWith(filter_img) ){
						File f_img = new File(tmp_folder, file_name);
						CONST.log.debug("img: " + f_img.getAbsolutePath() );
						if (!f_img.exists()) {
							WebSpinner.down(img_src, f_img);
						}
						elm_img.attr("src", file_name);
						bk.addItem(f_img);
						bk_all.addItem(f_img);
					}else{
						CONST.log.info("skip img " + img_src);
					}
				}

			}
			}catch(Exception e){
				
			}
			String content = docT.html();
			bk.addString(href, ti, content);
			bk_all.addString(href, ti, content);
		}	

			bk.writeEpub(outFile);
			CONST.log.info(" Merge ok, send out. --"+ outFile.getAbsolutePath() + ","+ (newest_dt-last_dt) );
//		checkEpub(outFile.getAbsolutePath());
		return newest_dt;
	}
	public String readHtml(String url, JSONObject json_config) throws Exception{
		String cnt = json_config.optString("cnt");
		String s;
		org.jsoup.nodes.Document doc=null;
		try{
			doc = Jsoup.connect(url)
				.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
				.header("Accept", "text/html")
//				.data("wd", "Java")
				  .timeout(35000)
				  .get();

			if (cnt.length() > 1){
				s = doc.select(cnt).first().html();
			}else{
				s = doc.body().html();
			}
			Element elm_t = doc.select(".articalTitle").first();
			
			json_config.put("t_name", elm_t.select(".titName").first().text());
			json_config.put("t_time", elm_t.select(".time").first().text());
			
			Element elm = doc.select(".articalfrontback2").first();
			Element elm_link = null;
			Elements links = elm.select("a[href]");
			if (links.size() == 1){
				Element elm_l = elm.select(".SG_floatL").first();
				if (elm_l != null){
					elm_link = links.first();
				}
			} else {
				elm_link = links.first();
			}
			if (elm_link != null) {
			String href0 = elm_link.attr("href");
			json_config.put("nexturl", href0);
			
			}

		}catch (Exception e){
			CONST.log.debug("e: "+ url, e);
			s = "ERR";
			if (doc != null){
			String html = doc.html();
			File f = new File(IOUtil.getTempFolder()+"/f", "p"+String.format("%03d", count)+".htm");
			FileOutputStream output = new FileOutputStream(f);
//			byte[] imgdata = request.bodyAsBytes();
			output.write(html.getBytes());
			output.flush();
			output.close();
			}
		}
//		if (content != null)
//			return content;
		
//		CONST.log.info(content);
		return s;
	}
	   /**
     * Getting RSS feed link from HTML source code
     *
     * @param ulr is url of the website
     * @returns url of rss link of website
     * */
    public String getRSSLinkFromURL(String url) {
        // RSS url
        String rss_url = null;
 
        try {
            // Using JSoup library to parse the html source code
            org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
            // finding rss links which are having link[type=application/rss+xml]
            org.jsoup.select.Elements links = doc
                    .select("link[type=application/rss+xml]");
 
            CONST.log.info("No of RSS links found: " + links.size());
 
            // check if urls found or not
            if (links.size() > 0) {
                rss_url = links.get(0).attr("href").toString();
            } else {
                // finding rss links which are having link[type=application/rss+xml]
                org.jsoup.select.Elements links1 = doc
                        .select("link[type=application/atom+xml]");
                if(links1.size() > 0){
                    rss_url = links1.get(0).attr("href").toString();
                }
            }
 
        } catch (IOException e) {
            e.printStackTrace();
        }
 
        // returing RSS url
        return rss_url;
    }
    public String analyzeZhuanlan(Document doc) throws Exception{
//    	org.jsoup.nodes.Document doc = Jsoup.parse(cnt);
    	
    	Element elm_hdr = doc.select("header").first();
    	String title = elm_hdr.select(".entry-title").text();
    	org.jsoup.nodes.Element t_img = elm_hdr.select(".entry-title-image").first().select("img").first();
    	img(t_img);
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
			img(elm_img);
//			elm_img.attributes().removeAttr("");
		}
		d_new.appendChild(elm_sec);
//		return doc.outerHtml();
		return d_new.html();
	}
    private Element img(Element elm_img) throws Exception{
    	
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
			bk_all.addItem(f_img);
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
		for (int i = 0; i < 100000; i++){
			int size = downZhuanlan(name, i*10);
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
	public int downZhuanlan(String name, int offset) throws Exception {
		int size = 0;
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
				String title = j.getString("title");
				Document doc = new Document("");
				doc.appendElement("h2").attr("id", id).appendText(title );
				doc.appendElement("p").appendText( j.getString("publishedTime"));
				String t_img = j.getString("titleImage");
				if ( t_img!= null && t_img.length()> 2) {
				Element img = doc.createElement("img").attr("src", t_img);
				try {
				img(img);
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
						img(e_img);
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
				
				bk_all.addString(id+".htm", title, doc.html());
			}
		} else {
			CONST.log.debug(body);
			size = 0;
		}
		//JSONObject j = WebSpinner.downJson(url);
		
		return size;
	}
}
