package niuteam.book.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;

import niuteam.util.IOUtil;

public class StringResource extends Resource {
	private byte[] data;

	public StringResource(String name){
		super(name);
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
		return new ByteArrayInputStream(data);
	}

	/**
	 * Sets the data of the Resource.
	 * If the data is a of a different type then the original data then make sure to change the MediaType.
	 * 
	 * @param data
	 */
	public void loadString(String s) {
		this.data = s.getBytes();
		mediaType = CONST.MIME.HTM;
		href = "Text/" + this.getId();
	}


}
