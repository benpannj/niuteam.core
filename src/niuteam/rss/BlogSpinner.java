package niuteam.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.epub.Epub;
import niuteam.util.IOUtil;
import niuteam.util.WebSpinner;

import com.cs.esp.org.json.JSONArray;
import com.cs.esp.org.json.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class BlogSpinner {
	private String user = "Ben Pan";
	private File tmp_folder;
	private SimpleDateFormat fmt_yyyymmdd = null;
	private long newest_dt = 0, last_dt = 0;
	private String yymmdd = null;
	private int count = 0;
	private JSONObject json_tmpl = null;

	public void init(){
		Date date_now = new Date();
		fmt_yyyymmdd = new SimpleDateFormat("yyyyMMdd");//HHmmss
//	    fmt_eeedmy = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		
//		long now = date_now.getTime(); // System.currentTimeMillis();
		yymmdd = fmt_yyyymmdd.format(date_now);
		
		tmp_folder = new File(IOUtil.getTempFolder()+"/f", yymmdd);
		if (tmp_folder.exists()) {
		} else {
			tmp_folder.mkdirs();
		}
	}
	// merge epub from web url
	public void blog2epub() throws Exception {
		File f = new File("etc/book.blog.json");
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
		json_tmpl = json.getJSONObject("tmpl");
		JSONArray ary = json.getJSONArray("rss");
		for (int i = 0, len = ary.length(); i < len; i++){
			JSONObject j = ary.getJSONObject(i);
			String id = j.optString("id");
			if (id.length() < 1) continue;
			try{
				// build rss book
				boolean d = readBlog(id, j);
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
		}
	}	
	public boolean readBlog(String id, JSONObject json_config) throws Exception{
		String title = json_config.optString("title");
		String url = json_config.optString("url");
		last_dt = json_config.optLong("last_dt", 0);
		newest_dt = 0;
		JSONObject j_tmpl = null;
		String tmpl = json_config.optString("tmpl");
		if (json_tmpl.has(tmpl)){
			j_tmpl = json_tmpl.getJSONObject(tmpl);
		}

		CONST.log.info("load .. " + id + ", " + title + ", " + url + ", tmpl " + tmpl);

		File outFile =  new File(IOUtil.getTempFolder(), id+".epub");
		Epub bk = new Epub();
		File destFile = null;
		if (outFile.exists()){
			destFile =  new File(IOUtil.getTempFolder(), id+yymmdd+count+".epub");
			count++;
			outFile.renameTo(destFile );
			bk.readEpub(destFile);
		} else {
			bk.create(title, user,"zh");
		}
		if (j_tmpl.has("page")){
			// page mode
			int start = j_tmpl.optInt("page_start", 0);
			int end = 3000;  //100000
			for (int i = start; i < end; i++){
				int size = page(id, url, i, bk, last_dt, j_tmpl);
				if (size == 0){
					break;
				}
			}
		} else {
			// next url mode
			int size = next(id, url, bk, last_dt, j_tmpl);
		}
	
		boolean dirty = newest_dt > last_dt;
		if (dirty){
			bk.writeEpub(outFile);
			CONST.log.info(" Merge ok, send out. --"+ outFile.getAbsolutePath() + ","+ (newest_dt-last_dt) );
//			checkEpub(outFile.getAbsolutePath());
			json_config.put("last_dt", Long.valueOf(newest_dt));
		} else if (destFile!= null){
			destFile.renameTo(outFile);
		}
		return dirty;
	}
	public String readHtml(org.jsoup.nodes.Document doc, JSONObject json_config) throws Exception{
		String cnt = json_config.optString("cnt");
		String s;
		
		try{

			if (cnt.length() > 1){
				s = doc.select(cnt).first().html();
			}else{
				s = doc.body().html();
			}
			Element elm_t = doc.select(".articalTitle").first();
			
			json_config.put("t_name", elm_t.select(".titName").first().text());
			json_config.put("t_time", elm_t.select(".time").first().text());
			
			Element elm = doc.select(".articalfrontback2").first();
			if (elm == null){
				elm = doc.select(".articalfrontback").first();
			}
			Element elm_link = null;
			Elements links = elm.select("a[href]");
			if (links.size() == 1){
				Element elm_l = elm.select(".SG_floatL").first();
				if (elm_l != null){
					elm_link = links.first();
				} else {
					String txt = elm.text();
					if (txt.startsWith("前")){
						elm_link = links.first();
					}
				}
			} else {
				elm_link = links.first();
			}
			if (elm_link != null) {
			String href0 = elm_link.attr("href");
			json_config.put("nexturl", href0);
			
			}

		}catch (Exception e){
			CONST.log.debug("e: ", e);
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
	public int page(String name, String url, int offset,Epub book, long last_dt, JSONObject j_tmpl) throws Exception {
		int size = 0;
		String s_page = j_tmpl.getString("page");
		if (offset > 0){
		}
		s_page = MessageFormat.format(s_page, new Object[] {offset});
//			url += "?limit=10&offset="+offset;
		String url_full = url + s_page;
		
		Document doc = Jsoup.connect(url_full).referrer(url_full)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
		.timeout(50000).get();
		String div_list = j_tmpl.getString("div_list");;
		// ".bloglist"
		Elements lists = doc.select(div_list);
//		size = lists.size();
		for (Iterator<Element> it = lists.iterator(); it.hasNext(); ){
			Element elm = it.next();
			String href0 = elm.select("a[href]").first().attr("href");
			String time = elm.select("time.published").first().attr("datetime");
			// 2015-08-03T15:12:24+00:00
			long pub_dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(time).getTime();
			if (pub_dt < last_dt){
				break;
			}
			if (newest_dt == 0){
				newest_dt = pub_dt; // rec first
			}
			// 
			size++;
			CONST.log.info("read more " + href0);
			String href = href0.substring(href0.lastIndexOf('/')+1);
			File f_html = new File(tmp_folder, href +".htm");
			if (!f_html.exists()) {
				try {
				WebSpinner.down(href0, f_html);
				}catch (Exception e){
					CONST.log.error("EE", e);
					break;
				}
			} else {
			}
			Document doc_p = Jsoup.parse(new FileInputStream(f_html), "utf-8","");
		    article(href, doc_p, j_tmpl, book);
		}
		return size;
	}
	private void article(String sid, Document doc, JSONObject j_tmpl, Epub book) throws Exception{
		String filter_img = j_tmpl.optString("img");
		String ti_tmpl = j_tmpl.optString("ti");
		String cnt_tmpl = j_tmpl.optString("cnt");
		String cnt_clean = j_tmpl.optString("clean");
		String cnt, ti;
		if (cnt_tmpl.length() > 1){
			cnt = doc.select(cnt_tmpl).first().html();
		}else{
			cnt = doc.body().html();
		}
		if (ti_tmpl.length() > 1){
			ti = doc.select(ti_tmpl).first().text();
		}else{
			ti = doc.title();
		}
		// 
		if (cnt_clean != null && cnt_clean.length() > 1) {
			Whitelist wl = new Whitelist();
			wl.addTags(cnt_clean.split(","));//
			wl.addTags("img").addAttributes("img","src","alt","real_src");
			cnt = Jsoup.clean(cnt, wl);
		}

		InputStream ins = IOUtil.loadTemplate(CONST.TMPL_HTM);
		Document docT = Jsoup.parse(ins, "utf-8","");
//		String ti = j_tmpl.optString("t_name")+" "+j_tmpl.optString("t_time");
//		j_tmpl.remove("t_name");
		docT.select("h2").first().html(ti);
		//.appendElement("p").text(json_config.optString("t_time"));
//		j_tmpl.remove("t_time");
//		docT.select("div").first().html(link);
		docT.select("p").first().html(cnt);

		Elements elm_imgs = docT.select("img");
		if (filter_img.length() > 1 && elm_imgs!=null  &&  elm_imgs.size() > 0 ){
			for (Iterator<Element> itor = elm_imgs.iterator(); itor.hasNext(); ){
				org.jsoup.nodes.Element elm_img = itor.next();
				String img_src = elm_img.attr(elm_img.hasAttr("real_src") ? "real_src": "src");
				String img_nm = img_src.substring(img_src.lastIndexOf('/')+1);
				String file_name = "i"+String.format("%03d", count)+img_nm;
				if (CONST.MIME.HTM.equals( Resource.determineMediaType(file_name) ) ){
					file_name = file_name+".jpg";
				}
				
				count++;
				if ("*.*".equals(filter_img) || file_name.endsWith(filter_img) ){
					File f_img = new File(tmp_folder, file_name);
					CONST.log.debug("img: " + f_img.getAbsolutePath() );
					if (!f_img.exists()) {
						WebSpinner.down(img_src, f_img);
					}
					elm_img.attr("src", file_name);
//					
					Element pp = elm_img.parent();
					elm_img.remove();
					pp.appendElement("div").attr("class", "duokan-image-single").appendChild(elm_img);
//					docT.r
//					String url_img = s_img.substring(i_org, e_org);
//					Element e_img = doc.createElement("img").attr("src",url_img);
//					img(e_img, book);
//					doc.appendElement("div").attr("class", "duokan-image-single").appendChild(e_img);
					
					book.addItem(f_img);
					
				}else{
					CONST.log.info("skip img " + img_src);
				}
			}

		}
		String content = docT.html();
//		int pos1 = href.lastIndexOf('.');
//		if (){}
//		String sid = pos1 > 0 ? href.substring(0, pos1) : href;
		book.addString(sid, ti, content);
	}
	public int next(String id, String url, Epub book, long last_dt, JSONObject j_tmpl) throws Exception {
		int size = 0;
		String s_page = j_tmpl.getString("page");
		
		Document doc;
		doc = Jsoup.connect(url) //.timeout(30000)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
//				.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
//				.header("Accept", "text/html")
//				.data("wd", "Java")
			.timeout(15000).get();
		String div_list = "div.articleList";
		// ".bloglist"
		Element elm_link = doc.select(div_list).first().select("a[href]").first();
		String href0 = elm_link.attr("href");
		j_tmpl.put("nexturl", href0);
		//json_config.put("nexttl", elm_link.text());
		String ext = j_tmpl.optString("ext");
		String filter = j_tmpl.optString("filter");
		String filter_img = j_tmpl.optString("img");
		
		while (j_tmpl.has("nexturl")){
			String link = j_tmpl.optString("nexturl");
			String name;
			j_tmpl.remove("nexturl");
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
				if (!f_html.exists()) {
					try {
					WebSpinner.down(link, f_html);
					}catch (Exception e){
						CONST.log.error("EE", e);
						break;
					}
				} else {
				}
				Document doc_p = Jsoup.parse(new FileInputStream(f_html), "utf-8","");
				
		    	cnt = readHtml(doc_p, j_tmpl);
		    	if ("ERR".equals(cnt)){
		    		break;
		    	}

		    }else{
		    	// need this?
		    	CONST.log.info("skip " + link);
		    	continue;
		    }
			
			
			Whitelist wl = new Whitelist();
			wl.addTags("p","span","pre","code","ul","li");//
			wl.addTags("img").addAttributes("img","src","alt","real_src");
			cnt = Jsoup.clean(cnt, wl);

			InputStream ins = IOUtil.loadTemplate(CONST.TMPL_HTM);
			Document docT = Jsoup.parse(ins, "utf-8","");
			String ti = j_tmpl.optString("t_name")+" "+j_tmpl.optString("t_time");
			j_tmpl.remove("t_name");
			docT.select("h2").first().html(ti);
			//.appendElement("p").text(json_config.optString("t_time"));
			j_tmpl.remove("t_time");
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
						book.addItem(f_img);
						
					}else{
						CONST.log.info("skip img " + img_src);
					}
				}

			}
			}catch(Exception e){
				
			}
			String content = docT.html();
			int pos1 = href.lastIndexOf('.');
//			if (){}
			String sid = pos1 > 0 ? href.substring(0, pos1) : href;
			book.addString(sid, ti, content);
		}
		return 0;
	}
}
