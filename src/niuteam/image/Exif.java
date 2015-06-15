package niuteam.image;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import niuteam.book.core.CONST;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class Exif {
	private String base_folder;
	public Exif(String f){
		base_folder = f;
	}
	public void dump(File f) throws Exception{
		Metadata metadata = JpegMetadataReader.readMetadata(f);
		Iterator<Directory> directories = metadata.getDirectories().iterator();
		while (directories.hasNext()) {
			Directory directory = (Directory)directories.next();
			// iterate through tags and print to System.out
			Iterator<Tag> tags = directory.getTags().iterator();
			while (tags.hasNext()) {
				Tag tag = (Tag)tags.next();
				// use Tag.toString()
				System.out.println(tag);
			}
		}
		
	}
	public int organize(File f) throws Exception{
		if (base_folder == null) return -1;
		if (f.isDirectory()){
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++){
				int ret = organize(files[i]);
				if (ret < -2){
					CONST.log.info( "bad " +  files[i].getAbsolutePath());
//					dump(files[i]);
				}
			}
			if ( f.listFiles().length == 0){
				CONST.log.info( "empty " +  f.getAbsolutePath());
				f.delete();
			}
		} else {
			String name = f.getName();
			if (name.startsWith("MVI")){ // || name.endsWith("dup")
				CONST.log.info("debug name: " + name);
			}
			int pos = name.lastIndexOf('.');
			if (pos == -1 && name.startsWith("MVI")){
				name = name+".avi";
				pos = name.lastIndexOf('.');
			}
			if (pos == -1){
				CONST.log.info("name: " + name);
				return -5;
			}
			String ext = name.substring(pos);
			
			Date date = null;
			String productor = null;
			Metadata metadata = null;
			try {
				metadata = JpegMetadataReader.readMetadata(f);
			} catch (Throwable e) {
				CONST.log.error( "bad no meta: " +  f.getAbsolutePath());
				
				return -5;
			}
			if (metadata == null){
				if (".avi".equalsIgnoreCase(ext)) {
					long l = f.lastModified();
					date = new Date(l);
					productor = "Cannon";
				} else if (".rmvb".equalsIgnoreCase(ext)) {
					return -2;
				}else{
					long l = f.lastModified();
					date = new Date(l);
					productor = "Zte";
				}
			}else {
				Directory exif_sub;
				exif_sub = metadata.getDirectory(ExifSubIFDDirectory.class);
				if (exif_sub != null) {
					date = exif_sub.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				}
				Directory exif_ifd0 = metadata.getDirectory(ExifIFD0Directory.class);
				if (exif_ifd0 != null) {
					productor = exif_ifd0.getDescription( ExifIFD0Directory.TAG_MAKE);
					if (date == null){
						date = exif_ifd0.getDate(ExifIFD0Directory.TAG_DATETIME);
					}
				}
			}
			if (date == null){
				if (".jpg".equalsIgnoreCase(ext)) {
					long l = f.lastModified();
					date = new Date(l);
					productor = "Cannon";
				}else{
					return -4;
				}
//				return -4;
			}
			Calendar c = Calendar.getInstance();
			c.set(1990, 1, 1);
			if ( date.before(c.getTime()) ){
				return -4;
			}
			DateFormat fmt = new java.text.SimpleDateFormat("yyyyMMdd.HHmm.");
			String s_date = fmt.format(date);
//			if (s_date )
			File folder = new File(base_folder, s_date.substring(0, 6));
			if (!folder.exists()){
				folder.mkdirs();
			}
			File parent = f.getParentFile();
			// use for "_choice" foler 
			if (parent.getName().equals("_choice")){
				folder = parent;
			}

			pos = 0;
			if (productor !=null && productor.length() > 2) {
				ext = productor.substring(0, 2)+ext;
			}
			String f_name = "I" + s_date+pos+ ext;
			boolean same_folder = folder.equals(parent );
//			"_choice"
			if (name.equals(f_name) && same_folder){
//				CONST.log.info( "exist " +   ", " + f.getAbsolutePath());
				return 0;
			}
			File fn = new File( folder, f_name);
			long size_l = f.length();
			while(fn.exists()){
				long size_r = fn.length();
				if (size_l == size_r){
					if (same_folder){
						if (name.equals(f_name)) {
							return 0;
						} else {
							ext = ext+".dup";
						}
					} else {
						CONST.log.info( " same size, may exist "+fn.getAbsolutePath() +   ", " + f.getAbsolutePath());
						ext = ext+".dup";
					}
				}
				pos++;
				f_name = "I" + s_date+pos+ ext;
				fn = new File( folder, f_name);
			} 
			
			boolean ok = f.renameTo(fn);
			if (!ok){
				CONST.log.info( " bad rename "+fn.getAbsolutePath() +   ", " + f.getAbsolutePath());
			}
			
		}
		return 0;
	}
}
