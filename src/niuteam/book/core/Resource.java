package niuteam.book.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

import niuteam.util.IOUtil;
import niuteam.util.XmlStreamReader;

/**
 * Represents a resource that is part of the epub.
 * A resource can be a html file, image, xml, etc.
 * 
 * @author paul
 *
 */
public class Resource implements Serializable {
	
	private static final long serialVersionUID = 1043946707835004037L;
	private String id;
	private String href;
	private String mediaType;
	private byte[] data;

	private String title;
	

	/**
	 * Creates a resource with the given id, data, mediatype at the specified href.
	 * If the data is of a text type (html/css/etc) then it will use the given inputEncoding.
	 * 
	 * @param id The id of the Resource. Internal use only. Will be auto-generated if it has a null-value.
	 * @param data The Resource's contents
	 * @param href The location of the resource within the epub. Example: "chapter1.html".
	 * @param mediaType The resources MediaType
	 * @param inputEncoding If the data is of a text type (html/css/etc) then it will use the given inputEncoding.
	 */
	public Resource(String id, byte[] data, String href, String mediaType) {
		this.id = id;
		this.href = href;
		this.mediaType = mediaType;
		this.data = data;
	}
	public Resource(InputStream in, String href) throws IOException {
		this(null, IOUtil.toByteArray(in), href, determineMediaType(href));
	}
	public static String determineMediaType(String href){
		return CONST.MIME_HTM;
	}
	/**
	 * Gets the contents of the Resource as Reader.
	 * 
	 * Does all sorts of smart things (courtesy of apache commons io XMLStreamREader) to handle encodings, byte order markers, etc.
	 * 
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	public Reader getReader() throws IOException {
		return new XmlStreamReader(new ByteArrayInputStream(data), CONST.ENCODING);
	}
	
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
	 * The contents of the resource as a byte[]
	 * 
	 * @return The contents of the resource
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Sets the data of the Resource.
	 * If the data is a of a different type then the original data then make sure to change the MediaType.
	 * 
	 * @param data
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * If the title is found by scanning the underlying html document then it is cached here.
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Sets the Resource's id: Make sure it is unique and a valid identifier.
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * The resources Id.
	 * 
	 * Must be both unique within all the resources of this book and a valid identifier.
	 * @return
	 */
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
	 * Sets the Resource's href.
	 * 
	 * @param href
	 */
	public void setHref(String href) {
		this.href = href;
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
	
	/**
	 * This resource's mediaType.
	 * 
	 * @return
	 */
	public String getMediaType() {
		return mediaType;
	}
	
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String toString() {
		return IOUtil.toString("id", id,
				"title", title,
				"mediaType", mediaType,
				"href", href,
				"size", (data == null ? 0 : data.length));
	}
}
