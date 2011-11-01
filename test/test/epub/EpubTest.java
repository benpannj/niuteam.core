package test.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;

import niuteam.book.core.Book;
import niuteam.book.core.CONST;
import niuteam.book.core.Resource;
import niuteam.book.epub.Epub;

import org.junit.Test;

public class EpubTest {

	@Test
	public void testReadEpub() throws Exception {
		File epubFile = new File("/tmp", "test.epub");
//		FileInputStream in = new FileInputStream(epubFile);
		ZipInputStream in = new ZipInputStream(new FileInputStream(epubFile));
		Epub bk = new Epub();
		bk.readEpub(in, CONST.ENCODING);
//		fail("Not yet implemented");
		CONST.log.info("debug {} ", epubFile.getAbsoluteFile() );
	}
	@Test
	public void testReadEpubFile() throws Exception {
		CONST.log.info("testReadEpubFile B -----------------------------");
		File epubFile = new File("/tmp", "test.epub");
//		FileInputStream in = new FileInputStream(epubFile);
		Epub bk = new Epub();
		bk.readEpub(epubFile);
		CONST.log.info("testReadEpubFile E -----------------------------");
	}

	@Test
	public void testWrite() throws Exception {
		// fail("Not yet implemented");
		Epub bk = new Epub();
		Book book = bk.create("Title", "Ben", "cn");
		book.metadata.set("src", "own");
		Resource res = new Resource("id1", "Hello".getBytes(), "id1", CONST.MIME_HTM);
		book.resources.put("id1", res);
		
		book.validate();

		OutputStream out = new FileOutputStream("/tmp/test_wr.zip");
		bk.write(out);
		
		book.metadata.writeOpf();
	}

}
