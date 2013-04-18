package niuteam.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import niuteam.book.core.CONST;
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

public class RssSpinner {
	private String user = "Ben Pan";
	public void init(){
		
	}
	// merge epub from web url
	public void rss2epub() throws Exception {
		File f = new File("etc/bookrss.conf");
		if (!f.exists()){
			CONST.log.error("no conf: " + f.getAbsolutePath());
			return;
		}
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

			long last_dt = readRss(url, id, title, old_last_dt, j);
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
		}
	}	
	public long readRss(String url,String id, String title, long last_dt, JSONObject json_config) throws Exception{
		long now = System.currentTimeMillis();
		long newest_dt = last_dt;
		File outFile =  new File(CONST.tmp_folder, id+now+".epub");
		Epub bk = new Epub();
		if (outFile.exists()){
			File destFile =  new File(CONST.tmp_folder, id+now+".epub");
			outFile.renameTo(destFile );
			bk.readEpub(destFile);
		} else {
			bk.create(title, user,"zh");
		}
//		File f = new File("/tmp/i21st.html");
		Response resp = Jsoup.connect(url) //.timeout(30000)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
				.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
				.header("Accept", "text/html")
//				.data("wd", "Java")
				  .timeout(15000)
		.execute();
		String s = resp.body();
		
		JSONObject json = XML.toJSONObject(s);
	    SimpleDateFormat f = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		String last_build_date = Json.getNodeVal(json, "/rss/channel/lastBuildDate");
		if (last_build_date!=null){
			long l = f.parse(last_build_date).getTime();
		    CONST.log.info("lastBuildDate - "  + l + ", "+last_dt+", " + newest_dt);
		    if (l <= last_dt) return 0;

		}
		String ext = json_config.optString("ext");
		String filter = json_config.optString("filter");
		String filter_img = json_config.optString("img");
		
//		JSONObject j_chanel = json.getJSONObject("channel/lastBuildDate");
		JSONArray ary = Json.findArray(json, "/rss/channel/item");
		if (ary == null) return 0;
		for (int i = 0, len = ary.length(); i < len; i++){
			JSONObject j = ary.getJSONObject(i);
			String link = j.optString("link");
			String dt = j.optString("pubDate"); //// Thu, 1 Nov 2012 16:13:06
		    long l = f.parse(dt).getTime();
		    CONST.log.info(" - "  + l + ", "+last_dt+", " + newest_dt);
		    if (l <= last_dt) continue;
		    if (l > newest_dt){
		    	newest_dt = l;
		    }

		    // href
		    int pos = link.lastIndexOf('/');
			if (pos > 0){
				String kk = link.substring(pos);
			}
			String cnt;
		    
			String ti = j.optString("title");
		    
		    if ("true".equals(ext) ) {
		    	// use content:encoded
		    }
		    cnt = j.optString("content:encoded", null);
			if (cnt !=null){
				// use this
			}else if (link.endsWith(filter)){
		    	cnt = readHtml(link, json_config);
		    }else{
		    	// need this?
		    	CONST.log.info("skip " + link);
		    	continue;
		    }

			Whitelist wl = new Whitelist();
			wl.addTags("p","span");//
			wl.addTags("img").addAttributes("img","src","alt");
			cnt = Jsoup.clean(cnt, wl);

			InputStream ins = IOUtil.loadTemplate("OEBPS/Text/c_00.htm");
			Document docT = Jsoup.parse(ins, "utf-8","");
			docT.select("h2").first().html(ti);
			docT.select("div").first().html(dt);
			docT.select("p").first().html(cnt);
			File tmp_folder = new File(CONST.tmp_folder+"/f", id);
			if (tmp_folder.exists()) {
			} else {
				tmp_folder.mkdirs();
			}

			try{
			Elements elm_imgs = docT.select("img");
			if (filter_img.length() > 1 && elm_imgs!=null  &&  elm_imgs.size() > 0 ){
				for (Iterator<Element> itor = elm_imgs.iterator(); itor.hasNext(); ){
					org.jsoup.nodes.Element elm_img = itor.next();
					String img_src = elm_img.attr("src");
					String file_name = "i"+img_src.substring(img_src.lastIndexOf('/')+1);
					if ( file_name.endsWith(filter_img) ){
						File f_img = new File(tmp_folder, file_name);
						if (!f_img.exists()) {
							WebSpinner.down(img_src, f_img);
						}
						elm_img.attr("src", file_name);
						bk.addItem(f_img);
					}else{
						
					}
				}

			}
			}catch(Exception e){
				
			}
			String content = docT.html();
			String href = "p"+String.format("%03d", i)+"."+now+".htm";				
			bk.addString(href, ti, content);
		}	
		if (newest_dt > last_dt){
			bk.writeEpub(outFile);
			CONST.log.info(" Merge ok, send out. --"+ outFile.getAbsolutePath() + ","+ (newest_dt-last_dt) );
		}
//		checkEpub(outFile.getAbsolutePath());
		return newest_dt;
	}
	public String readHtml(String url, JSONObject json_config) throws Exception{
		String cnt = json_config.optString("cnt");
		org.jsoup.nodes.Document doc;
		doc = Jsoup.connect(url)
				.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
				.header("Accept", "text/html")
//				.data("wd", "Java")
				  .timeout(15000)
				  .get();
//		String html = doc.html();
//		File f = new File("/tmp/i21st.html");
//		FileOutputStream output = new FileOutputStream(f);
//		byte[] imgdata = request.bodyAsBytes();
//		output.write(html.getBytes());
//		output.flush();
//		output.close();
		String s;
		if (cnt.length() > 1){
			s = doc.select(cnt).first().html();
		}else{
			s = doc.body().html();
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
 
            CONST.log.info("No of RSS links found", " " + links.size());
 
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
