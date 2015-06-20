package test.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Iterator;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.epub.Epub;
import niuteam.util.EpubUtil;
import niuteam.util.IOUtil;
import niuteam.util.WebSpinner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebEpub {
	
	/**
	 * download epub from weiphone.com
	 * 1. http://bbs.weiphone.com/read-htm-tid-2300277.html
	 * 2. 
	 * @throws Exception
	 */
	public void createFromWeiphone() throws Exception {
//		String encoding = "utf-8";
		String url = "http://www.tieku.org/58387/1.html";
//		url = "http://bbs.weiphone.com/read-htm-tid-2300277.html";
		WebSpinner.down("bbs.weiphone.com", "3814258", 1);
	        
//		XhtmlDoc doc = new XhtmlDoc();
//		String html = doc.downloadUrlContent(url, encoding);
//		CONST.log.info("" + html);
		File folder = new File("/home/ben/doc/etc");
		File[] fs = folder.listFiles();
		for (File f : fs){
			String epub_txt = f.getName();
			String name = epub_txt.substring(0, epub_txt.lastIndexOf('.'));
			File f_epub = new File(folder, name);
			if (f_epub.exists()) continue;
//			f.renameTo(new File(folder, f.getName()+".txt"));
			InputStream ins = new FileInputStream(f);
			StringWriter out2 = new StringWriter();
			IOUtil.copy(new InputStreamReader(ins, "utf-8"), out2 );
			StringBuffer buf = new StringBuffer(out2.toString());
			int pos = buf.indexOf("remotedown.");
			if (pos < 0){
				continue;
			}
			buf.insert(pos, '/');
			String url_epub = buf.toString();
//			CONST.log.info();
			try {
			WebSpinner.down(url_epub, f_epub);
			}catch(Exception e){
				CONST.log.info("next down: " + e.getMessage());
//				break;
			}
		}
		EpubUtil util = new EpubUtil();
//		util.setEncoding("utf-8");
//		util.web2epub("www.onlylz.com/postcache","13lq",5);

//		util.web2epub("www.tieku.org","199375",300);
//		File[] files = folder.listFiles();
//		for (int i = 0; i < files.length; i++){
//			File f = files[i];
//			if (f.isDirectory() ) {
//				util.folder2epub(f);
//			}
//		}
	}
	public void chanlun() throws Exception {
//		String encoding = "utf-8";
		String site = "http://chanlun.agutong.com";
		File f = new File("etc/case/chanlun.htm");
		if (!f.exists()) return;
		FileInputStream ins = new FileInputStream(f);
		Document doc = Jsoup.parse(ins, "utf-8","");
		Elements hrefs = doc.select("div#index_108ke a");
		Epub bk_all = new Epub();
		bk_all.create("chanlun", "Chan","zh");
		int count = 0;
		for (Iterator<Element> itor = hrefs.iterator(); itor.hasNext(); ){
			org.jsoup.nodes.Element href = itor.next();
			String id = "ke"+String.format("%03d", count);
			count++;
			String title = href.text();
			File f_p = new File(IOUtil.getTempFolder()+"/f/", id+".htm");
			if (!f_p.exists()){
				String p_url = site + href.attr("href");
				CONST.log.debug("down: " + p_url);
				WebSpinner.down(p_url, f_p);
			}
			Document doc_p = Jsoup.parse(new FileInputStream(f_p), "utf-8","");
			Element cnt = doc_p.select("div#sina_keyword_ad_area2").first();
			org.jsoup.select.Elements elm_imgs = cnt.select("img");
			int c_img = 0;
			for (Iterator<Element> it = elm_imgs.iterator(); it.hasNext(); ){
				org.jsoup.nodes.Element elm_img = it.next();
				String img_nm = id+String.format("%03d", c_img);
				c_img++;
				try{
				img(elm_img, img_nm, bk_all, "http://chanlun.agutong.com/chan/gp108ke/108ke/");
				}catch(Exception e){
					CONST.log.debug(""+img_nm, e);
				}
//				elm_img.attributes().removeAttr("");
			}
//			elm_img.attributes().removeAttr("");
			bk_all.addString(id, title, cnt.html());
		}
		File outFile =  new File(IOUtil.getTempFolder(), "ALL.C.epub");
		if (outFile.exists()){
			File destFile =  new File(IOUtil.getTempFolder(), "ALL.C"+System.currentTimeMillis()+".epub");
			outFile.renameTo(destFile );
		}
		bk_all.writeEpub(outFile);

	}
	private Element img(Element elm_img, String nm, Epub bk_all, String parent) throws Exception{
		String img_src = elm_img.attr("src");
		elm_img.removeAttr("real_src");
		elm_img.removeAttr("id");
		int i = img_src.lastIndexOf('.');
		if (i == -1){
			CONST.log.debug("[BAD ]" + elm_img.html() + ", " + nm);
			i = 0;
			return elm_img;
		}
		String img_nm = img_src.substring(i);
		String file_name = nm+img_nm;
		if (CONST.MIME.HTM.equals( Resource.determineMediaType(file_name) ) ){
			file_name = file_name+".jpg";
		}
		elm_img.attr("src", file_name);
		File tmp_folder = new File(IOUtil.getTempFolder()+"/img/");
		File f_img = new File(tmp_folder, file_name);
//		CONST.log.debug("img: " + f_img.getAbsolutePath() );
		if (!f_img.exists()) {
			if (!img_src.startsWith("http")){
				img_src = parent+img_src;
			}
			WebSpinner.down(img_src, f_img);
		}
		bk_all.addItem(f_img);
		return elm_img;
    }
}
