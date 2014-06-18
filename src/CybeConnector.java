import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author: Lucy Linder
 * @date: 18.06.2014
 */
public class CybeConnector {


    @FunctionalInterface
    public interface ThrowableConsumer<A> {
        void accept( A a ) throws Exception;
    }

    private static final String ORGANISATION_FORM_URL = "https://wayf.switch" +
            ".ch/SWITCHaai/WAYF?entityID=https%3A%2F%2Fcyberlearn.hes-so" +
            ".ch%2Fshibboleth&return=https%3A%2F%2Fcyberlearn.hes-so.ch%2FShibboleth" +
            ".sso%2FLogin%3FSAMLDS%3D1%26target%3Dhttps%253A%252F%252Fcyberlearn.hes-so" +
            ".ch%252Fauth%252Fshibboleth%252Findex.php";
    private static final String AUTH_FORM_URL = "https://aai-logon.hes-so.ch/idp/Authn/UserPassword";
    private static final String CONFIRM_FORM_URL = "https://cyberlearn.hes-so.ch/Shibboleth.sso/SAML2/POST";
    private static final String LOGOUT_URL = "http://cyberlearn.hes-so.ch/login/logout.php";
    private static final String HOME_URI = "http://cyberlearn.hes-so.ch";

    private static final String username = "lucy.linder";
    private CloseableHttpClient httpclient;
    private Map<String, String> courses = new HashMap<>();


    public static void main( String[] args ) throws Exception{
        CybeConnector connector = new CybeConnector();
        connector.connect();
        connector.disconnect();
    }//end main


    public CybeConnector(){
        BasicCookieStore cookieStore = new BasicCookieStore();
        httpclient = HttpClients.custom().setDefaultCookieStore( cookieStore ).setRedirectStrategy( new
                LaxRedirectStrategy() ).build();
    }


    public static void setProperties(){

        Properties prop = new Properties();
        OutputStream output = null;

        try{

            output = new FileOutputStream( "config.properties" );

            // set the properties value
            prop.setProperty( "database", "localhost" );
            prop.setProperty( "dbuser", "mkyong" );
            prop.setProperty( "dbpassword", "password" );

            // save properties to project root folder
            prop.store( output, null );

        }catch( IOException io ){
            io.printStackTrace();
        }finally{
            if( output != null ){
                try{
                    output.close();
                }catch( IOException e ){
                    e.printStackTrace();
                }
            }

        }
    }//end setProperties


    private void connect() throws Exception{

        final List<NameValuePair> pairs = new ArrayList<>();
        pairs.add( new BasicNameValuePair( "user_idp", "https://aai-logon.hes-so.ch/idp/shibboleth" ) );
        pairs.add( new BasicNameValuePair( "j_username", username ) );
        pairs.add( new BasicNameValuePair( "j_password", "YesYouCan6" ) );

        ThrowableConsumer<String> doPostFunction = ( url ) -> {
            HttpPost httppost = new HttpPost( url );
            try{
            // create and execute the post
            httppost.setEntity( new UrlEncodedFormEntity( pairs ) );
            HttpResponse response = httpclient.execute( httppost );

            // check the answer is 200 - OK
            if( response.getStatusLine().getStatusCode() != 200 ){
                throw new Exception( String.format( "post to %s failed. Status line = %s%n", url,
                        response.getStatusLine() ) );
            }

            // get the hidden SAML fields
            HttpEntity entity = response.getEntity();
            Document doc = Jsoup.parse( EntityUtils.toString( entity, "UTF-8" ) );
            for( Element input : doc.select( "input[type=hidden]" ) ){
                pairs.add( new BasicNameValuePair( input.attr( "name" ), input.attr( "value" ) ) );
            }//end for

            // close the entity streams
            EntityUtils.consume( entity );

            }finally{
            }

        };

        doPostFunction.accept( ORGANISATION_FORM_URL );
        doPostFunction.accept( AUTH_FORM_URL );
        doPostFunction.accept( CONFIRM_FORM_URL );

        HttpEntity entity = httpclient.execute( new HttpGet( HOME_URI ) ).getEntity();
        String welcomePage = EntityUtils.toString( entity, "UTF-8" );
        parseCourses( welcomePage );
        EntityUtils.consume( entity );

        System.out.println( "Connected" );
    }//end main


    public boolean disconnect() throws IOException{
        return httpclient.execute( new HttpGet( LOGOUT_URL ) ).getStatusLine().getStatusCode() == 200;
    }//end disconnect


    private static Map<String, String> parseCourses( String content ){
        Map<String, String> courses = new HashMap<>();
        Document doc = Jsoup.parse( content );

        Elements as = doc.select( "li.type_course a[title]" );
        for( Element a : as ){
            courses.put( a.attr( "title" ), a.attr( "href" ) );
        }//end for

        return courses;
    }//end parseCourses


    private static List<NameValuePair> getHiddenFields( String body ){
        List<NameValuePair> pairs = new ArrayList<>();


        return pairs;
    }//end getHiddenFields


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

}//end class
