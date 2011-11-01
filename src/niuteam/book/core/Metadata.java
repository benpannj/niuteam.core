package niuteam.book.core;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import niuteam.util.DomUtil;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlpull.v1.XmlSerializer;

public class Metadata {
	public String bkid_val;
	public String bkid_name;
//	public String title;
//	public String auth;
//	public String lang;
	private Map<String, String> datas = new HashMap<String, String>();
	
	public interface DCTags {
		String title = "title";
        String creator = "creator";
        String subject = "subject";
        String description = "description";
        String publisher = "publisher";
        String contributor = "contributor";
        String date = "date";
        String type = "type";
        String format = "format";
        String identifier = "identifier";
        String source = "source";
        String language = "language";
        String relation = "relation";
        String coverage = "coverage";
        String rights = "rights";
	}
	public void set(String key, String val){
		datas.put(key, val);
	}
	public void readOpf(Element opf){
		Element metadataElement = DomUtil.getFirstElementByTagNameNS(opf, CONST.NAMESPACE_OPF, "metadata");
		if(metadataElement == null) {
			CONST.log.error("Package does not contain element metadata ");
			return;
		}
		Node nd = metadataElement.getFirstChild();
		while (nd != null){
			if (nd instanceof Element){
				Element elm = (Element)nd;
				String key = elm.getLocalName();
				String value = DomUtil.getTextContent(elm);
				if (DCTags.identifier.equals(key)){
					// key attr
					bkid_name = elm.getAttributeNS("", "id"); // CONST.NAMESPACE_OPF
					bkid_val = value;
//					CONST.log.info("meta:  id  - {}", attr );
				} else {
					CONST.log.info("meta:  {}  - {}", key, value );
					datas.put(key, value);
					if (elm.hasAttributes()){
						CONST.log.info("has attrs   - {}", key );
					}
				}
			}
			nd = nd.getNextSibling();
		}
	}
	public void writeOpf() throws Exception{
		StringWriter out = new StringWriter(); 
		XmlSerializer serializer = DomUtil.createXmlSerializer(out);
		serializer.startDocument(CONST.ENCODING, false);
		serializer.setPrefix(CONST.PREFIX_OPF, CONST.NAMESPACE_OPF);
		serializer.setPrefix(CONST.PREFIX_DUBLIN_CORE, CONST.NAMESPACE_DUBLIN_CORE);
		serializer.startTag(CONST.NAMESPACE_OPF, "package");
		serializer.attribute("", "version", "2.0");
		serializer.attribute("", "uniqueIdentifier", bkid_name);
		
		serializer.startTag(CONST.NAMESPACE_OPF, "metadata");
		serializer.setPrefix(CONST.PREFIX_DUBLIN_CORE, CONST.NAMESPACE_DUBLIN_CORE);
		serializer.setPrefix(CONST.PREFIX_OPF, CONST.NAMESPACE_OPF);

		serializer.startTag(CONST.NAMESPACE_DUBLIN_CORE, DCTags.identifier);
		serializer.attribute("", "id", bkid_name);
//		serializer.attribute(CONST.NAMESPACE_OPF, "scheme", "UUID");
		serializer.text(bkid_val);
		serializer.endTag(CONST.NAMESPACE_DUBLIN_CORE, DCTags.identifier);
		
		
		for ( Iterator<Entry<String, String>> itor = datas.entrySet().iterator(); itor.hasNext(); ) {
			Entry<String, String> item = itor.next();
			String tagName = item.getKey();
			String value = item.getValue();
			serializer.startTag(CONST.NAMESPACE_DUBLIN_CORE, tagName);
			serializer.text(value);
			serializer.endTag(CONST.NAMESPACE_DUBLIN_CORE, tagName);
		}
//		writeIdentifiers(book.getMetadata().getIdentifiers(), serializer);
		serializer.endTag(CONST.NAMESPACE_OPF, "metadata");
		
		serializer.endTag(CONST.NAMESPACE_OPF, "package");
		serializer.endDocument();
		serializer.flush();

		out.flush();
		CONST.log.info(" write  {}", out.toString()  );
	}
}
