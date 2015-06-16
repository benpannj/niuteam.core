package test.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;

import junit.framework.TestCase;
import niuteam.book.core.CONST;
import niuteam.book.epub.Epub;
import niuteam.image.Exif;
import niuteam.rss.BlogSpinner;
import niuteam.rss.RssSpinner;
import niuteam.rss.ZhihuSpinner;
import niuteam.util.DocxHelper;
import niuteam.util.EpubUtil;
import niuteam.util.IOUtil;
import niuteam.util.PdfHelper;
import niuteam.util.WebSpinner;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import com.itextpdf.text.pdf.PRTokeniser;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.SimpleBookmark;

public class EpubTest extends TestCase {
	static{
		Properties prop = new Properties();
		prop.setProperty("log4j.rootLogger", "debug, stdout");
		prop.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		prop.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		String pat = 
//				"%-5p %d{ISO8601} (%C:%L) (%F:%L) %M %x - %m\n";
		 "%5p (%F:%L) %M() - %m%n";
		prop.setProperty("log4j.appender.stdout.layout.ConversionPattern", pat);

				
		org.apache.log4j.PropertyConfigurator.configure(prop);
		File tmp_folder = new File(IOUtil.getTempFolder());
		if (tmp_folder.exists()) {
			
		} else {
			tmp_folder.mkdirs();
		}
		
	}

//	
	public void _testReadEpubFile() throws Exception {
		File folder = new File(IOUtil.getTempFolder(),"txt");
		if (!folder.exists()){
			folder.mkdirs();
			return;
		}
		
		File[] fs = folder.listFiles();
		EpubUtil util = new EpubUtil();
//		util.fixEpub(folder, false);
		for (File epubFile : fs) {
			String name = epubFile.getName();
			if (!name.endsWith(".epub")) continue;
			File backup = new File(folder, name+".zip");
			if (backup.exists()) continue;
			CONST.log.info("testReadEpubFile B -------------------------- " + name);
//			File epubFile = new File("IOUtil.getTempFolder()/etc", "1.epub");
			// checkEpub(epubFile.getAbsolutePath());
	//		FileInputStream in = new FileInputStream(epubFile);
			Epub bk = new Epub();
			ZipFile zf = bk.readEpub(epubFile);
			bk.compact();
	//		bk.addString("test.htm", "<html><body>Hello, world!</body></html>");
			File outFile =  new File(folder, name+".epub");
			bk.writeEpub(outFile);
//			EpubUtil.checkEpub(outFile.getAbsolutePath());
			zf.close();
			CONST.log.info("backup to " + backup.getAbsolutePath());
			boolean ok = epubFile.renameTo(backup);
			
			if (!ok){
				CONST.log.info("BAD backup to " + epubFile.getAbsolutePath() + ", " + epubFile.exists());
			}
			ok = outFile.renameTo(epubFile);
		}
		CONST.log.info("testReadEpubFile E -----------------------------");
	}
//	
	public void _testFixEpubFile() throws Exception {
		CONST.log.info("testFixEpubFile B -----------------------------");
		
//		OpfResource opf = new OpfResource();
//		InputStream in = new FileInputStream("IOUtil.getTempFolder()/OEBPS/content.opf");
//		// IOUtil.loadTemplate(CONST.FILE_OPF)
//		Document docOpf = XmlUtil.stream2doc(in);
//		opf.readXml(CONST.FILE_OPF, docOpf, null);
//		CONST.log.info("  dirty?  " + opf.isDirty());
//		String s = XmlUtil.node2String(opf.getDoc());
		File folder = new File(IOUtil.getTempFolder(),"txt");
//		File folder = new File("/home/ben/doc/etc");
		// /mnt/DOC/Book/K85/epub
		// IOUtil.getTempFolder()
		EpubUtil util = new EpubUtil();
		String s = folder.getAbsolutePath();
		util.fixEpub(folder, false);
		CONST.log.info("testFixEpubFile E ----------------------------- "+ s);
	}

	
//	
	public void _testCreateEpubFile() throws Exception {
		CONST.log.info("testCreateEpubFile B -----------------------------");
		Epub bk = new Epub();
		bk.create("Ben", "Ben 1","zh");
//		bk.setMetadata(CONST.DCTags.subject, "Ben Pan subject ");
//		bk.setMetadata(CONST.DCTags.title, "test title ");
//		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test ");
//		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test 22");
		// add chapter 1 
//		File f = new File(IOUtil.getTempFolder(), "git.htm");
//		bk.addItem(f);
		// 
		 bk.addString("test.htm", "test title", "<html><body>Hello, world!</body></html>");
		
		File outFile =  new File(IOUtil.getTempFolder(), "test_create.epub");
		bk.writeEpub(outFile);
//		EpubUtil.checkEpub(outFile.getAbsolutePath());
		CONST.log.info("testCreateEpubFile E -----------------------------");
	}

//	@Test
	public void _testCreateFromFolder() throws Exception {
		CONST.log.info("testCreateFromFolder B -----------------------------");
//		getTitleFromHhc("s");
		File folder = new File(IOUtil.getTempFolder(), "txt");
		if (!folder.exists()){
			folder.mkdirs();
			return;
		}
		EpubUtil util = new EpubUtil();
//		util.setEncoding("gbk");
		util.folder2epub(folder);
//		util.file2epub(folder);
//		File[] files = folder.listFiles();
//		for (int i = 0; i < files.length; i++){
//			File f = files[i];
//			if (f.isDirectory() ) {
//				util.folder2epub(f);
//			}
//		}
	}
	long start, end;
//	@Test
	public void _testCreateFromTieku() throws Exception {
		String encoding = "utf-8";
		entry(true);
//		StringWriter out = new StringWriter();
//		IOUtil.copy(new InputStreamReader(new FileInputStream("IOUtil.getTempFolder()/etc/gaoh.html"), encoding), out );
//		String html = out.toString();
//		Whitelist epub = new Whitelist();
//		epub.addTags("p","pre","br","li","ul");
//		String safe = Jsoup.clean(html, epub);
//		Writer fwu = new OutputStreamWriter(new FileOutputStream("IOUtil.getTempFolder()/etc/g.htm"), "utf-8");
//		fwu.write(safe);
//		fwu.flush();
//		fwu.close();

//		getTitleFromHhc("s");
//		String url = "http://zh.wikipedia.org";
//		url = "http://www.tieku.org/58387/1.html";

		EpubUtil util = new EpubUtil();
//		util.setEncoding("utf-8");
//		util.web2epub("www.onlylz.com/postcache","13lq",5);

		// 199375 
		// 58387
		// http://www.tieku001.com/226559/1.html
		util.web2epub("www.tieku.org","199375",300);
//		File[] files = folder.listFiles();
//		for (int i = 0; i < files.length; i++){
//			File f = files[i];
//			if (f.isDirectory() ) {
//				util.folder2epub(f);
//			}
//		}
		entry(false);
	}
	private void leave() {
	}
	private void entry(boolean in) {
		if (in) {
		CONST.log.info("testCreateFromTieku B -----------------------------" );
		start = System.nanoTime() ;
		} else {
			end = System.nanoTime() ;
			long diff = end-start;
			CONST.log.info("testCreateFromTieku E -----------------------------" + diff);
		}
	}
	private int count = 0;
//	@Test
	public void _testMergeEpub() throws Exception{
//		
		File folder = new File(IOUtil.getTempFolder(),"txt");
		if (!folder.exists()){
			folder.mkdirs();
			return;
		}
		count = 0;
		CONST.log.info(" merge begin: ");
		Epub bk = new Epub();
		mergeFolder(bk, folder);
		
//		File f1 = new File(IOUtil.getTempFolder(), "10.epub");
//		bk.readEpub(f1);
//		bk.addString("test2.htm", "<html><body>Hello, world!</body></html>");
//		File f2= new File(IOUtil.getTempFolder(), "11.epub");
//		bk.addEpub(f2, "bk"+count);
//
//		// Ba Ba De You Xi Ge Ming ----Wan Chu Cong - Edu
//		bk.addString("test3.htm", "<html><body>3</body></html>");
//		bk.addEpub(new File(IOUtil.getTempFolder(), "3.epub") );
//		bk.addString("test4.htm", "<html><body>4</body></html>");
//		bk.addEpub(new File(IOUtil.getTempFolder(), "4.epub") );
//		bk.addString("test5.htm", "<html><body>5</body></html>");
//		bk.addEpub(new File(IOUtil.getTempFolder(), "5.epub") );
//		bk.addString("test6.htm", "<html><body>6</body></html>");
//		bk.addEpub(new File(IOUtil.getTempFolder(), "6.epub") );

//		bk.compact();

		File outFile =  new File(IOUtil.getTempFolder(), "test_merge.epub");
		bk.writeEpub(outFile);
		CONST.log.info(" E --"+ outFile.getAbsolutePath());
	}
	private void mergeFolder(Epub bk, File folder) throws Exception {
		File[] files = folder.listFiles();
		if (files.length <2) return;
		for (int i = 0; i < files.length; i++){
			File f = files[i];
			if (f.isDirectory() ) {
				mergeFolder(bk, f);
			} else {
				String name = f.getName();
				if (name.endsWith(".epub")){
					
					if (f.length() > 7000000) {
						CONST.log.info("skip size " +  f.length() );
						continue;
					}
					if (count == 0) {
						bk.readEpub(f);
//						bk.compact();
					} else {
						bk.addString("_test_"+count+ ".htm",name, "<html><body><h1>"+ name + "</h1></body></html>");
						bk.addEpub(f, "bk"+String.format("%02d", count) );
					}
					count++;
				}
			}
		}
	}	


	// @Test
	public void _testPdf() throws Exception{
//		File folder = new File("/mnt/DOC/Book2/5 哲学");
//		fixPdf(folder);
		String ss = PdfHelper.pdf2txt(IOUtil.getTempFolder()+"/p/ANSI_X12_850_purchase_order.pdf");
		
		CONST.log.debug(ss);
//		gulong_list.htm
		
		File f1 = new File(IOUtil.getTempFolder(), "r79-C.pdf" );
		if (!f1.exists()){
			return;
		}
//		PdfHelper.splitPDFFile(f1.getAbsolutePath(), 40);
//		PdfHelper.removeBlankPdfPages(f1.getAbsolutePath(), "IOUtil.getTempFolder()/bak.pdf");
//		PdfReader reader = new PdfReader(new FileInputStream(f1));
		PdfReader reader = new PdfReader(new RandomAccessFileOrArray(f1.getAbsolutePath()), null);
//		reader.consolidateNamedDestinations();
		// print meta info
		HashMap map= reader.getInfo();
		for (Iterator i = map.keySet().iterator();i.hasNext();) {
		    String key = (String)i.next();
		    if ("Title".equals(key)){
		    	Object o = map.get(key);
		    	String s = (String)o;
		    	String strTarget = new String (s.getBytes(Charset.forName("gb2312")), "utf-8");
		    	CONST.log.info( "title  " +strTarget + ", " + o.getClass().getName());
		    }
		    System.out.println(key + " :  " + (String)map.get(key));
		}
//		ArrayList masterBookMarkList = new ArrayList();
		int pageOffset = 0;
		int totalPages = reader.getNumberOfPages();
		System.out.println("Checking for bookmarks...");
		List bookmarks = SimpleBookmark.getBookmark(reader);
//		if (pageOffset != 0)
//			SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset,null);
		if (bookmarks != null) {
			String title = "";
			for (Iterator i = bookmarks.iterator(); i.hasNext();) {
				HashMap obj = (HashMap)i.next();
				title = (String)obj.get("Title");
				title = title.trim();
				if (obj.containsKey("Kids")){
					List kids = (List)obj.get("Kids");
//					title = (String)obj.get("Title");
//					title = title.trim();
					CONST.log.info("kids] " + kids  );
				}
				// {Action=GoTo, Page=27 FitBH 484, Title=爵位名称的由来 Kids=array} 
			}
			CONST.log.info("[dd] " + title +" data :"+ bookmarks  );
//			 f1.renameTo(new File(IOUtil.getTempFolder(), f1.getName()+"."+title+".pdf"));
			System.out.println("Bookmarks found and storing...");
			return;
		}else{
			System.out.println("No bookmarks in this file...");
		}
		pageOffset += totalPages;
		
//		reader.getPageN(0).getAsString(arg0)
		byte[] streamBytes = reader.getPageContent(1);
        PRTokeniser tokenizer = new PRTokeniser(streamBytes);
        StringWriter out = new StringWriter();
        while (tokenizer.nextToken()) {
            int type = tokenizer.getTokenType();
            String val = tokenizer.getStringValue();
			if (type == PRTokeniser.TK_STRING) {
				out.write(val);
            } else if ( type == PRTokeniser.TK_NUMBER ){
				// out.write(val);
            } else {
            	CONST.log.info("type " + type + val);
            }
        }
        out.flush();
        String s = out.toString();
        String strTarget = new String (s.getBytes(Charset.forName("utf-8")), "utf-8");
        CONST.log.info("s  " + strTarget);
	}
//	@Test
	public void _testImgExif() throws Exception {
		CONST.log.info("testImgExif B -----------------------------");
		String base_folder = "/mnt/DOC/temp/IPAD/base"; 
		// "/mnt/DOC/Private/PanQing/2013"
		Exif e = new Exif(base_folder);
		String temp_folder =  "/mnt/DOC/temp/IPAD/IBM";
		// "/mnt/DOC/Private/PanQing/new";
		File f = new File(temp_folder);
		if (f.exists() && f.isDirectory()){
//		e.dump(f);
			e.organize(f);
		} else {
			CONST.log.info("" + f.exists() + ",  " + f.isDirectory() + ", " + f.getAbsolutePath());
		}
//		File base_folder = new File("/mnt/DOC/Private/PanQing/2011");
//		File jpegFile = new File(base_folder, "/T0_1/IMG_2583.JPG");
//		e.organize(new File("/mnt/DOC/Private/PanQing/2011/201108"));
//		e.dump(jpegFile);
		CONST.log.info("testImgExif E -----------------------------");
	}
//	@Test
	public void _testWebWpub()  throws Exception {
		WebEpub e = new WebEpub();
		e.createFromWeiphone();
	}
	public void testRssEpub()  throws Exception {
//		String url = "http://zhuanlan.zhihu.com/api/columns/agBJB/posts/20074303";
//		
//		String s = WebSpinner.downZip(url);
//		CONST.log.debug(s);
		
//		RssSpinner e = new RssSpinner();
//		e.rss2epub();

//		WebPageSpinner e = new WebPageSpinner();
//		e.webpage2epub();
		
//		BlogSpinner e = new BlogSpinner();
//		e.rss2epub();

		ZhihuSpinner e = new ZhihuSpinner();
		e.init();
//		File f = new File(IOUtil.getTempFolder(), "aaa.htm");
//		if (!f.exists()) return;
//		FileInputStream ins = new FileInputStream(f);
//		byte[] b = IOUtil.toByteArray(ins);
//		String r1 = e.analyzeZhuanlan(new String(b) );
//		CONST.log.debug(r1);
//		ins.close();
		
//		Document doc = Jsoup.parse(ins, "utf-8","");
		String r = e.downZhuanlan("agBJB");
		CONST.log.debug(r);
	}
	public void _testJsoup() throws Exception{
		String cnt = "div#content";
		//<div id="content">  "div#content"
		//<div class="content">  "div.content"
		// http://www.accuitysolutions.com/en/Footer-Pages/Calendar-of-Holidays/
		FileInputStream ins = new FileInputStream(new File(IOUtil.getTempFolder(), "2011.html"));
		Document doc = Jsoup.parse(ins, "utf-8","");
//		String s11 = doc.select(cnt).first().html();
//		if (s11 != null){
//			CONST.log.debug(s11);
//		}
		Elements items2 = doc.select("table");
		CONST.log.info( "found " + items2.size() );
		Elements items = doc.select("ul");
		int size = items.size();
		for (int i = 0; i< size; i++){
			Element elm = items.get(i);
//		for (Element elm = items.first(); elm != null;elm = elm.nextElementSibling()){
			String v = elm.parent().parent().parent().select("td").first().text();
//			Elements lis = elm.children();
//			for (Element li = lis.first(); li!=null; li = li.nextElementSibling()){
//				String t = li.text();
//				int pos = t.length();
//				while (pos > 0){
//					pos--;
//					int c = t.charAt(pos);
//					if (c > 256){
//						String cnty = t.substring(0, pos);
//						String day = t.substring(pos+1);
//						CONST.log.info(" " + cnty + ", " + day+": " + v);
//						break;
//					}
//				}
//				if (pos == 0){
//					CONST.log.info(" " + v + ", " + t);
//				}
//			}
			CONST.log.info("v " + v);
		}
//		String s = doc.select(cnt).first().html();
		String s = doc.html();

		Whitelist wl = new Whitelist();
		wl.addTags("html","head","body");
		wl.addTags("p","h2");//,"span"
		wl.addTags("img").addAttributes("img","src","alt");
		cnt = Jsoup.clean(s, wl);
		cnt = cnt.replaceAll("(&nbsp;)", " ");
		CONST.log.info(""+ cnt);
	}
	public void _testDocx() throws Exception {
		DocxHelper d = new DocxHelper();
		d.toPdf();
	}
	public void _testIbmMqCode() throws Exception {
		JSONObject root = new JSONObject();
		String encoding = "utf-8";
		String site = "http://www-01.ibm.com/support/knowledgecenter/api/content/SSFKSJ_7.5.0/com.ibm.mq.tro.doc/";
		File fd_local = new File(IOUtil.getTempFolder(), "mq");
		// http://www-01.ibm.com/support/knowledgecenter/api/content/SSFKSJ_7.5.0/com.ibm.mq.tro.doc/q040710_.htm
		FileInputStream ins = new FileInputStream(new File(fd_local, "mq_reasoncode.html"));
		Document doc = Jsoup.parse(ins, encoding,"");
		Elements items = doc.select("a.xref");
		int size = items.size();
		for (int i = 0; i< size; i++){
			Element elm = items.get(i);
			String v = elm.text();
			String[] ss = v.split(" ", 4);
//			if (ss.length < 4){
//				CONST.log.info("\r\n\r\n --- Code " + v);
//				continue;
//			}
//			if (!v.startsWith("6106")){
//				continue;
//			}
			String ref = elm.attr("href");
			CONST.log.info("Code " + ss[0] +", " + ss[ss.length-1] +", " + ref);

			File f = new File(fd_local, ref);
			Document d_f;
			if (!f.exists()){
				String url = site + ref;
				d_f = Jsoup.connect(url)
						.userAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.10.229 Version/11.61")
						.header("Accept", "text/html")
//						.data("wd", "Java")
						  .timeout(15000)
						  .get();
				FileOutputStream output = new FileOutputStream(f);
				byte[] bin = d_f.html().getBytes(encoding);
				output.write(bin);
				output.flush();
				output.close();			
			} else{
				d_f = Jsoup.parse(f, encoding);
			}		
			JSONObject json = new JSONObject();
			json.put("Code", ss[ss.length-1]);
			Elements divs = d_f.select("div.section");
			for (Iterator<Element> itor = divs.iterator(); itor.hasNext();){
				Element div = itor.next();
				Element e_h2 = div.select("h2").first();
				String h2 = e_h2.text();
				e_h2.remove();
				String p = div.text(); // div.select("p").first().text();
				if ("Completion Code".equalsIgnoreCase(h2)){
//					"".equalsIgnoreCase(anotherString)
//					CONST.log.debug("CompCode " + p);
					json.put("CompCode", p);
				}else if ("Programmer response".equalsIgnoreCase(h2)){
//					CONST.log.debug("Resp " + p);
					json.put("Resp", p);
				}else if ("Explanation".equalsIgnoreCase(h2)){
					json.put("Explan", p);
//					CONST.log.debug("Explan " + p);
				} else {
					CONST.log.debug("Type " + h2);
				}
			}
			root.put(ss[0], json);	
		}
//		json
		File f = new File(fd_local, "mq_reasoncode.json");
		FileOutputStream output = new FileOutputStream(f);
		output.write(root.toString(2).getBytes());
		output.flush();
		output.close();
		
	}
}
