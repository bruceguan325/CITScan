package com.intumit.smartwiki.util;

 public class UnZipUtil {  
     private static String unrarCmd = "C:\\Program Files (x86)\\WinRAR\\UnRar x ";     
     public static String rarFileName;
     public static String targetDirectory;
     
     public UnZipUtil (String rarFileName, String targetDirectory ) {
	 this.rarFileName = rarFileName;
	 this.targetDirectory = targetDirectory;
     }
     
   
     public static void unRARFile() {  
        unrarCmd += rarFileName + " " + targetDirectory;  
        try {  
            Runtime rt = Runtime.getRuntime();  
            Process p = rt.exec(unrarCmd);   
        } catch (Exception e) {  
            System.out.println(e.getMessage());     
        }  
     }  
 }