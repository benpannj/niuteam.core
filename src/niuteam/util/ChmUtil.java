package niuteam.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import niuteam.book.core.CONST;

public class ChmUtil {
	private String getTitleFromHhc(String folder) throws Exception{
		File f = new File("/tmp", "0000.hhc");
		String encoding = "gb2312";
//		List list = new ArrayList();
//		new InputStreamReader(new FileInputStream(f), encoding)
		BufferedReader reader = new BufferedReader( new InputStreamReader(new FileInputStream(f), encoding) );//  FileReader(f);
		String line = null;
		String name=null, value=null;
		String title = null, type= null, path= null;
		String last_title = null;
		while( (line = reader.readLine() ) != null){
			if (line.startsWith("<param ")){
				int pos = line.indexOf("name=\"");
				if (pos > 1) {
					pos+=6;
					int end = line.indexOf("\"", pos);
					name = line.substring(pos, end);
				}
				pos = line.indexOf("value=\"");
				if (pos > 1) {
					pos+=7;
					value = line.substring(pos, line.length()-2);
				}
				if (name.equals("Name")){
					title = value;
				} else if (name.equals("ImageNumber")){
					type = value;
					if ("1".equals(type)){
						// folder name
						last_title = title;
					} else if ("11".equals(type)){
						// get path
						if (last_title != null) {
							int i= path.indexOf('/');
							if (i > 0)
								path = path.substring(0, i);
							CONST.log.info(" " + last_title + ", " + path);
							File ef = new File("/home/ben/doc/epub");
							File ep = new File(ef, path+".epub");
							if (ep.exists()) {
								ep.renameTo(new File(ef, last_title+".epub" ));
							} else {
								CONST.log.info("BAD! ---  " + last_title + ", " + path);
								
							}
							last_title = null;
						}
					}
				} else if (name.equals("Local")){
					path = value;
				}
			}
		}
		return "";
	}
}
