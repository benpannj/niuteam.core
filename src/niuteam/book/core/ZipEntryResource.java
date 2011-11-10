package niuteam.book.core;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipEntryResource extends Resource{
	private ZipFile zf;
	private String base_path = "";
	public ZipEntryResource(String name){
		super(name);
	}
	public void loadEntry(ZipFile file, String href, String type, String path){
		this.zf = file;
		this.href = href;
		this.mediaType = type;
		this.base_path = path;
	}
	public InputStream getInputStream() throws Exception {
		InputStream ins = null;
		try {
			if (zf != null) {
				ZipEntry ze = new ZipEntry(base_path + href);
				ins = zf.getInputStream(ze);
			}
		} catch(Exception e){
			CONST.log.error("ERROR: "+ href, e);
		}
		if (ins == null){
			CONST.log.error("ERROR null: "+ href);
		}
		return ins;
	}
}
