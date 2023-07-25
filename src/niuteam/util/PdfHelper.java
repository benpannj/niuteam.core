package niuteam.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import niuteam.book.core.CONST;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PRAcroForm;
import com.itextpdf.text.pdf.PRTokeniser;
import com.itextpdf.text.pdf.PdfContentParser;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.SimpleBookmark;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.RenderListener;

public class PdfHelper {
	public static void removeBlankPdfPages(String pdfSourceFile, String pdfDestinationFile)
    {
		int blankPdfsize = 100;
        try
        {
            // step 1: create new reader
            PdfReader r = new PdfReader(pdfSourceFile);
            RandomAccessFileOrArray raf = new RandomAccessFileOrArray(pdfSourceFile);
            Document document = new Document(PageSize.A1, 0, 0, 0, 0);
//            Document document = new Document(r.getPageSizeWithRotation(1));
            // step 2: create a writer that listens to the document
            PdfCopy writer = new PdfCopy(document, new FileOutputStream(pdfDestinationFile));
            // step 3: we open the document
            document.open();
            // step 4: we add content
            PdfImportedPage page = null;


            //loop through each page and if the bs is larger than 20 than we know it is not blank.
            //if it is less than 20 than we don't include that blank page.
            for (int i=1;i<=r.getNumberOfPages();i++)
            {
                //get the page content
                byte bContent [] = r.getPageContent(i,raf);
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                //write the content to an output stream
                bs.write(bContent);
                CONST.log.info("page content length of page "+i+" = "+bs.size());
                //add the page to the new pdf
                if (bs.size() > blankPdfsize)
                {
                    page = writer.getImportedPage(r, i);
                    writer.addPage(page);
                }
                bs.close();
            }
            //close everything
            document.close();
            writer.close();
            raf.close();
            r.close();
        }
        catch(Exception e)
        {
        //do what you need here
        }
    }
	
	// http://sanjaal.com/java/448/java-pdf/splitting-pdf-file-using-java-itext-api-into-multiple-pdfs/
	/**
	 * @param fileName
	 *            : PDF file that has to be splitted
	 * @param splittedPageSize
	 *            : Page size of each splitted files
	 */
	public static void splitPDFFile(String fileName, int splittedPageSize) {
		try {
			/**
			 * Read the input PDF file
			 */
			PdfReader reader = new PdfReader(fileName);
			System.out.println("Successfully read input file: " + fileName
					+ "\n");
			int totalPages = reader.getNumberOfPages();
			System.out.println("There are total " + totalPages
					+ " pages in this input file\n");
			int split = 0;

			float size = 0.6f;
			Rectangle rect = reader.getPageSizeWithRotation(1).rectangle(size, size);
			/**
			 * Note: Page numbers start from 1 to n (not 0 to n-1)
			 */
			for (int pageNum = 1; pageNum <= totalPages; pageNum += splittedPageSize) {
				split++;
				String outFile = fileName
						.substring(0, fileName.indexOf(".pdf"))
						+ "-split-"
						+ split + ".pdf";
				

				Document document = new Document(PageSize.A6, 0, 0, 0, 0);
						//reader.getPageSizeWithRotation(1));
				PdfCopy writer = new PdfCopy(document, new FileOutputStream(
						outFile));
				document.open();
				/**
				 * Each split might contain one or more pages defined by
				 * splittedPageSize
				 * 
				 * E.g. We are splitting a 15 pages pdf to 4 page each. In this
				 * example, the last split will have only 3 pages (4+4+4+3 =15)
				 * 
				 * Note the following condition that handles the scenario where
				 * total number of pages in the splitted file is less that
				 * splittedpageSize
				 * 
				 * It will always be the last split.
				 * 
				 * splittedPageSize && (pageNum+offset) <=totalPages
				 */
				int tempPageCount = 0;
				for (int offset = 0; offset < splittedPageSize
						&& (pageNum + offset) <= totalPages; offset++) {
					PdfImportedPage page = writer.getImportedPage(reader,
							pageNum + offset);
					writer.addPage(page);
					tempPageCount++;
				}

				document.close();
				/**
				 * The following will trigger the PDF file being written to the
				 * system
				 **/
				writer.close();

				System.out.println("Split: [" + tempPageCount + " page]: "
						+ outFile);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tool that can be used to concatenate any number of existing PDF files To
	 * One.
	 */

	public static void mergeMyFiles(String filesToBeMerged[],
			String mergedFileLocation) {

		System.out.println("Starting To Merge Files...");
		System.out.println("Total Number Of Files To Be Merged..."
				+ filesToBeMerged.length + "\n");
		try {
			int pageOffset = 0;
			ArrayList masterBookMarkList = new ArrayList();

			int fileIndex = 0;
			String outFile = mergedFileLocation;
			Document document = null;
			PdfCopy writer = null;
			PdfReader reader = null;

			for (fileIndex = 0; fileIndex < filesToBeMerged.length; fileIndex++) {

				/**
				 * Create a reader for the file that we are reading
				 */
				reader = new PdfReader(filesToBeMerged[fileIndex]);
				System.out.println("Reading File -"
						+ filesToBeMerged[fileIndex]);

				/**
				 * Replace all the local named links with the actual
				 * destinations.
				 */
				reader.consolidateNamedDestinations();

				/**
				 * Retrieve the total number of pages for this document
				 */
				int totalPages = reader.getNumberOfPages();

				/**
				 * Get the list of bookmarks for the current document If the
				 * bookmarks are not empty, store the bookmarks into a master
				 * list
				 */
				System.out.println("Checking for bookmarks...");
				List bookmarks = SimpleBookmark.getBookmark(reader);
				if (bookmarks != null) {
					if (pageOffset != 0)
						SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset,
								null);
					masterBookMarkList.addAll(bookmarks);
					System.out.println("Bookmarks found and storing...");
				} else {
					System.out.println("No bookmarks in this file...");
				}
				pageOffset += totalPages;

				/**
				 * Merging the files to the first file. If we are passing file1,
				 * file2 and file3, we will merge file2 and file3 to file1.
				 */
				if (fileIndex == 0) {
					/**
					 * Create the document object from the reader
					 */
					document = new Document(reader.getPageSizeWithRotation(1));

					/**
					 * Create a pdf write that listens to this document. Any
					 * changes to this document will be written the file
					 * 
					 * outFile is a location where the final merged document
					 * will be written to.
					 */

					System.out.println("Creating an empty PDF...");
					writer = new PdfCopy(document,
							new FileOutputStream(outFile));
					/**
					 * Open this document
					 */
					document.open();
				}
				/**
				 * Add the conent of the file into this document (writer). Loop
				 * through multiple Pages
				 */
				System.out.println("Merging File: "
						+ filesToBeMerged[fileIndex]);
				PdfImportedPage page;
				for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
					page = writer.getImportedPage(reader, currentPage);
					writer.addPage(page);
				}

				/**
				 * This will get the documents acroform. This will return null
				 * if no acroform is part of the document.
				 * 
				 * Acroforms are PDFs that have been turned into fillable forms.
				 */
				System.out.println("Checking for Acroforms");
				PRAcroForm form = reader.getAcroForm();
				if (form != null) {
//					writer.copyAcroForm(reader);
					writer.copyDocumentFields(reader);
					System.out.println("Acroforms found and copied");
				} else
					System.out.println("Acroforms not found for this file");

				System.out.println();
			}
			/**
			 * After looping through all the files, add the master bookmarklist.
			 * If individual PDF documents had separate bookmarks, master
			 * bookmark list will contain a combination of all those bookmarks
			 * in the merged document.
			 */
			if (!masterBookMarkList.isEmpty()) {
				writer.setOutlines(masterBookMarkList);
				System.out.println("All bookmarks combined and added");

			} else {
				System.out.println("No bookmarks to add in the new file");

			}

			/**
			 * Finally Close the main document, which will trigger the pdfcopy
			 * to write back to the filesystem.
			 */
			document.close();

			System.out.println("File has been merged and written to-"
					+ mergedFileLocation);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	private void fixPdf(File folder ) throws Exception{
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++){
			File f = files[i];
			String name = f.getName();
			if (name.charAt(0) == '.') continue;
			if (f.isDirectory() ) fixPdf(f);
			int pos = name.toLowerCase().indexOf(".pdf");
			if (pos <1) continue;
			name = name.substring(0, pos);
			
			PdfReader reader = new PdfReader(new RandomAccessFileOrArray(f.getAbsolutePath()), null);
			List bookmarks = SimpleBookmark.getBookmark(reader);
			if (bookmarks != null) {
				String title = null;
				for (Iterator itor = bookmarks.iterator(); itor.hasNext();) {
					HashMap obj = (HashMap)itor.next();
					if (obj.containsKey("Kids")){
						title = null; // use this title
					}
					if (title == null) {
					}
						title = (String)obj.get("Title");
						title = title.trim();
				}
				if (title != null && title.length()> 0) {
					title = title.trim();
					if (name.indexOf(title) > 0) continue;
					// {Action=GoTo, Page=27 FitBH 484, Title=爵位名称的由来 Kids=array} 
					CONST.log.info("[dd] " + title +" data :"+ bookmarks  );
					File newFile = new File(folder, name+"."+title+".pdf");
					if (! newFile.exists()) {
						System.out.println("rename: " + f.getAbsolutePath() );
						f.renameTo(newFile);
					}
				}
			}
		}
	}
	
//	public void parsePdf(String src, String dest) throws IOException {
//        PdfReader reader = new PdfReader(src);
//        // we can inspect the syntax of the imported page
//        byte[] streamBytes = reader.getPageContent(1);
//        PRTokeniser tokenizer = new PRTokeniser(streamBytes);
//        PrintWriter out = new PrintWriter(new FileOutputStream(dest));
//        while (tokenizer.nextToken()) {
//            if (tokenizer.getTokenType() == PRTokeniser.TK_STRING) {
//                out.println(tokenizer.getStringValue());
//            }
//        }
//        out.flush();
//        out.close();
//    }
	
//	 public void extractText(String src, String dest) throws IOException {
//	        PrintWriter out = new PrintWriter(new FileOutputStream(dest));
//	        PdfReader reader = new PdfReader(src);
//	        RenderListener listener = new MyTextRenderListener(out);
//	        PdfContentStreamProcessor processor = new PdfContentStreamProcessor(listener);
//	        PdfDictionary pageDic = reader.getPageN(1);
//	        PdfDictionary resourcesDic = pageDic.getAsDict(PdfName.RESOURCES);
//	        processor.processContent(ContentByteUtils.getContentBytesForPage(reader, 1), resourcesDic);
//	        out.flush();
//	        out.close();
//	    }	
//	 
	 
//	 public void parsePdf(String pdf, String txt) throws IOException {
//	        PdfReader reader = new PdfReader(pdf);
//	        PdfContentParser parser = new PdfContentParser(reader);
//	        PrintWriter out = new PrintWriter(new FileOutputStream(txt));
//	        TextExtractionStrategy strategy;
//	        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
//	            strategy = parser.processContent(i, new LocationTextExtractionStrategy());
//	            out.println(strategy.getResultantText());
//	        }
//	        out.flush();
//	        out.close();
//	    }	 
	public static String pdf2txt(String pdfSourceFile){
		StringBuilder buf = new StringBuilder(300);
//		int blankPdfsize = 100;
//		String encoding = "gbk";
        try
        {
            // step 1: create new reader
            PdfReader r = new PdfReader(pdfSourceFile);
            RandomAccessFileOrArray raf = new RandomAccessFileOrArray(pdfSourceFile);
//            Document document = new Document(r.getPageSizeWithRotation(1));
            // step 2: create a writer that listens to the document
            // step 3: we open the document
            // step 4: we add content
//            PdfImportedPage page = null;


            //loop through each page and if the bs is larger than 20 than we know it is not blank.
            //if it is less than 20 than we don't include that blank page.
//        	PdfTextExtractor p = new PdfTextExtractor(r);
        	 // PdfReaderContentParser
            for (int i=1;i<=r.getNumberOfPages();i++)
            {
            	String page = PdfTextExtractor.getTextFromPage(r, i);
//            	CONST.log.info(" "+ page );
            	buf.append(page);
                //get the page content
//                byte bContent [] = r.getPageContent(i,raf);
//                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                //write the content to an output stream
//                bs.write(bContent);
//                CONST.log.info("page content length of page "+i+" = "+bs.size());
                //add the page to the new pdf
//                if (bs.size() > blankPdfsize)
//                  	CONST.log.info(" "+ new String( bContent,  encoding) );
//                bs.close();
            }
            //close everything
            raf.close();
            r.close();
        }catch(Exception e){
        //do what you need here
        }
        return buf.toString();
    }	
}
