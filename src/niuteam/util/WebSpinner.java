package niuteam.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import niuteam.book.core.CONST;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class WebSpinner {
	public static boolean mergeTmpl(String url, File fout, String next_url) throws Exception{
		Document doc = Jsoup.connect(url)
//				.data("wd", "Java")
				  .timeout(5000)
				  .get();
		String title = doc.title();
//		Element h2 = doc.select("h2").first();
//		String t2 = h2.text();
		Element e_d = doc.select("div#content").first();
		Elements aus = e_d.select("div.author");
		for (Element au : aus) {
			au.text("");
		}
		String t_d = e_d.html();
		Whitelist epub = new Whitelist();
		epub.addTags("p","pre","br","li","ul","h1","h2","h3");
		String content = Jsoup.clean(t_d, epub);

		boolean next = true;
		// <a href="/199375/2.html">
		Element e_pg = doc.select("div.pg").first();
		next = e_pg.select("a[href$="+next_url+"]").size() > 0;
		
		
		InputStream ins = IOUtil.loadTemplate(CONST.TMPL_HTM);
//		StringWriter out2 = new StringWriter();
//		IOUtil.copy(new InputStreamReader(ins, "utf-8"), out2 );
		Document docT = Jsoup.parse(ins, "utf-8","");
		docT.select("h2").first().html(title);
		docT.select("p").first().html(content);

		String html = docT.html();

		Writer fwu = new OutputStreamWriter(new FileOutputStream(fout), "utf-8");
		fwu.write(html);
		fwu.flush();
		fwu.close();
		return next;
	}
	public static void down(String site, String tid, int patge) throws Exception{
		File f = new File("/tmp/"+tid+".html");
		Document doc;
		if (!f.exists()){
			String url = "http://bbs.weiphone.com/"+"read-htm-tid-"+tid+".html";
			doc = Jsoup.connect(url)
					.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
					.header("Accept", "text/html")
//					.data("wd", "Java")
					  .timeout(15000)
					  .get();
			FileOutputStream output = new FileOutputStream(f);
			byte[] imgdata = doc.html().getBytes();
			output.write(imgdata);
//		if (encoding == null) {
//		} else {
//			String s = request.body();
//			String t = request.charset();
//			print("- %s %s %s", request.contentType(), t, encoding);
//			if (t == null){
//				t = encoding;
//			}
//			output.write(s.getBytes(t) );
//		}
//		FileUtils.writeByteArrayToFile(new File("/tmp/"+file_name), imgdata);
		output.flush();
		output.close();			
		} else{
			doc = Jsoup.parse(f, "utf-8");
		}

//		Document docHtml = Jsoup.connect(url)
//				  .data("query", "Java")
//				  .userAgent("Mozilla")
//				  .cookie("auth", "token")
//				  .timeout(3000)
//				  .post();
//		Document doc = Jsoup.parse(safe);
		
		Elements links = doc.select("a[href]");
		IOUtil.print("\nLinks: (%d)", links.size());
		for (Element link : links) {
            String href = link.text();
            if (href!= null && href.endsWith(".epub")){
    			try {
            	downEpub(link, "gbk");
    			}catch(Exception e){
    				IOUtil.print(" * a: <%s>  (%s)", link.attr("abs:href"), IOUtil.trim(link.text(), 35));
    				
    			}
//            	break;
            }
		}
		Elements media = doc.select("[src]");

		IOUtil.print("\nMedia: (%d)", media.size());
        for (Element src : media) {
        	String encoding = null;
            if (src.tagName().equals("img")) {
//                print(" * %s: <%s> %sx%s (%s)",
//                        src.tagName(), src.attr("abs:src"), src.attr("width"), src.attr("height"),
//                        trim(src.attr("alt"), 20));
            }else{
//            	encoding = "utf-8";
            	encoding = "gbk";
                String href = src.attr("abs:src");
                if (href!= null && href.endsWith(".epub")){
                	IOUtil.print(" * %s: <%s>", src.tagName(), href);
                }
            }
        }
//		Response request = Jsoup.connect(src).referrer(src)
//				.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
//				.execute();
//				byte[] imgdata = request.bodyAsBytes();
//				FileUtils.writeByteArrayToFile(new File("get.png"), imgdata);
	}
	
	private static void downImg(Element src, String encoding) throws Exception {
		String url = src.attr("abs:src");
		String file_name = url.substring(url.lastIndexOf('/')+1);
		Response request = Jsoup.connect(url).referrer(url)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
		.execute();
		FileOutputStream output = new FileOutputStream(new File("/tmp/"+file_name));
			byte[] imgdata = request.bodyAsBytes();
			output.write(imgdata);
//		if (encoding == null) {
//		} else {
//			String s = request.body();
//			String t = request.charset();
//			print("- %s %s %s", request.contentType(), t, encoding);
//			if (t == null){
//				t = encoding;
//			}
//			output.write(s.getBytes(t) );
//		}
//		FileUtils.writeByteArrayToFile(new File("/tmp/"+file_name), imgdata);
		output.flush();
		output.close();
	}
	private static void downEpub(Element link, String encoding) throws Exception {
		String f_name = link.text();
		File file = new File("/tmp/etc/"+f_name);
		if (file.exists()) return;
		String ref = "http://bbs.weiphone.com/read-htm-tid-2300277.html";
		String url = link.attr("abs:href");
		url+="&check=1&nowtime="+System.nanoTime()+"&verify=2a6442c3";
		Document doc = Jsoup.connect(url).referrer(ref)
				.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
				.header("Accept", "text/html")
//				.data("wd", "Java")
				  .timeout(13000)
				  .get();
		String ajax = doc.text();
		Document docT = Jsoup.parseBodyFragment(ajax);
		IOUtil.print("--  %s", ajax);
		Elements links = docT.select("a[href]");
		IOUtil.print("\nLinks: (%d)", links.size());
		for (Element link2 : links) {
            String href = link2.attr("abs:href");
            String href1 = link2.attr("href");
            IOUtil.print(" *2 a: <%s>  (%s)", href1,  IOUtil.trim(link2.text(), 35));
            if (href1.contains("zjmcc")){
            	String epub_url = "http://bbs.weiphone.com/"+href1;
            	IOUtil.print(" save %s from %s", f_name, epub_url);
				FileOutputStream output = new FileOutputStream(file);
    			byte[] imgdata = epub_url.getBytes();
    			output.write(imgdata);
    			output.flush();
    			output.close();
    			break;
            }
//            if (href!= null && href.endsWith(".epub")){
//            	downEpub(link, "gbk");
//            }
		}
//		Elements elmAjax = doc.select("ajax");
//		String file_name = link.text();
//		String s1 = elmAjax.outerHtml();
//		print("s1  %s", s1);
//		String file_name = url.substring(url.lastIndexOf('/')+1);
//		Response request = Jsoup.connect(url).referrer(url)
//		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
//		.execute();
//		FileOutputStream output = new FileOutputStream(new File("/tmp/"+file_name));
//			byte[] imgdata = request.bodyAsBytes();
//			output.write(imgdata);
//		if (encoding == null) {
//		} else {
//			String s = request.body();
//			String t = request.charset();
//			print("- %s %s %s", request.contentType(), t, encoding);
//			if (t == null){
//				t = encoding;
//			}
//			output.write(s.getBytes(t) );
//		}
//		FileUtils.writeByteArrayToFile(new File("/tmp/"+file_name), imgdata);
//		output.flush();
//		output.close();
	}	
	public static void down(String url, File f) throws Exception{
		CONST.log.info("begin down: " + url);
		Response request = Jsoup.connect(url).referrer(url)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
		.timeout(300000).execute();
		FileOutputStream output = new FileOutputStream(f);
		byte[] imgdata = request.bodyAsBytes();
			output.write(imgdata);
//		if (encoding == null) {
//		} else {
//			String s = request.body();
//			String t = request.charset();
//			print("- %s %s %s", request.contentType(), t, encoding);
//			if (t == null){
//				t = encoding;
//			}
//			output.write(s.getBytes(t) );
//		}
//		FileUtils.writeByteArrayToFile(new File("/tmp/"+file_name), imgdata);
		output.flush();
		output.close();
	}
	public static JSONObject downJson(String url) throws Exception{
		CONST.log.info("begin down: " + url);
		Response request = Jsoup.connect(url).referrer(url)
		.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6")
		.execute();
		
		String type = request.contentType();
		//byte[] imgdata = request.bodyAsBytes();
		
		String body = request.body();
		if (type.contains("json")){
			JSONObject json ;
			if (body.charAt(0) == '['){
				// this is array list
				JSONArray ary = new JSONArray(body);
				CONST.log.debug(" " + ary.toString(2));
				json = ary.getJSONObject(0);
			}else {
			 json = new JSONObject(body);
			}
			CONST.log.debug(" " + json.toString(2));
			return json;
		} else {
			CONST.log.debug(""+ type + "\n" + body);
			return null;
		}
		
//		File f = new File(IOUtil.getTempFolder(), "aaa.json");
//		
//		FileOutputStream output = new FileOutputStream(f);
//		output.write(imgdata);
//		output.flush();
//		output.close();	
		//return "";
	}
}
