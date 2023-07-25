package test.epub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import junit.framework.TestCase;
import niuteam.book.core.CONST;
import niuteam.util.PdfHelper;
import test.JUnitEnv;

/**
 * Copyright (c) 2018 China Systems Corp.
 *
 * @author ben.pan (ben.pan@chinasystems.com)
 * @date 2018-06-01.
 */
public class PdfTest extends TestCase{
    static {
        JUnitEnv.init();
    }
    public void testPdf() throws Exception {
        CONST.log.debug(" PDF ");
//        manipulatePdf("/tmp/a.pdf", "/tmp/b.pdf");
//        CONST.log.debug("-------------");
//        manipulatePdf("/tmp/b/nj.pdf", "/tmp/b/nj_o.pdf");
        File fd = new File("/tmp/b");
        for (File f : fd.listFiles() ) {
            String src = f.getAbsolutePath();
            String dest = "/tmp/d/"+f.getName();
            CONST.log.debug(" from " + src + "   ->  " + dest);
            manipulatePdf(src, dest);
        }
    }

    public void manipulatePdf(String src, String dest) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src);
        int size = reader.getNumberOfPages();
        CONST.log.debug("Total pages: " + size);
        PdfDictionary page = reader.getPageN(1);
        PdfDictionary resources = page.getAsDict(PdfName.RESOURCES);

        StringBuilder buf = new StringBuilder();
        PdfDictionary xobjects = resources.getAsDict(PdfName.XOBJECT);
        for ( Iterator<PdfName> it = xobjects.getKeys().iterator(); it.hasNext();) {
            PdfName imgRef = it.next();
            PRStream stream = (PRStream) xobjects.getAsStream(imgRef);
            int len = stream.getLength();
            String s = ""+stream.get(PdfName.INTERPOLATE)+imgRef.toString();
//                    +"/"+stream.get(PdfName.SUBTYPE);
//            buf.setLength(0);
//            for (PdfName key: stream.getKeys()) {
//                buf.append(" ").append(key).append(":").append( stream.get(key) );
//            }
            if ("false/Image5".equalsIgnoreCase(s)
                    || "false/Image8".equalsIgnoreCase(s)
                    || "null/IM8".equalsIgnoreCase(s)
                    || "null/Im14".equalsIgnoreCase(s)
                    ){
                // this is background image
//            if (len == 15232 || len == 175983) {
                CONST.log.debug( "DELETE " + imgRef.toString() + " [" + stream.type() +"] " + len + " | " + s
//                            + buf.toString()
//                    + ",  " + stream.get(PdfName.SUBTYPE)
//                    + ",  " + stream.get(PdfName.FILTER)
//                    + ",  " + stream.get(PdfName.BITSPERCOMPONENT)
//                    + ",  " + stream.get(PdfName.COLORSPACE)
//                    + ",  " + stream.get(PdfName.WIDTH)
//                    + ",  " + stream.get(PdfName.HEIGHT)
                );
                // save to file
                byte[] b;
                try {
                    b = PdfReader.getStreamBytes(stream);
                }
                catch(Exception e) {
                    b = PdfReader.getStreamBytesRaw(stream);
                }
                FileOutputStream f = new FileOutputStream(new File("/tmp"+imgRef));
                f.write(b);
                f.flush();
                f.close();
                // remove from pdf
                stream.clear();
            }
        }

//        PdfObject object = dict.getDirectObject(PdfName.CONTENTS);
//        if (object instanceof PRStream) {
//            PRStream stream = (PRStream)object;
//            byte[] data = PdfReader.getStreamBytes(stream);
//            stream.setData(new String(data).replace("Hello World", "HELLO WORLD").getBytes());
//            CONST.log.debug( new String(data) );
//        } else {
//            CONST.log.debug(" TBD "+  object.getClass().getName() );
//        }
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
        stamper.close();
        reader.close();
    }

}
