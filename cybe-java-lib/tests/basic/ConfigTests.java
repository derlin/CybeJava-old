package basic;

import gson.GsonUtils;
import org.junit.Assert;
import org.junit.Test;
import props.LocalConfig;

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

    private String cybeFile = ConnectionTest.class.getResource( ".cybe" ).getPath();


    @Test
    public void localConfTest(){
        LocalConfig conf = ( LocalConfig ) GsonUtils.getJsonFromFile( cybeFile, new LocalConfig() );
        Assert.assertEquals( conf.getCourse(), "14_HES-SO_FR_Mathématiques spécifiques 2" );
    }//end localConfTest


    @Test
    public void testFileUniqueId() throws IOException{
        Path p = Paths.get( cybeFile );
        BasicFileAttributes attrs = Files.readAttributes( p, BasicFileAttributes.class );
        System.out.println( p );
        System.out.println( attrs.fileKey() );
    }//end testFileUniqueId
}//end class
