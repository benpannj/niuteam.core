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

public class BlogSpinner {
	private String user = "Ben Pan";
	private Epub bk_all = new Epub();
	private SimpleDateFormat fmt_yyyymmdd = null;
	private SimpleDateFormat fmt_eeedmy = null;
	private String yymmdd = null;
	private int count = 0;

	public void init(){
		Date date_now = new Date();
		fmt_yyyymmdd = new SimpleDateFormat("yyyyMMdd");//HHmmss
	    fmt_eeedmy = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		
//		long now = date_now.getTime(); // System.currentTimeMillis();
		yymmdd = fmt_yyyymmdd.format(date_now);
		bk_all.create(yymmdd, user,"zh");
	}
	// merge epub from web url
	public void rss2epub() throws Exception {
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
}
