package test.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import niuteam.book.core.CONST;
import niuteam.util.EpubUtil;
import niuteam.util.IOUtil;
import niuteam.util.WebSpinner;

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
	
}
