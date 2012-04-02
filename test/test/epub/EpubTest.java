package test.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import niuteam.book.core.CONST;
import niuteam.book.core.XhtmlDoc;
import niuteam.book.epub.Epub;
import niuteam.image.Exif;
import niuteam.util.EpubUtil;
import niuteam.util.IOUtil;
import niuteam.util.WebSpinner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.itextpdf.text.pdf.PRTokeniser;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.SimpleBookmark;

public class EpubTest {

//	@Test
	public void testReadEpubFile() throws Exception {
		File folder = new File("/tmp/etc");
		File[] fs = folder.listFiles();
		EpubUtil util = new EpubUtil();
//		util.fixEpub(folder, false);
		for (File epubFile : fs) {
			String name = epubFile.getName();
			if (!name.endsWith(".epub")) continue;
			File backup = new File(folder, name+".zip");
			if (backup.exists()) continue;
			CONST.log.info("testReadEpubFile B -------------------------- " + name);
//			File epubFile = new File("/tmp/etc", "1.epub");
			// checkEpub(epubFile.getAbsolutePath());
	//		FileInputStream in = new FileInputStream(epubFile);
			Epub bk = new Epub();
			bk.readEpub(epubFile);
			bk.compact();
	//		bk.addString("test.htm", "<html><body>Hello, world!</body></html>");
			epubFile.renameTo(backup);
			File outFile =  new File(folder, name);
			bk.writeEpub(outFile);
//			EpubUtil.checkEpub(outFile.getAbsolutePath());
		}
		CONST.log.info("testReadEpubFile E -----------------------------");
	}
//	@Test
	public void testFixEpubFile() throws Exception {
		CONST.log.info("testFixEpubFile B -----------------------------");
		
//		OpfResource opf = new OpfResource();
//		InputStream in = new FileInputStream("/tmp/OEBPS/content.opf");
//		// IOUtil.loadTemplate(CONST.FILE_OPF)
//		Document docOpf = XmlUtil.stream2doc(in);
//		opf.readXml(CONST.FILE_OPF, docOpf, null);
//		CONST.log.info("  dirty?  " + opf.isDirty());
//		String s = XmlUtil.node2String(opf.getDoc());
		File folder = new File("/tmp/etc");
//		File folder = new File("/home/ben/doc/etc");
		// /mnt/DOC/Book/K85/epub
		// /tmp
		EpubUtil util = new EpubUtil();
		String s = folder.getAbsolutePath();
		util.fixEpub(folder, false);
		CONST.log.info("testFixEpubFile E ----------------------------- {}", s);
	}

	
//	@Test
	public void testCreateEpubFile() throws Exception {
		CONST.log.info("testCreateEpubFile B -----------------------------");
		Epub bk = new Epub();
		bk.create("Ben", "Ben 1","en");
		bk.setMetadata(CONST.DCTags.subject, "Ben Pan subject ");
		bk.setMetadata(CONST.DCTags.title, "test title ");
		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test ");
		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test 22");
		// add chapter 1 
		File f = new File("/tmp", "c_01.htm");
		bk.addItem(f);
		// 
		 bk.addString("test.htm", "<html><body>Hello, world!</body></html>");
		
		File outFile =  new File("/tmp", "test_create.epub");
		bk.writeEpub(outFile);
		EpubUtil.checkEpub(outFile.getAbsolutePath());
		CONST.log.info("testCreateEpubFile E -----------------------------");
	}

//	@Test
	public void testCreateFromFolder() throws Exception {
		CONST.log.info("testCreateFromFolder B -----------------------------");
//		getTitleFromHhc("s");
		File folder = new File("/tmp/etc/txt");
		EpubUtil util = new EpubUtil();
		util.setEncoding("utf-8");
		util.folder2epub(folder);
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
	public void testCreateFromTieku() throws Exception {
		String encoding = "utf-8";
		entry(true);
//		StringWriter out = new StringWriter();
//		IOUtil.copy(new InputStreamReader(new FileInputStream("/tmp/etc/gaoh.html"), encoding), out );
//		String html = out.toString();
//		Whitelist epub = new Whitelist();
//		epub.addTags("p","pre","br","li","ul");
//		String safe = Jsoup.clean(html, epub);
//		Writer fwu = new OutputStreamWriter(new FileOutputStream("/tmp/etc/g.htm"), "utf-8");
//		fwu.write(safe);
//		fwu.flush();
//		fwu.close();

//		getTitleFromHhc("s");
		String url = "http://zh.wikipedia.org";
//		url = "http://www.tieku.org/199375/1.html";
//		url = "http://bbs.weiphone.com/read-htm-tid-2300277.html";
//		WebSpinner.down("bbs.weiphone.com", "2300277", 1);
	        
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
			WebSpinner.down(url_epub, f_epub, 1);
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
	@Test
	public void testMergeEpub() throws Exception{
		Epub bk = new Epub();
//		
		File folder = new File("/tmp/etc");
		count = 0;
		mergeFolder(bk, folder);
		
//		File f1 = new File("/tmp", "10.epub");
//		bk.readEpub(f1);
//		bk.addString("test2.htm", "<html><body>Hello, world!</body></html>");
//		File f2= new File("/tmp", "11.epub");
//		bk.addEpub(f2, "bk"+count);
//
//		// Ba Ba De You Xi Ge Ming ----Wan Chu Cong - Edu
//		bk.addString("test3.htm", "<html><body>3</body></html>");
//		bk.addEpub(new File("/tmp", "3.epub") );
//		bk.addString("test4.htm", "<html><body>4</body></html>");
//		bk.addEpub(new File("/tmp", "4.epub") );
//		bk.addString("test5.htm", "<html><body>5</body></html>");
//		bk.addEpub(new File("/tmp", "5.epub") );
//		bk.addString("test6.htm", "<html><body>6</body></html>");
//		bk.addEpub(new File("/tmp", "6.epub") );

		File outFile =  new File("/tmp", "test_merge.epub");
		bk.writeEpub(outFile);
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
					
					if (f.length() > 400000) {
						CONST.log.info("skip size " +  f.length() );
						continue;
					}
					if (count == 0) {
						bk.readEpub(f);
					} else {
						bk.addString("_test_"+count+ ".htm", "<html><body><h1>"+ name + "</h1></body></html>");
						bk.addEpub(f, "bk"+String.format("%02d", count) );
					}
					count++;
				}
			}
		}
	}	


	// @Test
	public void testPdf() throws Exception{
		File folder = new File("/mnt/DOC/Book2/5 哲学");
//		fixPdf(folder);
		
//		gulong_list.htm
		
		File f1 = new File("/tmp", "i62.pdf" );
//		PdfHelper.splitPDFFile(f1.getAbsolutePath(), 40);
//		PdfHelper.removeBlankPdfPages(f1.getAbsolutePath(), "/tmp/bak.pdf");
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
//			 f1.renameTo(new File("/tmp", f1.getName()+"."+title+".pdf"));
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
            	CONST.log.info("type {} " + type, val);
            }
        }
        out.flush();
        String s = out.toString();
        String strTarget = new String (s.getBytes(Charset.forName("utf-8")), "utf-8");
        CONST.log.info("s  " + strTarget);
	}
//	@Test
	public void testImgExif() throws Exception {
		CONST.log.info("testImgExif B -----------------------------");
		Exif e = new Exif("/tmp/pic");
		File f = new File("/tmp/pic");
//		e.dump(f);
		e.organize(f);
//		File base_folder = new File("/mnt/DOC/Private/PanQing/2011");
//		File jpegFile = new File(base_folder, "/T0_1/IMG_2583.JPG");
//		e.organize(new File("/mnt/DOC/Private/PanQing/2011/201108"));
//		e.dump(jpegFile);
		CONST.log.info("testImgExif E -----------------------------");
	}
	
}
