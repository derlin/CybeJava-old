import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
public class CybeConnector{

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
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore( cookieStore ).build();
        List<NameValuePair> pairs;
        try{
            HttpPost httppost = new HttpPost( organisation_form_url );
            httppost.setEntity( new UrlEncodedFormEntity( new ArrayList<NameValuePair>(){{
                add( organisation_form_value );
            }} ) );

            CloseableHttpResponse response1 = httpclient.execute( httppost );
            try{
                HttpEntity entity = response1.getEntity();

                System.out.println( "Login form get: " + response1.getStatusLine() );
                String responseString = EntityUtils.toString( entity, "UTF-8" );
                Document doc = Jsoup.parse( responseString);
                pairs = new ArrayList<NameValuePair>(  );
                pairs.add( new BasicNameValuePair( "j_username", username ) );
                pairs.add( new BasicNameValuePair( "j_password", "YesYouCan6" ) );

                for( Element hidden : doc.getElementsByTag( "hidden" ) ){
                     pairs.add( new BasicNameValuePair( hidden.attr( "name" ), hidden.attr( "value" ) ) );
                }//end for

                EntityUtils.consume( entity );

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

            //HttpUriRequest login = RequestBuilder.post().setUri( new URI( "https://someportal/" ) ).addParameter(
            //        "j_username", username ).addParameter( "j_password", "YesYouCan6" ).build();
            //CloseableHttpResponse response2 = httpclient.execute( login );

            HttpPost httppost2 = new HttpPost( auth_form_url );
            httppost2.setEntity( new UrlEncodedFormEntity( pairs ) );

            CloseableHttpResponse response2 = httpclient.execute( httppost2 );

            try{
                HttpEntity entity = response2.getEntity();

                System.out.println( "Login form get: " + response2.getStatusLine() );
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
            CloseableHttpResponse response3 = httpclient.execute( httppost3 );

            System.out.println(response3.getStatusLine() );
        }finally{
            httpclient.close();
        }

    }//end main


    static class KeyVal{
        String key;
        String value;


        public KeyVal( String key, String value ){
            this.key = key;
            this.value = value;
        }
    }// end class

}//end class
