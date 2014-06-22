package basic;

import network.AuthContainer;
import network.CybeConnector;
import network.CybeParser;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.BasicCookieStore;
import org.junit.After;
import org.junit.Before;
import props.GlobalConfig;
import props.PlatformLinks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;
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
        testCybeConnection();
        testMoodleUnil();
        //init();
        //        testCybeConnection();
        //testPull( analyse3 );
        //        CybeConnector connector = new CybeConnector( PlateformLinks.getInstance( "cyberlearn.hes-so" ) );
        //        testCybeDownload(  connector  );
        //testCookie();
        //connector.restoreCookies( cookiePath );

    }//end main


    @Before
    public static void init() throws Exception{
        connector = new CybeConnector( PlatformLinks.getInstance( "cyberlearn.hes-so" ) );
        connector.connect( GlobalConfig.getInstance() );
    }//end init


    @After
    public static void cleanUp(){
        try{
            if( connector != null && !connector.isConnected() ) connector.close();
        }catch( IOException e ){
            e.printStackTrace();
        }
    }//end cleanUp


    public static void testCookie(){
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


    public static void testPull( String courseUrl ) throws Exception{
        CybeParser cybeParser = new CybeParser( connector );
        List<Future<NameValuePair>> listOfResources = cybeParser.findCourseResources( courseUrl, //
                ( t, n, i ) -> System.out.println( "## Consumer " + n ), System.err::println );

        System.out.println( cybeParser.futuresToMap( listOfResources, 10000 ) );

        System.out.println( "done" );

        //connector.disconnect();
    }//end testPull


    public static void testCybeConnection() throws Exception{
        CybeConnector connector = new CybeConnector( PlatformLinks.getInstance( "cyberlearn.hes-so" ) );
        connector.connect( GlobalConfig.getInstance() );
        connector.close();
    }//end testCybeConnection


    public static void testMoodleUnil() throws Exception{
        CybeConnector connector = new CybeConnector( PlatformLinks.getInstance( "moodle.unil" ) );
        connector.connect( new AuthContainer.BasicAuthContainer( "acharmel", "09MoSar10" ) );
        connector.close();
    }//end testMoodleUnil

}//end class
