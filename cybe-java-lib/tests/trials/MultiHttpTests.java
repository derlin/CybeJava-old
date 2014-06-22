package trials;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

/**
 * @author: Lucy Linder
 * @date: 20.06.2014
 */
public class MultiHttpTests{

    private static ExecutorService pool;
    private static HttpClient client;
    private static List<URI> uris = new ArrayList<>();


    public static void main( String[] args ) throws Exception{
        init();
        Map<Integer,Long> results = new TreeMap<>(  );
        for( int i = 1; i < 10; i++ ){
            long time = launch( i );
            results.put( i, time );
        }//end for
        for( Map.Entry<Integer, Long> entry : results.entrySet() ){
            System.out.printf( "With %d threads ====> %d%n", entry.getKey(), entry.getValue() );
        }//end for

        System.out.println( "done" );
    }//end main


    public static void init() throws Exception{
        client = HttpClients.custom().setRedirectStrategy( new LaxRedirectStrategy() ).setConnectionManager( new
                PoolingHttpClientConnectionManager() ).build();

        HttpResponse response;

        URI base = new URI( "http://pages.cs.wisc.edu/~hasti/cs368/JavaTutorial/NOTES/" );
        System.out.println( base.resolve( new URI( "JavaIO_Scanner.html" ) ) );

        response = client.execute( new HttpGet( base + "/JavaIO_Scanner.html" ) );
        Document doc = Jsoup.parse( EntityUtils.toString( response.getEntity() ) );

        for( Element link : doc.select( "a[href]" ) ){
            URI uri = base.resolve( new URI( link.attr( "href" ) ) );
            uris.add( uri );
        }//end for

    }//end init


    public static long launch( int threadPoolSize ){
        pool = Executors.newFixedThreadPool( threadPoolSize );
        List<Future<String>> calls = new ArrayList<>();

        for( URI uri : uris ){
            calls.add( upload( uri ) );
        }//end for

        long start = System.nanoTime();
        for( Future<String> call : calls ){
            try{
                System.out.println( calls.size() + " - " + call.get( 1, TimeUnit.SECONDS ) );
            }catch( Exception e ){
                System.out.println( e.toString() );
            }
        }//end for
        long ret = System.nanoTime() - start;
        pool.shutdown();

        return ret;
    }//end launch


    static int ids;


    public static Future<String> upload( final URI url ){

        CallableTask ctask = new CallableTask();
        ctask.setClient( client );
        ctask.setUrl( url.toString() );
        ctask.id = ids++;
        Future<String> f = pool.submit( ctask ); //This will create an HttpPost that posts 'largefile' to the 'url'
        return f;
    }


    static class CallableTask implements Callable{

        private String url;
        private HttpClient client;
        private int id;


        public String call(){
            HttpGet get = new HttpGet( url );

            try{
                HttpResponse response = client.execute( get );
                StatusLine line = response.getStatusLine();
                return url + "  " + line.toString();
            }catch( IOException e ){
                //e.printStackTrace();
                return e.toString();
            }finally{
                get.releaseConnection();
            }
        }


        public void setClient( HttpClient client ){
            this.client = client;
        }


        public void setUrl( String url ){
            this.url = url;
        }//end setUrl
    }

}//end class
