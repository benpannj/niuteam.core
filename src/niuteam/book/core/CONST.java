package niuteam.book.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface CONST {
	public static final Logger log = LoggerFactory.getLogger(CONST.class);
	String ENCODING = "UTF-8";
	String DOCTYPE_XHTML = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">";
	String NAMESPACE_XHTML = "http://www.w3.org/1999/xhtml";
	String EPUBLIB_GENERATOR_NAME = "niut v201110";
	/**
	 * Dublin Core namespace
	 */
	public static final String dcns = "http://purl.org/dc/elements/1.1/";
	public static final String BOOK_ID_ID = "BookId";
	public static final String NAMESPACE_OPF = "http://www.idpf.org/2007/opf";
	public static final String NAMESPACE_DUBLIN_CORE = "http://purl.org/dc/elements/1.1/";
	public static final String PREFIX_DUBLIN_CORE = "dc";
	public static final String PREFIX_OPF = null;
//	public static final String PREFIX_OPF = "opf";
	public static final String dateFormat = "yyyy-MM-dd";

	/**
	 * OCF namespace
	 */
	public static final String ocfns = "urn:oasis:names:tc:opendocument:xmlns:container";
	
	// char FRAGMENT_SEPARATOR_CHAR = '#';
	String DEFAULT_TOC_ID = "toc";
	String MIME_EPUB="application/epub+zip";
	String MIME_NCX = "application/x-dtbncx+xml";
//	String MIME_OPF
	String MIME_HTM = "application/xhtml+xml";
	String MIME_PNG = "image/png";
	String MIME_CSS = "text/css";

	// fixed file name
	String FILE_ROOT="mimetype";
	String FILE_INFO = "META-INF/container.xml";
	// default file name
	String FILE_OPF = "OEBPS/content.opf";
	String FILE_NCX = "OEBPS/toc.ncx";
	String FILE_CSS = "OEBPS/main.css";
//	public static final MediaType XHTML = new MediaType("application/xhtml+xml", ".xhtml", new String[] {".htm", ".html", ".xhtml"});
//	public static final MediaType EPUB = new MediaType("application/epub+zip", ".epub");
//	public static final MediaType JPG = new MediaType("image/jpeg", ".jpg", new String[] {".jpg", ".jpeg"});
//	public static final MediaType PNG = new MediaType("image/png", ".png");
//	public static final MediaType GIF = new MediaType("image/gif", ".gif");
//	public static final MediaType CSS = new MediaType("text/css", ".css");
//	public static final MediaType SVG = new MediaType("image/svg+xml", ".svg");
//	public static final MediaType TTF = new MediaType("application/x-truetype-font", ".ttf");
//	public static final MediaType NCX = new MediaType("application/x-dtbncx+xml", ".ncx");
//	public static final MediaType XPGT = new MediaType("application/adobe-page-template+xml", ".xpgt");
//	public static final MediaType OPENTYPE = new MediaType("font/opentype", ".otf");

}
