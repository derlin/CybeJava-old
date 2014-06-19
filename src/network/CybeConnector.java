package network;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import props.PlateformLinks;
import utils.SuperSimpleLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Lucy Linder
 * @date: 18.06.2014
 */
public class CybeConnector{


    private static final String DEFAULT_ENCODING = "UTF-8";
    static SuperSimpleLogger logger = SuperSimpleLogger.defaultInstanceVerbose();

    private static final ResourceConsumer<String, InputStream> DEFAULT_RES_CONSUMER = ( type, name, in ) -> {
        try( FileOutputStream out = new FileOutputStream( new File( name ) ) ){
            IOUtils.copy( in, out );
        }catch( Exception e ){
            e.printStackTrace();
        }
    };

    //----------------------------------------------------

    private CloseableHttpClient httpclient;
    private BasicCookieStore cookieStore;
    private Map<String, String> courses = new HashMap<>();
    private ResourceConsumer<String, InputStream> resourcesConsumer;
    private PlateformLinks plateformLinks;


    /* *****************************************************************
     * Constructors
     * ****************************************************************/

    public CybeConnector( PlateformLinks plateform, ResourceConsumer<String, InputStream> resourcesConsumer ){
        plateformLinks = plateform;
        cookieStore = new BasicCookieStore();
        httpclient = HttpClients.custom().setDefaultCookieStore( cookieStore ).setRedirectStrategy( new
                LaxRedirectStrategy() ).build();
        this.resourcesConsumer = resourcesConsumer;
    }


    public CybeConnector( PlateformLinks plateform ){
        this( plateform, DEFAULT_RES_CONSUMER );
    }

    /* *****************************************************************
     * Connection
     * ****************************************************************/

    public void connect( AuthContainer auth ) throws Exception{
        final List<NameValuePair> pairs = new ArrayList<>();
        pairs.add( new BasicNameValuePair( "user_idp", plateformLinks.organisationFormIdp() ) );
        pairs.add( new BasicNameValuePair( "j_username", auth.username() ) );
        pairs.add( new BasicNameValuePair( "j_password", auth.password() ) );

        ThrowableConsumer<String> doPostFunction = ( url ) -> {

            HttpPost httppost = new HttpPost( url );

            // create and execute the post
            httppost.setEntity( new UrlEncodedFormEntity( pairs ) );
            HttpResponse response = httpclient.execute( httppost );

            logger.debug.printf( "got %s (%s)%n", url, response.getStatusLine() );
            // check the answer is 200 - OK
            if( response.getStatusLine().getStatusCode() != 200 ){
                throw new Exception( String.format( "post to %s failed. Status line = %s%n", url,
                        response.getStatusLine() ) );
            }

            // get the hidden SAML fields
            HttpEntity entity = response.getEntity();
            Document doc = Jsoup.parse( EntityUtils.toString( entity, DEFAULT_ENCODING ) );
            for( Element input : doc.select( "input[type=hidden]" ) ){
                pairs.add( new BasicNameValuePair( input.attr( "name" ), input.attr( "value" ) ) );
            }//end for

            // close the entity streams
            EntityUtils.consume( entity );

        };

        // do the three-way auth process
        logger.info.printf( "Filling organisation form... " );
        doPostFunction.accept( plateformLinks.organisationFormUrl() );
        logger.info.printf( "Ok.%nFilling authentication form... " );
        doPostFunction.accept( plateformLinks.authFormUrl() );
        logger.info.printf( "Ok.%n" );
        doPostFunction.accept( plateformLinks.confirmAuthUrl() );

        logger.verbose.printf( "Building the list of courses... " );

        getResource( plateformLinks.homeUrl(), ( ct, n, i ) -> {
            String welcomePage = IOUtils.toString( i );
            Map<String, String> courses = parseCourses( welcomePage );
            for( Map.Entry<String, String> entry : courses.entrySet() ){
                logger.debug.printf( "%s :%s%n", entry.getKey(), entry.getValue() );
            }//end for
        } );
        logger.verbose.printf( "Ok.%n" );
        logger.info.printf( "%nConnected.%n" );
    }//end main

    //----------------------------------------------------

    public boolean disconnect() throws IOException{
        if( httpclient.execute( new HttpGet( plateformLinks.logoutUrl() ) ).getStatusLine().getStatusCode() == 200 ){
            logger.info.printf( "Disconnected.%n" );
            return true;
        }else{
            return false;
        }
    }//end disconnect


    /* *****************************************************************
     * Resources download
     * ****************************************************************/


    public void setResourcesConsumer( ResourceConsumer<String, InputStream> resourcesConsumer ){
        this.resourcesConsumer = resourcesConsumer;
    }


    public void getResource( String url ) throws Exception{
        getResource( url, this.resourcesConsumer );
    }


    public void getResource( String url, ResourceConsumer<String, InputStream> consumer ) throws Exception{
        HttpGet get = new HttpGet( url );
        CloseableHttpResponse response = null;

        try{
            response = httpclient.execute( get );
            if( response.getStatusLine().getStatusCode() == 200 ){
                HttpEntity entity = response.getEntity();
                logger.info.printf( "Content-type %s %n", entity.getContentType() );


                consumer.accept( entity.getContentType().getValue(), //
                        url.substring( url.lastIndexOf( "/" ) + 1, url.length() ),  //
                        entity.getContent() );
                EntityUtils.consume( entity );

            }
        }finally{
            if( response != null ) response.close();
        }

    }//end getResource

    //----------------------------------------------------


    private static Map<String, String> parseCourses( String content ){
        Map<String, String> courses = new HashMap<>();
        Document doc = Jsoup.parse( content );

        Elements as = doc.select( "li.type_course a[title]" );
        for( Element a : as ){
            courses.put( a.attr( "title" ), a.attr( "href" ) );
        }//end for

        return courses;
    }//end parseCourses


    //----------------------------------------------------


    private static String listToString( List<? extends Object> list ){
        if( list.size() == 0 ) return "[]";

        StringBuilder builder = new StringBuilder();
        builder.append( "[ " );
        for( Object o : list ){
            builder.append( o.toString() ).append( ", " );
        }//end for

        builder.replace( builder.length() - 3, builder.length() - 1, " ]" );
        return builder.toString();
    }//end listToString


    /* *****************************************************************
     * cookie handling
     * ****************************************************************/


    public void saveCookies( String path ){
        if( cookieStore != null ){
            try{
                ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream( path ) );
                out.writeObject( cookieStore );
                out.close();
                logger.info.printf( "Serialized cookieStore in %s%n", path );

            }catch( IOException e ){
                e.printStackTrace();
            }
        }
    }//end saveCookies

    //----------------------------------------------------


    public boolean restoreCookies( String path ){
        try{
            ObjectInputStream inputStream = new ObjectInputStream( new FileInputStream( path ) );
            BasicCookieStore cookies = ( BasicCookieStore ) inputStream.readObject();
            logger.info.printf( "Deserialized cookieStore saved in %s%n", path );
            for( Cookie cookie : cookies.getCookies() ){
                cookieStore.addCookie( cookie );
            }//end for
            return true;
        }catch( IOException | ClassNotFoundException e ){
            e.printStackTrace();
        }
        return false;
    }//end restoreCookies



    /* *****************************************************************
     * Functional interfaces
     * ****************************************************************/

    @FunctionalInterface
    public interface ThrowableConsumer<A>{
        void accept( A a ) throws Exception;
    }

    @FunctionalInterface
    public interface ResourceConsumer<String, InputStream>{
        void accept( String contentType, String name, InputStream stream ) throws Exception;
    }

}//end class
