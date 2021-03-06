package niuteam.util;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipFile;

import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.core.TextResource;
import niuteam.book.core.XhtmlDoc;
import niuteam.book.epub.Epub;

public class EpubUtil {
	private int count = 100;
	public void fixEpub(File folder, boolean deep)  {
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			File epubFile = files[i];
			if (epubFile.isDirectory() ){
				if (deep) 	fixEpub(epubFile, deep);
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
			ZipFile zf = bk.readEpub(epubFile);
			if (bk.isDirty() ){
				File outFile =  new File(folder, name + ".zip");
				bk.writeEpub(outFile);
				zf.close();
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
//	public static void checkEpub(String epubName) {
//		CONST.log.info("checkEpub -----------------------------");
//		Report report = new DefaultReportImpl(epubName);
//		if (!epubName.endsWith(".epub"))
//			report.warning(null, 0, "filename does not include ���.epub��� suffix");
//
//		EpubCheck check = new EpubCheck(new File(epubName), report);
//		if (check.validate())
//			System.out.println("No errors or warnings detected");
//		else {
//			System.err.println("\nCheck finished with warnings or errors!\n");
//		}
//	}
	private String encoding = CONST.ENCODING;
	public void setEncoding(String s){
		this.encoding = s;
	}
	public void folder2epub( File folder) throws Exception {
		Epub bk = new Epub();
		count = 100;
		String title = folder.getName();
		File tmp_folder = new File(IOUtil.getTempFolder()+"/f", title);
		if (tmp_folder.exists()) {
			
		} else {
			tmp_folder.mkdirs();
		}
		bk.create(title, "Ben Pan","zh");
//		bk.setMetadata(CONST.DCTags.subject, "Ben Pan subject ");
//		bk.setMetadata(CONST.DCTags.title, "test title ");
		bk.setMetadata(CONST.DCTags.meta, folder.getAbsolutePath() );
//		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test 22");
		addFolder(folder, bk, tmp_folder,"p_");
		File outFile =  new File(IOUtil.getTempFolder(), title+".epub");
		if (outFile.exists()){
			outFile =  new File(IOUtil.getTempFolder(), title+"."+System.currentTimeMillis()+".epub");
		}
		bk.writeEpub(outFile);
//		bk.compact();
//		checkEpub(outFile.getAbsolutePath());
		CONST.log.info(" E --"+ outFile.getAbsolutePath());
	}
	private void addFolder(File folder, Epub bk, File tmp_folder, String prefix)
			throws Exception {
		File[] files = folder.listFiles();
		Arrays.sort(files, new Comparator<File>(){
		    public int compare(File f1, File f2)
		    {
		    	return f1.getName().compareToIgnoreCase(f2.getName());
//		        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
		    } });
		for (int i = 0; i < files.length; i++){
			File f = files[i];
			if (f.isDirectory() ) {
				addFolder(f, bk, tmp_folder, prefix+f.getName());
				count++;
				continue;
			}
			String name = f.getName().toLowerCase();
			long len = f.length();
			if (len < 10) continue;
			String type = Resource.determineMediaType(name);
			if (CONST.MIME.HTM.endsWith( type )) {
//				int pos = name.lastIndexOf(".txt");
				boolean is_htm = name.endsWith(".htm") || name.endsWith(".html");
				if (is_htm){
//					XhtmlDoc doc = new XhtmlDoc(f, encoding);
//					doc.analyzeTitle("<TITLE>", "</TITLE>", name.substring(0, name.length()-4));
//					doc.analyzeContent("<TD CLASS=ART>", "<DIV class=FL>");
//	//				doc.analyzeContent("<!--HTMLBUILERPART0-->", "<!--/HTMLBUILERPART0-->"); //"<blockquote>", "</blockquote>"
//					File fout = new File(tmp_folder, prefix + name );
//					doc.mergeTmpl(fout);
//					bk.addItem(fout);
					bk.addItem(f);
				} else {
					String id = "p" + count+"."+String.format("%03d", i);
					TextResource res = new TextResource(id);
					res.loadFile(f, encoding, id);
					bk.addResource(res);
//					continue;
				}
			} else if (CONST.MIME.CSS.equals(type)){
			} else if (CONST.MIME.PNG.equals(type) || CONST.MIME.GIF.equals(type) || CONST.MIME.JPG.equals(type)){
				bk.addItem(f);
			} else {
				bk.addItem(f);
				CONST.log.info("unknown: " + name);
			}
		}
	}
	// merge epub from web url
	public void web2epub(String site, String title, int page) throws Exception {
		Epub bk = new Epub();
		File tmp_folder = new File(IOUtil.getTempFolder()+"/f", title);
		if (tmp_folder.exists()) {
			
		} else {
			tmp_folder.mkdirs();
		}
		bk.create(title, "Ben Pan","zh");
//		bk.setMetadata(CONST.DCTags.subject, "Ben Pan subject ");
//		bk.setMetadata(CONST.DCTags.title, "test title ");
		bk.setMetadata(CONST.DCTags.meta, site+"/"+title);
//		bk.setMetadata(CONST.DCTags.meta, "Ben Pan  meta test 22");
		int last_len = 0;
		for (int i = 1; i < page; i++){
			File fout = new File(tmp_folder, "p_"+ String.format("%04d", i)+".htm" );
			if (!fout.exists()){
			String url = "http://"+site+"/"+title+"/";
//			if(i == 1){
//				url += "page.html";
//			} else {
//				url += "page-"+i+".html";
//			}
			url = url+ i+ ".html"; // tieku.com
			String next_url = ""+ (i+1)+ ".html"; // tieku.com
			boolean next = WebSpinner.mergeTmpl(url, fout, next_url);
//			XhtmlDoc doc = new XhtmlDoc();
//			String html = doc.downloadUrlContent(url, encoding);
//			doc.analyzeTitle("<title>", "</title>", "TITLE");
//			doc.analyzeContent("<div id=\"content\">", "<div class=\"pg\">");
////			doc.analyzeContentMulti("<div class=\"postinfo\">", "</div>", false);
////			doc.analyzeContent("<!--HTMLBUILERPART0-->", "<!--/HTMLBUILERPART0-->"); //"<blockquote>", "</blockquote>"
//			doc.mergeTmpl(fout);
			bk.addItem(fout);
			if (!next){
				break;
			}

//			if (html.indexOf("下一页") == -1){
//				break;
//			}
//			if (last_len == html.length()){
//				CONST.log.info("REQ: " + url);
//				break;
//			} else {
//				last_len = html.length();
//				}
			} else {
			bk.addItem(fout);
			}
		}
		
		File outFile =  new File(IOUtil.getTempFolder(), title+".epub");
		bk.writeEpub(outFile);
//		checkEpub(outFile.getAbsolutePath());
		CONST.log.info(" E --"+ outFile.getAbsolutePath());
	}
	public void file2epub( File folder) throws Exception {
		File tmp_folder = new File(IOUtil.getTempFolder()+"/f");
		if (tmp_folder.exists()) {
			
		} else {
			tmp_folder.mkdirs();
		}
		File dest_folder = new File(IOUtil.getTempFolder(),"dest");
		if (dest_folder.exists() && dest_folder.isDirectory()) {
			
		} else {
			dest_folder.mkdirs();
		}

		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++){
			File f = files[i];
			if (f.isDirectory() ) {
				continue;
			}
			Epub bk = new Epub();
			String name = f.getName();
			String title = name.substring(0, name.length()-4);
			bk.create(title, "Ben Pan","zh");
			bk.setMetadata(CONST.DCTags.meta, f.getAbsolutePath() );
			String type = Resource.determineMediaType(name);
			if (CONST.MIME.HTM.endsWith( type )) {
//				int pos = name.lastIndexOf(".txt");
				if (name.endsWith(".txt")){
					String id = "p" + count+"."+String.format("%03d", i);
					TextResource res = new TextResource(id);
					res.loadFile(f, encoding, id);
					bk.addResource(res);
				} else {
					XhtmlDoc doc = new XhtmlDoc(f, encoding);
					doc.analyzeTitle("<TITLE>", "</TITLE>", title);
					doc.analyzeContent("<TD CLASS=ART>", "<DIV class=FL>");
	//				doc.analyzeContent("<!--HTMLBUILERPART0-->", "<!--/HTMLBUILERPART0-->"); //"<blockquote>", "</blockquote>"
					File fout = new File(tmp_folder, name );
					doc.mergeTmpl(fout);
					bk.addItem(fout);
				}
			} else if (CONST.MIME.CSS.equals(type)){
			} else if (CONST.MIME.PNG.equals(type) || CONST.MIME.GIF.equals(type) || CONST.MIME.JPG.equals(type)){
				bk.addItem(f);
			} else {
				bk.addItem(f);
				CONST.log.info("unknown: " + name);
			}
			
			File outFile =  new File(dest_folder, title+".epub");
			bk.writeEpub(outFile);
//			bk.compact();
//			checkEpub(outFile.getAbsolutePath());
			CONST.log.info(" E --"+ outFile.getAbsolutePath());
		}

	}	
}
