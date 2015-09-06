package niuteam.book.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;

import niuteam.util.IOUtil;

public class TextResource extends Resource{
	private String encoding = CONST.ENCODING;	
	
	private File f;
	public TextResource(String name){
		super(name);
	}
	public void loadFile(File file, String encoding, String id) throws Exception {
		mediaType = CONST.MIME.HTM;
		String name = file.getName();
		title = name.substring(0, name.length()-4);
		
		String f_name = getId() +".htm" ;
		this.f = new File(IOUtil.getTempFolder()+File.separator+"f", f_name);
		href = "Text/" + f_name;
		
		// 
		StringWriter out = new StringWriter();
		IOUtil.copy(new InputStreamReader(new FileInputStream(file), encoding), out );
		String content = txt2htm(out.toString(), title);
		
		
		InputStream ins = IOUtil.loadTemplate(CONST.TMPL_HTM);
		StringWriter out2 = new StringWriter();
		IOUtil.copy(new InputStreamReader(ins, "utf-8"), out2 );


		StringBuffer buf = new StringBuffer(out2.toString());

		XhtmlDoc.str_rpl(buf, "<h2>", "</h2>", "<h2>"+title + "</h2>");
		if (content.startsWith("<p>")){
			XhtmlDoc.str_rpl(buf, "<p>", "</p>", content);
		}else {
			XhtmlDoc.str_rpl(buf, "<p>", "</p>", "<p>"+content + "</p>");
		}

		Writer fwu = new OutputStreamWriter(new FileOutputStream(this.f), "utf-8");
		fwu.write(buf.toString());
		fwu.flush();
		fwu.close();
		
//		this.id = id;
		
	}
	
	public InputStream getInputStream() throws Exception {
		CONST.log.info("get file ins!  item: " + this.getHref() );
		InputStream ins = null;
		try {
			if (f != null)
				ins = new FileInputStream(f);
		} catch(Exception e){
			CONST.log.error("ERROR " + this.getHref(), e);
		}
		return ins;
	}
	public OutputStream getOutputStream() throws Exception {
		FileOutputStream fos = new FileOutputStream(f);
		return fos;
	}
	
	public long getSize(){
		if (!CONST.MIME.HTM.equals(getMediaType()) ){
			return 0;
		}
		if (f != null)
			return f.length();
		else {
			return 0;
		}
	}
	

	public String txt2htm(String s, String tl) {
		if (s == null) return null;
		int l_t = tl==null? 0: tl.length();
		StringBuilder sb = new StringBuilder(4096);
		StringBuilder rest = new StringBuilder(4096);
		BufferedReader in = new BufferedReader(new StringReader(s) );
		String line;
		sb.append("<p>");
		rest.append("<p>");
		try {
			int cur_len = 0;
			while ((line = in.readLine()) != null) {
				String line_t = line.trim();
				
//				line_t = URLEncoder.encode(line_t, "utf-8"); //encoding
				int l = line_t.length();
				if (l == 0) continue;
				if (line_t.startsWith("----") && line_t.endsWith("----")){
					rest.append("<div>");
					rest.append(line_t);
					StringBuilder line_bf = new StringBuilder();
					// EDIT 3: skip 3, change to 1 at 20121221
					for (int i = 0; i < 3; i++){
						// skip next 3 line for book
						line = in.readLine();
						if (line ==null){
//							rest.append("</div>");
							break;
						}
						line_bf.append(line);
						if (line.length() <2){
						}else {
//							StringBuilder line_bf = new StringBuilder(line);
						}
					}
					boolean s_chk = checkNumber(line_bf, true);
					if (s_chk ){
						boolean istxt = line_bf.indexOf("，") >0;
						if (istxt){
							sb.append(line_bf);
							sb.append("</p>").append("<p>");
						} else {
//							CONST.log.info(""+ line_bf );
						rest.append(line_bf);
						}
					}else{
//						rest.append(line_bf);
						sb.append(line_bf);
						sb.append("</p>").append("<p>");
					}
					rest.append("</div>");
					continue;
				}
				if (l_t>0 && line_t.contains(tl) && (l-l_t>=0 && l-l_t <6)){
					rest.append("<div>");
					rest.append(line_t);
					// skip next page
//					line = in.readLine();
//					if (line ==null) break;
//					rest.append(line);
					// 
					rest.append("</div>");
					continue;
				}
				cur_len += l;
				String end = "。";
				// ADD_20121220_B remove space in line_t
//				line_t = line_t.replaceAll(" ", "");
				line_t = line_t.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
				
				// ADD_20121220_E
				StringBuilder line_bf = new StringBuilder(line_t);
				checkNumber(line_bf, false);
				sb.append(line_bf);
//				sb.append(" ");

//				sb.append(line_t);
				if (line_t.endsWith(end)||line_t.endsWith("”") ||line_t.endsWith("！") || cur_len >1024) {
					sb.append("</p>").append("<p>");
					cur_len = 0;
				}
			}
			sb.append("</p>");
			in.close();
			rest.append("</p>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sb.append(rest);
//		System.out.println(urlStr);
		return sb.toString().trim();
	}
	/**
	 * @param line
	 * @param check
	 * @return if no cn num, return false. no change of line.
	 *  if found, return true, return updated string line.
	 */
	private boolean checkNumber(StringBuilder line, boolean check){
//		String s = "０１２３４５６７８９";
//		String s2 = "0123456789";
		//ｉｄｅｍｓｅｍｐｅｒｆａｅｉｔｉｄｅｍ
		int len = line.length();
		boolean found = false;
//		for (int i=0; i < 10; i++){
//			String c= ""+s.charAt(i);
//			int pos = line.indexOf(c);
//			if ( pos != -1){
//				found = true;
//				if (check)	return true;
//			}
//			while (pos !=-1 && pos <len){
//				line.setCharAt(pos, s2.charAt(i));
//				pos++;
//				pos = line.indexOf(c, pos);
//			}
//		}
//		if (check) return found;
		char c_a = 'ａ';
		char c_A = 'Ａ';
		char c_0 = '０';
		for (int i = 0; i < len; i++){
			char c = line.charAt(i);
			int pos_a = c-c_a;
			if (pos_a >=0 && pos_a <26 ){
				line.setCharAt(i, (char)('a'+pos_a));
				continue;
			}
			int pos_A = c-c_A;
			if (pos_A >=0 && pos_A <26 ){
				line.setCharAt(i, (char)('A'+pos_A));
				continue;
			}
			int pos_0 = c-c_0;
			if (pos_0 >=0 && pos_0 <10 ){
				line.setCharAt(i, (char)('0'+pos_0));
				found = true;
				continue;
			}
			if (c >= '0' && c <= '9'){
				found = true;
			}
		}
		return found;
	}
}
