package basic;

import network.AuthContainer;
import network.CybeConnector;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import props.GlobalConfig;
import props.PlateformLinks;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class ConnectionTest{

    static String txtResource = "http://cyberlearn.hes-so.ch/mod/resource/view.php?id=370890";
    static String pdfResource = "http://cyberlearn.hes-so.ch/mod/resource/view.php?id=379714";
    static String cookiePath = "cookies.ser";


    public static void main( String[] args ) throws Exception{
        testCybeConnection();
        //connector.restoreCookies( cookiePath );

    }//end main


    public static void testCybeDownload(CybeConnector connector) throws Exception{

        connector.setResourcesConsumer( ( type, name, in ) -> {
            if( type.contains( "text/html" ) ){
                Elements links = Jsoup.parse( IOUtils.toString( in ) ) //
                        .select( "div" + ".resourceworkaround a" );

                if( links.size() == 1 ){
                    connector.getResource( links.get( 0 ).attr( "href" ) );
                }else{
                    System.out.println( "hum, trouble " + links.size() );
                }

            }else{
                try( FileOutputStream out = new FileOutputStream( new File( name ) ) ){
                    IOUtils.copy( in, out );

                }catch( Exception e ){
                    e.printStackTrace();
                }
            }
        } );
        connector.getResource( txtResource );
        connector.getResource( pdfResource );

    }//end testCybeDownload

    public static void testCybeConnection() throws Exception{
        CybeConnector connector = new CybeConnector( PlateformLinks.getInstance( "cyberlearn.hes-so" ) );
        connector.connect( GlobalConfig.getInstance() );
        connector.disconnect();
    }//end testCybeConnection

    public static void testMoodleUnil() throws Exception{
        CybeConnector connector = new CybeConnector( PlateformLinks.getInstance( "moodle.unil" ) );
        connector.connect( new AuthContainer.BasicAuthContainer( "acharmel", "09MoSar10" ) );
        connector.disconnect();
    }//end testMoodleUnil

}//end class
