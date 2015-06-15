package niuteam.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Most of the functions herein are re-implementations of the ones in apache io IOUtils.
 * The reason for re-implementing this is that the functions are fairly simple and using my own implementation saves the inclusion of a 200Kb jar file.
 */
public class IOUtil {

	public static final int IO_COPY_BUFFER_SIZE = 1024 * 4;

	/**
	 * Gets the contents of the Reader as a byte[], with the given character encoding.
	 * 
	 * @param in
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray(Reader in, String encoding) throws IOException {
		StringWriter out = new StringWriter();
		copy(in, out);
		out.flush();
		return out.toString().getBytes(encoding);
	}

	/**
	 * Returns the contents of the InputStream as a byte[]
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		copy(in, result);
		result.flush();
		return result.toByteArray();
	}

	/**
	 * if totalNrRead < 0 then totalNrRead is returned, if (nrRead + totalNrRead) < Integer.MAX_VALUE then nrRead + totalNrRead is returned, -1 otherwise.
	 * @param nrRead
	 * @param totalNrNread
	 * @return
	 */
	protected static int calcNewNrReadSize(int nrRead, int totalNrNread) {
		if (totalNrNread < 0) {
			return totalNrNread;
		}
		if (totalNrNread > (Integer.MAX_VALUE - nrRead)) {
			return -1;
		} else {
			return (totalNrNread + nrRead);
		}
	}

	/**
	 * Copies the contents of the InputStream to the OutputStream.
	 * 
	 * @param in
	 * @param out
	 * @return the nr of bytes read, or -1 if the amount > Integer.MAX_VALUE
	 * @throws IOException
	 */
	public static int copy(InputStream in, OutputStream out)
			throws IOException {
		if (in == null) return 0;
		byte[] buffer = new byte[IO_COPY_BUFFER_SIZE];
		int readSize = -1;
		int result = 0;
		while ((readSize = in.read(buffer)) >= 0) {
			out.write(buffer, 0, readSize);
			result = calcNewNrReadSize(readSize, result); 
		}
		out.flush();
		return result;
	}

	/**
	 * Copies the contents of the Reader to the Writer.
	 * 
	 * @param in
	 * @param out
	 * @return the nr of characters read, or -1 if the amount > Integer.MAX_VALUE
	 * @throws IOException
	 */
	public static int copy(Reader in, Writer out) throws IOException {
		char[] buffer = new char[IO_COPY_BUFFER_SIZE];
		int readSize = -1;
		int result = 0;
		while ((readSize = in.read(buffer)) >= 0) {
			out.write(buffer, 0, readSize);
			result = calcNewNrReadSize(readSize, result); 
		}
		out.flush();
		return result;
	}
	public static String toString(Object ... keyValues) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (int i = 0; i < keyValues.length; i += 2) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(keyValues[i]);
			result.append(": ");
			Object value = null;
			if ((i + 1) < keyValues.length) {
				value = keyValues[i + 1];
			}
			if (value == null) {
				result.append("<null>");
			} else {
				result.append('\'');
				result.append(value);
				result.append('\'');
			}
		}
		result.append(']');
		return result.toString();
	}
	public static InputStream loadTemplate(String res){
		InputStream in = null;
		in = IOUtil.class.getResourceAsStream("/tmpl/"+res);
		return in;
	}
	public static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
	public static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }
    public static String getTempFolder() {
    	String dir = System.getProperty("java.io.tmpdir");
    	// os.name
    	// user.home
    	return dir;
    }
}
