import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author: Lucy Linder
 * @date: 18.06.2014
 */
public class CybeConnector {

    private static final String organisation_form_url = "https://wayf.switch" +
            ".ch/SWITCHaai/WAYF?entityID=https%3A%2F%2Fcyberlearn.hes-so" +
            ".ch%2Fshibboleth&return=https%3A%2F%2Fcyberlearn.hes-so.ch%2FShibboleth" +
            ".sso%2FLogin%3FSAMLDS%3D1%26target%3Dhttps%253A%252F%252Fcyberlearn.hes-so" +
            ".ch%252Fauth%252Fshibboleth%252Findex.php";

    private static final String auth_form_url = "https://aai-logon.hes-so.ch/idp/Authn/UserPassword";
    private static final String confirm_form_url = "https://cyberlearn.hes-so.ch/Shibboleth.sso/SAML2/POST";

    private static final NameValuePair organisation_form_value = new BasicNameValuePair( "user_idp",
            "https://aai-logon.hes-so.ch/idp/shibboleth" );

    private static final String username = "lucy.linder";


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


    public static void main( String[] args ) throws Exception{

        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore( cookieStore )
                .setRedirectStrategy( new LaxRedirectStrategy() ).build();
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add( new BasicNameValuePair( "j_username", username ) );
        pairs.add( new BasicNameValuePair( "j_password", "YesYouCan6" ) );

        try{
            HttpPost httppost = new HttpPost( organisation_form_url );
            httppost.setEntity( new UrlEncodedFormEntity( new ArrayList<NameValuePair>() {{
                add( organisation_form_value );
            }} ) );

            CloseableHttpResponse response1 = httpclient.execute( httppost );
            try{
                HttpEntity entity = response1.getEntity();

                String responseString = EntityUtils.toString( entity, "UTF-8" );
                pairs.addAll( getHiddenFields( responseString ) );
                System.out.println("response2 hidden fields " + listToString( pairs ));

                EntityUtils.consume( entity );

                System.out.println( "organisation form: " + response1.getStatusLine() );
                System.out.println( "Initial set of cookies:" );
                List<Cookie> cookies = cookieStore.getCookies();
                if( cookies.isEmpty() ){
                    System.out.println( "None" );
                }else{
                    for( int i = 0; i < cookies.size(); i++ ){
                        System.out.println( "- " + cookies.get( i ).toString() );
                    }
                }

            }finally{
                response1.close();
            }

            //HttpUriRequest login = RequestBuilder.post().setUri( new URI( "https://someportal/" ) )
            // .addParameter(
            //        "j_username", username ).addParameter( "j_password", "YesYouCan6" ).build();
            //CloseableHttpResponse response2 = httpclient.execute( login );

            HttpPost httppost2 = new HttpPost( auth_form_url );
            httppost2.setEntity( new UrlEncodedFormEntity( pairs ) );

            CloseableHttpResponse response2 = httpclient.execute( httppost2 );

            try{
                HttpEntity entity = response2.getEntity();

                System.out.println( "Login form get: " + response2.getStatusLine() );
                String responseString = EntityUtils.toString( entity, "UTF-8" );
                pairs.addAll( getHiddenFields( responseString ) );
                System.out.println("response2 hidden fields " + listToString( pairs ));
                EntityUtils.consume( entity );

                System.out.println( "Post logon cookies:" );
                List<Cookie> cookies = cookieStore.getCookies();
                if( cookies.isEmpty() ){
                    System.out.println( "None" );
                }else{
                    for( int i = 0; i < cookies.size(); i++ ){
                        System.out.println( "- " + cookies.get( i ).toString() );
                    }
                }
            }finally{
                response2.close();
            }


            HttpPost httppost3 = new HttpPost( confirm_form_url );
            httppost3.setEntity( new UrlEncodedFormEntity( pairs ) );
            CloseableHttpResponse response3 = httpclient.execute( httppost3 );

            System.out.println( response3.getStatusLine() );
        }finally{
            httpclient.close();
        }

    }//end main


    private static List<NameValuePair> getHiddenFields( String body ){
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        Document doc = Jsoup.parse( body );

        for( Element input : doc.getElementsByTag( "input" ) ){
            if( input.attr( "type" ).equals( "hidden" ) ){
                pairs.add( new BasicNameValuePair( input.attr( "name" ), input.attr( "value" ) ) );
            }
        }//end for

        return pairs;
    }//end getHiddenFields


    private static String listToString( List<? extends Object> list ){
        if(list.size() == 0) return "[]";

        StringBuilder builder = new StringBuilder(  );
        builder.append( "[ " );
        for( Object o : list ){
           builder.append( o.toString() ).append( ", " );
        }//end for

        builder.replace( builder.length() - 3, builder.length() - 1, " ]" );
        return builder.toString();
    }//end listToString

}//end class
