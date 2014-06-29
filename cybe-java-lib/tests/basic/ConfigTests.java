package basic;

import gson.GsonUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import props.LocalConfig;
import utils.CybeUtils;

import java.io.IOException;

/**
 * @author: Lucy Linder
 * @date: 20.06.2014
 */
public class ConfigTests{

    private String cybeFile;


    @Before
    public void init(){
        cybeFile = ConnectionTest.class.getResource( ".cybe" ).getPath();

    }//end init


    @Test
    public void localConfReadTest(){
        LocalConfig conf = ( LocalConfig ) GsonUtils.getJsonFromFile( cybeFile, new LocalConfig() );
        Assert.assertEquals( conf.getCourse(), "14_HES-SO_FR_Mathématiques spécifiques 2" );
    }//end localConfTest

    @Test
    public void localConfLoadFail(){
        LocalConfig conf = LocalConfig.loadInstance( "prout-lala" );
        Assert.assertNull( conf );
    }


    @Test
    public void testFileUniqueId() throws IOException{
        String id = CybeUtils.getUniqueFileId( cybeFile );

        CybeUtils.OS os = CybeUtils.getOs();
        if( os == CybeUtils.OS.LINUX ){
            Assert.assertEquals( id, "38142131" );
        }
        System.out.println( "id: " + id );

    }//end testFileUniqueId
}//end class
