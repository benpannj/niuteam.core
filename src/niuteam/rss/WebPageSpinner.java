package niuteam.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import niuteam.book.core.CONST;
import niuteam.book.epub.Epub;
import niuteam.util.IOUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class WebPageSpinner {
	private String user = "Ben Pan";
	private static int count = 0;
	public void init(){
		
	}
	// merge epub from web url
	public void webpage2epub() throws Exception {
		File f = new File("etc/book.htm.json");
		if (!f.exists()){
			CONST.log.error("no conf: " + f.getAbsolutePath());
			return;
		}
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(new FileInputStream(f)), out );
		JSONObject json = new JSONObject(out.toString());
		user = json.optString("user");
		JSONArray ary = json.getJSONArray("web");
		for (int i = 0, len = ary.length(); i < len; i++){
			JSONObject j = ary.getJSONObject(i);
			String id = j.optString("id");
			if (id.length() < 1) continue;
			File tmp_folder = new File(IOUtil.getTempFolder()+"/f", id);
			if (tmp_folder.exists()) {
			} else {
				tmp_folder.mkdirs();
			}
			try{
			String title = j.optString("title");
			String ext = j.optString("ext");
			if ("true".equals(ext)){
				// this is index url
				String url = j.optString("url");
				CONST.log.info("load .. " + id + ", " + title + ", " + url);
				readIndexPages(url, id, title, j);
			} else {
				JSONArray urls = j.getJSONArray("url");
				CONST.log.info("load .. " + id + ", " + title + ", " + urls);
				readPages(urls, id, title, j);
			}
			}catch(Exception e){
				CONST.log.error("", e);
			}
		}
	}	
	public void readPages(JSONArray urls,String id, String title, JSONObject json_config) throws Exception{
		long now = System.currentTimeMillis();
		File outFile =  new File(IOUtil.getTempFolder(), id+now+".epub");
		Epub bk = new Epub();
		if (outFile.exists()){
			File destFile =  new File(IOUtil.getTempFolder(), id+now+"a.epub");
			outFile.renameTo(destFile );
			bk.readEpub(destFile);
		} else {
			bk.create(title, user,"zh");
		}
		for (int i = 0, len = urls.length(); i < len; i++){
			String url = urls.getString(i);
			int pos = url.lastIndexOf('[');
			if (pos == -1){
				readHtml(url, json_config, bk);
				continue;
			}
			// [03-09]
			int pos1 = url.indexOf('-', pos);
			int pos2 = url.indexOf(']', pos);
			if (pos1 == -1 || pos2 == -1) {
				// error!
				continue;
			}
			String pre = url.substring(0, pos);
			String from = url.substring(pos+1, pos1);
			int f = Integer.parseInt(from);
			String to = url.substring(pos1+1, pos2);
			int t = Integer.parseInt(to);
			String end = url.substring(pos2+1);
			String fmt = "%0"+from.length()+"d";// "%03d"
			for (int iii = f; iii <= t; iii++){
				String uu = pre+String.format(fmt, iii)+end;
				readHtml(uu, json_config, bk);
			}
//			int  = from.length();
		}	
		bk.writeEpub(outFile);
		CONST.log.info(" Merge ok, send out. --"+ outFile.getAbsolutePath()  );
	}
	private List pages = new ArrayList();
//	private String site = "";
	public void readHtml(String full_url, JSONObject json_config,Epub bk) throws Exception{
		if (full_url.contains("pr03.html")){
			count++;
		}
		String cnt = json_config.optString("cnt");
		org.jsoup.nodes.Document doc;
		String url = full_url;
		int pos = full_url.indexOf("#");
		if (pos < 0){
			// new page
		}else if (pos == 0){
			return;
		} else if (pos > 1){
			url = full_url.substring(0,  pos);
		}
		CONST.log.info("" + full_url +", " + cnt + ", "+ url);
		if (pages.contains(url)){
			return;
		}
		pages.add(url);
		String encoding = json_config.optString("encoding", CONST.ENCODING);
//		File f = new File(IOUtil.getTempFolder()+"/f",url);
//		if (!f.exists()){
//		}else{
//			doc = Jsoup.parse(f, encoding);
//		}
		doc = Jsoup.connect(url)
			.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
			.header("Accept", "text/html")
//			.data("wd", "Java")
			  .timeout(15000)
			  .get();
//			return;
		// index
		Element elm_index = null;
		String index = json_config.optString("index");
//		index = "div.toc";
		if (index.length() > 1){
			elm_index = doc.select(index).first();;
		}
		// content
		Element elm;
		String title = doc.title();
		if (cnt.length() > 1){
			elm = doc.select(cnt).first();
		}else{
			elm = doc.body();
		}
		// remove
		String skip = json_config.optString("skip");
		if (skip!=null && skip.length()>0){
			Elements links = elm.select(skip);
			for (Element link : links) {
				link.remove();
			}
		}
		String s;
		s = elm.html();

		Whitelist wl = new Whitelist();
		wl.addTags("p","span","pre","br","li","dl","ul","dt");//
		wl.addTags("a").addAttributes("href");
		s = Jsoup.clean(s, wl);
		//
		s.replaceAll("&nbsp;", " ");

		InputStream ins = IOUtil.loadTemplate("OEBPS/Text/c_00.htm");
		Document docT = Jsoup.parse(ins, "utf-8","");
		docT.select("h2").first().html(title);
//		docT.select("div").first().html(dt);
		docT.select("p").first().html(s);
		String content = docT.html();
		count++;
		String href = "p"+String.format("%03d", count)+".htm";				
		bk.addString(href, title, content);
		
		if (elm_index != null){
			Elements links = elm_index.select("a[href]");
			IOUtil.print("\nLinks: (%d)", links.size());
			String filter = json_config.optString("filter");
			String site = null;
			if (url.endsWith("/")){
				site = url;
			}else{
				int pos2 = url.lastIndexOf('/');
				site = url.substring(0, pos2+1);
			}
			for (Element link : links) {
//	            String href = link.text();
	            String href2 = link.attr("abs:href");
	            String href3 = link.attr("href");
	            if (href3!= null && href3.endsWith(filter) && !href3.contains("/")){
	            	String url3 = site + href3;
	    			try {
	    				readHtml(url3, json_config, bk);
	    			}catch(Exception e){
	    				IOUtil.print(" * a: <%s>  (%s)", url3, IOUtil.trim(link.text(), 35));
	    			}
//	            	break;
	            }
			}			
		}
	}
	public void readIndexPages(String urls,String id, String title, JSONObject json_config) throws Exception{
		long now = System.currentTimeMillis();
		File outFile =  new File(IOUtil.getTempFolder(), id+now+".epub");
		Epub bk = new Epub();
		if (outFile.exists()){
			File destFile =  new File(IOUtil.getTempFolder(), id+now+"a.epub");
			outFile.renameTo(destFile );
			bk.readEpub(destFile);
		} else {
			bk.create(title, user,"zh");
		}
		String encoding = json_config.optString("encoding", CONST.ENCODING);
		File f = new File(IOUtil.getTempFolder()+"/f",id+".htm");
		Document doc;
		if (!f.exists()){
			String url = urls;
			doc = Jsoup.connect(url)
					.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
					.header("Accept", "text/html")
//					.data("wd", "Java")
					  .timeout(15000)
					  .get();
			FileOutputStream output = new FileOutputStream(f);
			byte[] imgdata = doc.html().getBytes(encoding);
			output.write(imgdata);
			output.flush();
			output.close();			
		} else{
			doc = Jsoup.parse(f, encoding);
		}		
		Elements links = doc.select("a[href]");
		IOUtil.print("\nLinks: (%d)", links.size());
		String filter = json_config.optString("filter");
		String site = null;
		if (urls.endsWith("/")){
			site = urls;
		}else{
			int pos = urls.lastIndexOf('/');
			site = urls.substring(0, pos+1);
		}
		for (Element link : links) {
//            String href = link.text();
            String href2 = link.attr("abs:href");
            String href = link.attr("href");
            if (href!= null && href.endsWith(filter) && !href.contains("/")){
            	String url = site + href;
    			try {
    				readHtml(url, json_config, bk);
    			}catch(Exception e){
    				IOUtil.print(" * a: <%s>  (%s)", url, IOUtil.trim(link.text(), 35));
    			}
//            	break;
            }
		}
		bk.writeEpub(outFile);
		CONST.log.info(" Merge ok, send out. --"+ outFile.getAbsolutePath()  );
	}
}
