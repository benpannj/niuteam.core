package niuteam.book.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import niuteam.util.DomUtil;

import org.w3c.dom.Document;


public class Book {
	public Metadata metadata = new Metadata();
	public Map<String, Resource> resources = new HashMap<String, Resource>();
	public Resource ncx;
	public Resource opf;
	

	public void readOpf() throws Exception {
		Document docOpf = DomUtil.getAsDocument(opf);
		metadata.readOpf(docOpf.getDocumentElement());
		CONST.log.info( DomUtil.node2String(docOpf));
	}
	public void validate(){
		if (metadata.bkid_name == null){
			metadata.bkid_name = "bookid";
		}
		if (metadata.bkid_val == null){
			String format = "yyyyMMdd-HHmmss";
			SimpleDateFormat f = new SimpleDateFormat(format);
			metadata.bkid_val = "ID-"+f.format(new Date());
//			bkid_val = ""+System.nanoTime();
		}
		if (opf == null){
			// 
		}
	}
	
}
