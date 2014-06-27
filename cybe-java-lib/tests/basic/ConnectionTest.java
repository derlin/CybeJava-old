package basic;

import network.AuthContainer;
import network.CybeConnector;
import network.CybeParser;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.BasicCookieStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import props.GlobalConfig;
import props.PlatformLinks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class ConnectionTest{

    static String txtResource = "http://cyberlearn.hes-so.ch/mod/resource/view.php?id=370890";
    static String pdfResource = "http://cyberlearn.hes-so.ch/mod/resource/view.php?id=379714";
    static String analyse3 = "http://cyberlearn.hes-so.ch/enrol/index.php?id=5391";
    static String algo23 = "http://cyberlearn.hes-so.ch/course/view.php?id=144";
    static String cookiePath = "cookies.ser";

    static CybeConnector connector;

    public static void main( String[] args ) throws Exception{
        ConnectionTest test = new ConnectionTest();
        test.readLinks();
    }//end main

    @Test
    public void readLinks() throws Exception{
        PlatformLinks.getInstance( "cyberlearn.hes-so" );
        System.out.println("ok");
    }//end readLinks


    @Before
    public void init() throws Exception{
        connector = new CybeConnector( PlatformLinks.getInstance( PlatformLinks.CYBERLEARN_HES_SO) );
        connector.connect( GlobalConfig.getInstance() );
    }//end init


    @After
    public void cleanUp(){
        if( connector != null && !connector.isConnected() ) connector.close();
    }//end cleanUp


    @Test
    public void testCookie(){
        try{
            ObjectInputStream inputStream = new ObjectInputStream( new FileInputStream(
                    "/home/lucy/git/cybe-java/cookies.ser" ) );
            BasicCookieStore cookies = ( BasicCookieStore ) inputStream.readObject();
            System.out.println( cookies );
            cookies.clearExpired( new Date() );
        }catch( IOException | ClassNotFoundException e ){
            e.printStackTrace();
        }
    }//end testCookie


    @Test
    public void testPull() throws Exception{
        String courseUrl = analyse3;
        long start = System.currentTimeMillis();
        CybeParser cybeParser = new CybeParser( connector );
        List<Future<NameValuePair>> listOfResources = cybeParser.findCourseResources( courseUrl, //
                ( t, n, i ) -> System.out.println( "## Consumer " + n ), ( url, entity ) -> System.err.println( url +
                ": " + entity.getStatusLine() ) );

        Map<String, String> results = cybeParser.futuresToMap( listOfResources, 10000 );
        Assert.assertTrue( !results.isEmpty() );

        results.forEach( ( name, url ) -> {
            System.out.printf( "%s => %s%n", name, url );
        } );

        System.out.printf( "Execution time: %d%n", System.currentTimeMillis() - start );
        //connector.disconnect();
    }//end testPull


    @Test
    public void testCybeConnection() throws Exception{
        CybeConnector connector = new CybeConnector( PlatformLinks.getInstance( PlatformLinks.CYBERLEARN_HES_SO ) );
        connector.connect( GlobalConfig.getInstance() );
        connector.close();
    }//end testCybeConnection


    @Test
    public void testMoodleUnil() throws Exception{
        CybeConnector connector = new CybeConnector( PlatformLinks.getInstance( PlatformLinks.MOODLE_UNIL) );
        connector.connect( new AuthContainer.BasicAuthContainer( "acharmel", "09MoSar10" ) );
        connector.close();
    }//end testMoodleUnil

}//end class
