package network;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.CybeUtils;
import utils.SuperSimpleLogger;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static utils.CybeUtils.*;

import network.CybeConnector.*;

/**
 * User: lucy
 * Date: 20/06/14
 * Version: 0.1
 */
public class CybeParser{

    private static final int DEFAULT_TIMEOUT = 1000;

    private ExecutorService pool = Executors.newWorkStealingPool();
    private CybeConnector connector;
    private HttpErrorHandler errorHandler;

    private static SuperSimpleLogger logger = SuperSimpleLogger.defaultInstance();


    {
        logger.setDebug( null );
    }


    /**
     * Create a parser using the given cybeConnector.
     *
     * @param connector the connector to use
     */
    public CybeParser( CybeConnector connector ){
        this.connector = connector;
    }


    /**
     * see {@link #findCourseResources(String, network.CybeConnector.ResourceConsumer,
     * network.CybeConnector.HttpErrorHandler)}    *
     */
    public List<Future<NameValuePair>> findCourseResources( String baseUrl, ResourceConsumer consumer ) throws
            Exception{
        return findCourseResources( baseUrl, consumer, null );
    }


    /**
     * Parse a course page and iterate over all the resources it contains, calling the consumer.accept method for each
     * one.
     * Careful: the consumer can be called in parallel from different threads..
     *
     * @param baseUrl      the url of the course page
     * @param consumer     the consumer. Note: the consumer could be called from different threads !
     * @param errorHandler the error handler to use
     * @return a list of futures, allowing you to retrieve a map of (resource name, resource url). See {@link
     *         #futuresToMap(java.util.List, int)}.
     * @throws Exception {@link IOException} and any other exception that could be thrown by the consumer
     */
    public List<Future<NameValuePair>> findCourseResources( String baseUrl, ResourceConsumer consumer,
                                                            HttpErrorHandler errorHandler ) throws Exception{

        final Set<String> alreadySeen = new TreeSet<>();
        List<Future<NameValuePair>> list = new ArrayList<>();

        connector.getResource( baseUrl, ( type, name, in ) -> {
            Document doc = Jsoup.parse( IOUtils.toString( in ) );
            Elements links = doc.select( "#region-main a[href]" );  // get all links

            links.stream()
                    // get the href attr
                    .map( link -> link.attr( "href" ) )
                            // keep only potential resource links
                    .filter( href -> href.matches( ".+((\\.pdf)|(resource)).*" ) )
                            // don't process a link twice
                    .filter( alreadySeen::add )
                            // submit the job to the pool
                    .forEach( href -> {
                        list.add( pool.submit( new CallableResourceFinder( href, consumer ) ) );
                    } );

        }, errorHandler );

        return list;
    }//end getAllResources


    /**
     * Wait for all the future tasks to complete and return their results into a map
     *
     * @param futures the list of futures
     * @param timeout the timeout, in milliseconds. It must be positive.
     * @return a map of resource names and resources urls.
     */
    public Map<String, String> futuresToMap( List<Future<NameValuePair>> futures, final int timeout ){
        // using a parallel stream here divides the runtime by a factor 2
        Map<String, String> result = futures.parallelStream()
                // wait for each task to finish
                .map( f -> CybeParser.getWithTimeout( f, timeout ) )
                        // don't process null values
                .filter( f -> f != null )
                        // convert the result to a map
                .collect( Collectors.toMap( NameValuePair::getName, NameValuePair::getValue ) );

        return result;
    }//end getListOfCourses


    /**
     * Parse the welcome page and return the list of "My Course", with their url
     *
     * @return the map
     * @throws Exception see {@link CybeConnector#getResource(String, network.CybeConnector.ResourceConsumer,
     *                   network.CybeConnector.HttpErrorHandler)}
     */
    public Map<String, String> getListOfCourses() throws Exception{

        final Map<String, String> courses = new HashMap<>();

        connector.getResource( connector.getHomeUrl(), ( ct, n, i ) -> {
            String welcomePage = IOUtils.toString( i );
            Document doc = Jsoup.parse( welcomePage );
            doc.select( "li.type_course a[title]" ).forEach( ( a ) -> {
                courses.put( a.attr( "title" ), a.attr( "href" ) );
            } );
        }, errorHandler );

        return courses;
    }//end getListOfCourses


    /* *****************************************************************
     * private utils
     * ****************************************************************/


    /*
     * get a filename from  a link of the course page
     * @param   link  the link DOM element
     * @return  the filename, or an empty string
     */
    private String getFileName( Element link ){

        String href = link.attr( "href" );
        String filename = lastPartOfUrl( href );

        if( !isNullOrEmpty( filename ) && filename.matches( ".+\\.[a-z]{2,4}$" ) ) //
        {
            return normaliseFilname( filename );
        }

        // TODO
        Element span = link.select( "span.instancename" ).first();
        span.child( 0 ).remove();
        filename = CybeUtils.normaliseFilname( span.text() ).trim();
        if( link.toString().contains( "pdf-24" ) ) filename += ".pdf";
        filename = link.select( "span.instancename" ).text();
        if( !isNullOrEmpty( filename ) ){
            return filename;
        }

        return "";
    }


    /*
     * get the result of a future. The timeout is in ms.
     */
    private static NameValuePair getWithTimeout( Future<NameValuePair> f, int timeout ){
        try{
            return f.get( timeout, TimeUnit.MILLISECONDS );
        }catch( Exception e ){
            return null;
        }
    }


    /* *****************************************************************
     * resource finder class
     * ****************************************************************/

    /*
     * Callable task which finds a resource given a starting url and then
     * calls the given consumer if anything is found.
     *
     * We need this class since the Cyberlearn platform uses redirects a lot
     * and loves to wrap resources into embedded viewers...
     */
    private class CallableResourceFinder implements Callable<NameValuePair>{
        // the starting url: can lead either to the resource or to an embedded viewer (or to nothing)
        String url;
        NameValuePair nameUrlPair; // the result
        CybeConnector.ResourceConsumer consumer; // the consumer of the resource (callback)


        private CallableResourceFinder( String url, ResourceConsumer consumer ){
            this.consumer = consumer;
            this.url = url;
        }


        @Override
        public NameValuePair call() throws Exception{
            findResource( this.url );
            return nameUrlPair;
        }


        /* try to find the resource, doing potentially multiple http gets */
        private void findResource( String url ) throws Exception{

            logger.debug.printf( "get single resource " + url );
            connector.getResource( url, ( type, finalUrl, in ) -> {

                if( type.equals( ContentType.TEXT_HTML.getMimeType() ) ){
                    // we have an html page => check for an embedded resource
                    // the viewers always have a div.resourceworkaround element
                    Document doc = Jsoup.parse( IOUtils.toString( in ) );
                    Element link = doc.select( "div" + ".resourceworkaround a" ).first();
                    String href = null;

                    if( link != null ){
                        // only one viewer, it is probably an embedded resource
                        // check the link
                        href = link.attr( "href" );
                    }else{
                        // could also be an embedded pdf (<object data="..." ... />)
                        Element object = doc.select( "object#resourceobject[data]" ).first();
                        if( object != null ) href = object.attr( "data" );
                    }
                    if( href != null ){
                        logger.debug.printf( "getting %s%n", href );
                        findResource( href );
                    } // else: neither a resource, nor a "viewer". Nothing to do


                }else{  // we have a real resource (not html)
                    // get the name, probably the last part of the url (if it is done properly)
                    String name = URLDecoder.decode( CybeUtils.lastPartOfUrl( finalUrl ), "UTF-8" );
                    logger.debug.printf( " --------- found: %s%n", name );
                    // store the result
                    nameUrlPair = new BasicNameValuePair( name, finalUrl );
                    // callback: call the client
                    consumer.accept( type, name, in );
                }
            }, errorHandler );
        }

    }

}//end class
