package niuteam.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class DocxHelper {
	public void toPdf() throws Exception {
		File f_docx = new File( "/tmp/out.docx" ) ;
		File f_pdf = new File( "/tmp/out.pdf" ) ;
		// 1) Load DOCX into XWPFDocument
		InputStream in= new FileInputStream(f_docx);
		XWPFDocument document = new XWPFDocument(in);

		// 2) Prepare Pdf options
		PdfOptions options = PdfOptions.create();

		// 3) Convert XWPFDocument to Pdf
		OutputStream out = new FileOutputStream(f_pdf);
		PdfConverter.getInstance().convert(document, out, options);		
	}
}
