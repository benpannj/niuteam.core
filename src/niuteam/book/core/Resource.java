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

public abstract class Resource {
	private String id;
	protected String href;
	protected String mediaType;

	protected Resource(String id){
		this.id = id;
	}

	public static String determineMediaType(String href){
		CONST.log.info("file: --- {} ", href );
		if (href.endsWith(".png")) {
			return CONST.MIME.PNG;
		} else if (href.endsWith(".jpg")){
			return CONST.MIME.JPG;
		} else if (href.endsWith(".css")){
			return CONST.MIME.CSS;
		}
		return CONST.MIME.HTM;
	}

	/**
	 * Gets the contents of the Resource as an InputStream.
	 * 
	 * @return The contents of the Resource.
	 * 
	 * @throws IOException
	 */
	public abstract InputStream getInputStream() throws Exception ;

	public String getId() {
		return id;
	}

	/**
	 * The location of the resource within the contents folder of the epub file.
	 * 
	 * Example:<br/>
	 * images/cover.jpg<br/>
	 * content/chapter1.xhtml<br/>
	 * 
	 * @return
	 */
	public String getHref() {
		return href;
	}
	/**
	 * This resource's mediaType.
	 * 
	 * @return
	 */
	public String getMediaType() {
		return mediaType;
	}

	
	/**
	 * Gets the hashCode of the Resource's href.
	 * 
	 */
	public int hashCode() {
		return href.hashCode();
	}
	
	/**
	 * Checks to see of the given resourceObject is a resource and whether its href is equal to this one.
	 * 
	 */
	public boolean equals(Object resourceObject) {
		if (! (resourceObject instanceof Resource)) {
			return false;
		}
		return href.equals(((Resource) resourceObject).getHref());
	}
	
	public String toString() {
		return IOUtil.toString("id", id,
				"mediaType", mediaType,
				"href", href);
	}

}
