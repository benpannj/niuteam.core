利用百度AI OCR图片识别，Java实现PDF中的图片转换成文字

百度AI平台 获取AppID, API Key, Secret Key
Java SDK文档使用说明: https://ai.baidu.com/docs#/OCR-Java-SDK/top

# see BaiduAipNlp.java https://console.bce.baidu.com/ai/?_=1564485743845#/ai/nlp/app/detail~appId=1143137
baidunlp.appid=16919565
baidunlp.apikey=2c0uK4WElGTuFbIeDxyt4Ntk
baidunlp.seckey=zyclnXSmSU0nVxkewrE3cfb3oAcMp7aq

```xml
        <dependency><!--百度AI SDK-->
            <groupId>com.baidu.aip</groupId>
            <artifactId>java-sdk</artifactId>
            <version>4.8.0</version>
        </dependency>
        <dependency><!--PDF操作工具包-->
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox-app</artifactId>
            <version>2.0.16</version>
        </dependency>
```


java

```java
package com.example.demo;

import com.baidu.aip.ocr.AipOcr;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoApplication {
    //设置APPID/AK/SK
    public static final String APP_ID = "你的APP_ID";
    public static final String API_KEY = "你的API_KEY";
    public static final String SECRET_KEY = "你的SECRET_KEY ";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 解析pdf文档信息
     *
     * @param pdfPath pdf文档路径
     * @throws Exception
     */
    public static void pdfParse(String pdfPath) throws Exception {
        InputStream input = null;
        File pdfFile = new File(pdfPath);
        PDDocument document = null;
        try {
            input = new FileInputStream(pdfFile);
            //加载 pdf 文档
            document = PDDocument.load(input);

            /** 文档属性信息 **/
            PDDocumentInformation info = document.getDocumentInformation();
            System.out.println("标题:" + info.getTitle());
            System.out.println("主题:" + info.getSubject());
            System.out.println("作者:" + info.getAuthor());
            System.out.println("关键字:" + info.getKeywords());

            System.out.println("应用程序:" + info.getCreator());
            System.out.println("pdf 制作程序:" + info.getProducer());

            System.out.println("作者:" + info.getTrapped());

            System.out.println("创建时间:" + dateFormat(info.getCreationDate()));
            System.out.println("修改时间:" + dateFormat(info.getModificationDate()));


            //获取内容信息
            PDFTextStripper pts = new PDFTextStripper();
            String content = pts.getText(document);
            System.out.println("内容:" + content);


            /** 文档页面信息 **/
            PDDocumentCatalog cata = document.getDocumentCatalog();
            PDPageTree pages = cata.getPages();
            System.out.println(pages.getCount());
            int count = 1;

            // 初始化一个AipOcr
            AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);

            // 可选：设置网络连接参数
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);

            for (int i = 0; i < pages.getCount(); i++) {
                PDPage page = (PDPage) pages.get(i);
                if (null != page) {
                    PDResources res = page.getResources();
                    Iterable xobjects = res.getXObjectNames();
                    if(xobjects != null){
                        Iterator imageIter = xobjects.iterator();
                        while(imageIter.hasNext()){
                            COSName key = (COSName) imageIter.next();
                            if (res.isImageXObject(key)) {
                                try {
                                    PDImageXObject image = (PDImageXObject) res.getXObject(key);
                                    BufferedImage bimage = image.getImage();
                                     // 将BufferImage转换成字节数组
                                    ByteArrayOutputStream out =new ByteArrayOutputStream();
                                    ImageIO.write(bimage,"png",out);//png 为要保存的图片格式
                                    byte[] barray = out.toByteArray();
                                    out.close();
                                     // 发送图片识别请求 
                                    JSONObject json = client.basicGeneral(barray, new HashMap<String, String>());
                                    System.out.println(json.toString(2));
                                    count++;
                                    System.out.println(count);
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != input)
                input.close();
            if (null != document)
                document.close();
        }
    }

    /**
     * 获取格式化后的时间信息
     *
     * @param dar 时间信息
     * @return
     * @throws Exception
     */
    public static String dateFormat(Calendar calendar) throws Exception {
        if (null == calendar)
            return null;
        String date = null;
        try {
            String pattern = DATE_FORMAT;
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            date = format.format(calendar.getTime());
        } catch (Exception e) {
            throw e;
        }
        return date == null ? "" : date;
    }

    public static void main(String[] args) throws Exception {

        // 读取pdf文件
        String path = "C:\\Users\\fl\\Desktop\\a.pdf";
        pdfParse(path);

    }

}

```