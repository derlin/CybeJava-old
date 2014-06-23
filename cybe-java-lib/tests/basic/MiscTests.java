package basic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.CybeUtils;
import utils.SuperSimpleLogger;

import java.util.function.Supplier;

/**
 * User: lucy
 * Date: 20/06/14
 * Version: 0.1
 */
public class MiscTests {


    public static void main( String[] args ){
        //testFilenames();
        //testJsoup();
        //testNullLambda( () -> null );
        testLogger();
        String s = "sdf/lala?param=df";
        s = "https://forge.tic.eia-fr.ch/git?= sdlkfj";

        System.out.println(s.replaceAll( "(#|\\?).*", "" ).replaceAll( ".*/", "" ));
    }//end main


    public static void testJsoup(){
        String txt = "<a class=\"\" onclick=\"\" href=\"http://cyberlearn.hes-so.ch/mod/resource/view" +
                ".php?id=324626\"><img src=\"http://cyberlearn.hes-so.ch/theme/image" +
                ".php/bootstrap/core/1399440826/f/pdf-24\" class=\"iconlarge activityicon\" alt=\"Fichier\" /><span " +
                "class=\"instancename\">Flèche de cours ?<span class=\"accesshide \"> Fichier</span></span></a>";
        Document doc = Jsoup.parse( txt );
        Elements link = doc.select( "a" );
        Element span = link.select( "span.instancename" ).first();
        span.child( 0 ).remove();
        String name = CybeUtils.normaliseFilname( span.text() ).trim();
        if(link.toString().contains( "pdf-24" )) name += ".pdf";
        System.out.println(name);
    }//end testJsoup


    public static void testLogger(){
        SuperSimpleLogger logger = SuperSimpleLogger.defaultInstanceVerbose();
        logger.info.printf( "lala%n" );
        logger.error.printf( "error%n" );
        logger.debug.printf( "debug%n" );
    }//end testLogger

    public static void testNullLambda( Supplier<String> supplier ){
        SuperSimpleLogger logger = SuperSimpleLogger.defaultInstance();
        logger.verbose.printf( "lala" );
        logger.setDebug( null );
        logger.debug.printf( "prout" );
        System.out.println( supplier.get() );
    }//end testNullLambda

    public static void testFilenames(){
        String filename = "view.php?id=235452";
        System.out.printf("%s  => %s%n", filename, filename.matches( ".+\\.[a-z]{2,4}$" ));

        filename = "view.php";
        System.out.printf("%s  => %s%n", filename, filename.matches( ".+\\.[a-z]{2,4}$" ));


        filename = "lajfd/sldjf/?/Heç??^'llo pdf lalaééé .pdf";
        filename = CybeUtils.normaliseFilname( CybeUtils.lastPartOfUrl( filename ) );
        System.out.printf("%s  => %s%n", filename, filename.matches( ".+\\.[a-z]{2,4}$" ));

    }//end testFilenames
}//end class
