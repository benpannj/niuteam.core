package test.epub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;

import niuteam.book.core.CONST;
import niuteam.book.core.XhtmlDoc;
import niuteam.book.epub.Epub;

import org.junit.Test;

import com.adobe.epubcheck.api.EpubCheck;
import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.util.DefaultReportImpl;

public class EpubTest {

//	@Test
	public void testReadEpubFile() throws Exception {
		CONST.log.info("testReadEpubFile B -----------------------------");
		
		File epubFile = new File("/tmp", "todo.epub");
		// checkEpub(epubFile.getAbsolutePath());
//		FileInputStream in = new FileInputStream(epubFile);
		Epub bk = new Epub();
		bk.readEpub(epubFile);

		bk.addString("test.htm", "<html><body>Hello, world!</body></html>");

		File outFile =  new File("/tmp", "test_w.epub");
		bk.writeEpub(outFile);
		checkEpub(outFile.getAbsolutePath());
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
		File folder = new File("/tmp"); 
		// /mnt/DOC/Book/K85/epub
		// /tmp
		String s = folder.getAbsolutePath();
		fixEpub(folder);
		CONST.log.info("testFixEpubFile E ----------------------------- {}", s);
	}
	private void fixEpub(File folder)  {
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			File epubFile = files[i];
			if (epubFile.isDirectory() ){
				// fixEpub(epubFile);
				continue;
			}
			String name = epubFile.getName();
			// BEGIN
			if (name.endsWith(".epub.old")){
				File ep = new File(folder, name.substring(0, name.length()-4));
				if (ep.exists()){
					CONST.log.info("exit "  + ep.getAbsolutePath());
				} else {
					CONST.log.debug("no " + ep.getAbsolutePath() );
					try {
					epubFile.renameTo(ep);
					epubFile.createNewFile();
					}catch(Exception e){}
				}
			}
			// END
			
			if (!name.endsWith(".epub")){ continue;}
			File backup = new File(folder, name+".old");
			if (backup.exists()) continue;

			try {
			Epub bk = new Epub();
			bk.readEpub(epubFile);
			if (bk.isDirty() ){
				File outFile =  new File(folder, name + ".zip");
				bk.writeEpub(outFile);
				epubFile.renameTo(backup);
				outFile.renameTo(new File(folder, name));
				// checkEpub(epubFile.getAbsolutePath());
			} else {
//				epubFile.renameTo(backup);
				backup.createNewFile();
			}
			} catch (Exception e){
				CONST.log.error("BAD: " + epubFile.getAbsolutePath(), e );
				epubFile.renameTo(new File(folder, name+"_BAD.old.zip"));
			}
		}
		
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
		checkEpub(outFile.getAbsolutePath());
		CONST.log.info("testCreateEpubFile E -----------------------------");
	}
	public static void checkEpub(String epubName) {
		CONST.log.info("checkEpub -----------------------------");
		Report report = new DefaultReportImpl(epubName);
		if (!epubName.endsWith(".epub"))
			report.warning(null, 0, "filename does not include ‘.epub’ suffix");

		EpubCheck check = new EpubCheck(new File(epubName), report);
		if (check.validate())
			System.out.println("No errors or warnings detected");
		else {
			System.err.println("\nCheck finished with warnings or errors!\n");
		}
	}

	@Test
	public void testCreateFromFolder() throws Exception {
		CONST.log.info("testCreateFromFolder B -----------------------------");
		File folder = new File("/tmp/liangJian");
		String encoding = "gb2312";
		Epub bk = new Epub();
		bk.create("Title: Liang", "Ben Pan","cn");
//		bk.setMetadata(CONST.DCTags.subject, "Ben Pan subject ");
//		bk.setMetadata(CONST.DCTags.title, "test title ");
		bk.setMetadata(CONST.DCTags.meta, folder.getAbsolutePath() );
//		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test 22");
		File[] files = folder.listFiles();
		Arrays.sort(files, new Comparator<File>(){
		    public int compare(File f1, File f2)
		    {
		    	return f1.getName().compareToIgnoreCase(f2.getName());
//		        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
		    } });
		for (int i = 0; i < files.length; i++){
			File f = files[i];
			if (f.isDirectory() ) continue;
			XhtmlDoc doc = new XhtmlDoc(files[i], encoding);

			doc.analyzeTitle("<h1>", "</h1>");
			doc.analyzeContent("<div id=content2>", "</div>");
			File fout = new File(folder, "p_" + f.getName() );
			doc.mergeTmpl(fout);
			bk.addItem(fout);
		}
		File outFile =  new File("/tmp", "test_folder_create.epub");
		bk.writeEpub(outFile);
		checkEpub(outFile.getAbsolutePath());
		CONST.log.info("testCreateFromFolder E -----------------------------");
	}
	
	public void testhtm2bk() throws Exception{
		File f1 = new File("/tmp", "test_h.htm" );
		String encoding = "gb2312";
		CONST.log.info("" + f1.getAbsolutePath());
		XhtmlDoc doc = new XhtmlDoc(f1, encoding);
//		String bookCatalogUrl = "http://java2s.com/Code/Java/File-Input-Output/Writingdelimitedtextdatatoafileorastream.htm";
//		String bookCatalogUrl = "http://tldp.org/LDP/abs/html/";
//		String bookCatalogHtml = doc.downloadUrlContent(bookCatalogUrl, "utf-8");
//		FileOutputStream outs1 = new FileOutputStream(f1);
//		outs1.write( bookCatalogHtml.getBytes());
//		outs1.close();
		String html = doc.getHtml();
		String chapterText = doc.cleanHtml(html);
		File f = new File("/tmp", "test_h2.htm" );
		CONST.log.info("" + f.getAbsolutePath());
		Writer fw = (encoding == null)? new FileWriter(f): new OutputStreamWriter(new FileOutputStream(f), encoding);
		fw.write(chapterText );
		fw.flush();
		fw.close();
		
		// 
		doc.analyzeTitle("<h1>", "</h1>");
		doc.analyzeContent("<div id=content2>", "</div>");
		File fout = new File("/tmp", "test_h2_utf8.htm" );

		doc.mergeTmpl(fout);


//		chapterText = bookCatalogHtml.replaceAll("&nbsp;", " ");
////		chapterText = chapterText.replaceAll("<.*?>", "");
//		chapterText = chapterText.replaceAll("<b>", " ");
//		chapterText = chapterText.replaceAll("</b>", " ");
//		// class=
//		chapterText = chapterText.replaceAll(" class=.*?>", ">");
//		String[] tags = new String[] {"input", "font", "textarea", "br","table","tr","td","tbody","a", "form"};
//		for (int i = 0; i < tags.length; i++){
//			String s = tags[i];
//			String start = "<"+s+".*?>";
//			String end = "</"+s+">";
//			chapterText = chapterText.replaceAll(start, " ");
//			chapterText = chapterText.replaceAll(end, " ");
//			chapterText = chapterText.replaceAll(start.toUpperCase(), " ");
//			chapterText = chapterText.replaceAll(end.toUpperCase(), " ");
//		}
//		String[] tags_crlf = new String[] {"link", "script"};
//		for (int i = 0; i < tags_crlf.length; i++){
//			String s = tags_crlf[i];
//			String start = "<"+s+".*?>";
//			String end = "</"+s+">";
//			chapterText = chapterText.replaceAll(start, "\r\n");
//			chapterText = chapterText.replaceAll(end, "\r\n");
//			chapterText = chapterText.replaceAll(start.toUpperCase(), "\r\n");
//			chapterText = chapterText.replaceAll(end.toUpperCase(), "\r\n");
//		}
//		chapterText = chapterText.replaceAll("  ", " ");
		// if size > 300K, should split into two or more.
//		String chapterText = doc.getChapterText(bookCatalogHtml);
//		chapterText = chapterText.replaceAll("</p><p>", "\n");
//		chapterText = chapterText.replaceAll("<p>", "");
//		chapterText = doc.html2txt(chapterText.replaceAll("</p>", "")).trim();

		
		
		//		FileOutputStream outs = new FileOutputStream(f);
//		outs.write( chapterText.getBytes(encoding));
//		outs.close();

//		String[] chapterIds = doc.getChapterIds(bookCatalogHtml, "");
//		for (int i = 0; i < 3000 && i < chapterIds.length; i++) {
//			doc.addChapter("", chapterIds[i]);// 添加章节
//		}
	}
	
}
