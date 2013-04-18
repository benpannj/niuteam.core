package niuteam.book.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

public class FileResource extends Resource{
	private File f;
	public FileResource(String name){
		super(name);
	}
	public void loadFile(File file){
		this.f = file;
		String f_name = f.getName();
		mediaType = determineMediaType(f_name);
		href = "Text/" + f_name;
		
	}
	public OutputStream getOutputStream() throws Exception {
		FileOutputStream fos = new FileOutputStream(f);
		return fos;
	}
	
	public InputStream getInputStream() throws Exception {
		CONST.log.info("get file ins!  item: " + this.getHref() );
		InputStream ins = null;
		try {
			ins = new FileInputStream(f);
		} catch(Exception e){
			CONST.log.error("ERROR " + this.getHref(), e);
		}
		return ins;
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
}
