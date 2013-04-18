package niuteam.book.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StringResource extends Resource {
	private byte[] data;
	private ByteArrayOutputStream os = null;

	public StringResource(String href, String title){
		super(href);
		super.title = title;
	}
//	public StringResource(InputStream in, String href) throws IOException {
//		this(null, IOUtil.toByteArray(in), href, determineMediaType(href));
//	}
	/**
	 * Gets the contents of the Resource as an InputStream.
	 * 
	 * @return The contents of the Resource.
	 * 
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		if (os != null && os.size() > 0){
			data = os.toByteArray();
		}
		return new ByteArrayInputStream(data);
	}
	public OutputStream getOutputStream() throws Exception {
		if (os == null) {
			os = new ByteArrayOutputStream();
		} else{
			os.reset();
		}
//		os.write(data);
//		os.toByteArray();
		return os;
	}

	/**
	 * Sets the data of the Resource.
	 * If the data is a of a different type then the original data then make sure to change the MediaType.
	 * 
	 * @param data
	 */
	public void loadString(String s) {
		try {
		this.data = s.getBytes("utf-8");
		}catch(Exception e){}
		mediaType = CONST.MIME.HTM;
		href = "Text/" + this.getId();
	}
	public long getSize(){
		if (!CONST.MIME.HTM.equals(getMediaType()) ){
			return 0;
		}
		if (data != null)
			return data.length;
		else {
			return 0;
		}
	}

}
