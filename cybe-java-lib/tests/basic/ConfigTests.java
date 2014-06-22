package basic;

import gson.GsonUtils;
import props.LocalConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author: Lucy Linder
 * @date: 20.06.2014
 */
public class ConfigTests{

    private static String cybeFile = ConnectionTest.class.getResource( ".cybe" ).getPath();
    public static void main( String[] args ) throws IOException{
        localConfTest();
        //testFileUniqueId();
    }//end main

    public static void localConfTest(){
        LocalConfig conf = ( LocalConfig ) GsonUtils.getJsonFromFile( cybeFile, new LocalConfig() );
        System.out.println(conf);

    }//end localConfTest


    public static void testFileUniqueId() throws IOException{
        File file = new File( cybeFile );
        Path p = Paths.get( cybeFile);
        BasicFileAttributes attrs = Files.readAttributes( p , BasicFileAttributes.class );
        System.out.println(p);
        System.out.println(attrs.fileKey());
    }//end testFileUniqueId
}//end class
