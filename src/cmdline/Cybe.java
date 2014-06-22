package cmdline;

import gson.GsonUtils;
import network.CybeConnector;
import props.LocalConfig;
import utils.CybeUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static utils.SuperSimpleLogger.Outputter;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class Cybe {

    private static final String LOCAL_CONF_NAME = ".cybe";

    private static final String[] defaultCtypes = new String[]{ "text/pdf", "text/plain" };

    private String userDir;
    private LocalConfig localConfig;

    private CybeConnector connector;

    private Outputter out = ( f, o ) -> null;

    private static String[] supportedPlatforms = new String[]{ "cyberlearn.hes-so", "moodle.unil" };

    private Scanner in = new Scanner( System.in );

    @FunctionalInterface
    public interface ArgsHandler<String> {
        void process( String... args );
    }


    private Map<String, ArgsHandler<String>> connectionlessHandlers = new HashMap<>(),
            connectionfullHandlers = new HashMap<>();
    ;


    public static void main( String[] args ){
        //userDir = System.getProperty( "user.dir" );
        //localConfig = ( LocalConfig ) GsonUtils.getJsonFromFile( //
        //        userDir.concat( File.separator ).concat( LOCAL_CONF_NAME ), //
        //        new LocalConfig() );
        //
        //if(localConfig == null || isNullOrEmpty( localConfig.courseUrl )){
        //    System.out.println( "Directory not initialised. Try init");
        //}
        //
        //Scanner in = new Scanner( System.in );
        //
        //
        //if( args.length == 0 ){
        //    System.exit( 1 );
        //}


    }//end main


    public Cybe(){
        userDir = System.getProperty( "user.dir" );

        connectionlessHandlers = new HashMap<>();
        connectionlessHandlers.put( "init-global", this::initGlobal );
        connectionlessHandlers.put( "add-origin", this::origin );
        connectionlessHandlers.put( "rm-origin", this::origin );
        connectionlessHandlers.put( "add-ctype", this::ctype );
        connectionlessHandlers.put( "rm-ctype", this::ctype );
        connectionlessHandlers.put( "add-dir", this::dir );
        connectionlessHandlers.put( "rm-dir", this::dir );

        connectionfullHandlers.put( "init", this::init );
        connectionfullHandlers.put( "pull", this::pull );
        if( loadLocalConfig() ){

        }
    }


    public void setOutputter( Outputter outputter ){
        this.out = outputter;
    }//end setOutputter


    private void initGlobal( String... args ){
        System.out.println( "init global" );
    }//end initGlobal


    private void init( String... args ){
        System.out.println( "init global" );
    }//end initGlobal


    private void origin( String... args ){

        if( args[ 0 ].equals( "add-origin" ) ){
            if( args.length < 2 ) printUsageAndQuit( "Usage: add-origin <url> [, <urls>]", 1 );
            for( int i = 1; i < args.length; i++ ){
                if( localConfig.removeOrigin( args[ i ] ) ){
                    out.printf( "added origin '%s'%n", args[ i ] );
                }

            }//end for

        }else if( args[ 0 ].equals( "remove-origin" ) ){
            if( args.length < 2 ) printUsageAndQuit( "Usage: remove-origin <url> [, <urls>]", 1 );

            for( int i = 1; i < args.length; i++ ){
                if( localConfig.addOrigin( args[ i ] ) ){
                    out.printf( "removed origin '%s'%n", args[ i ] );
                }
            }//end for
        }
    }//end origin


    private void ctype( String... args ){
        boolean add = args[ 0 ].startsWith( "add-" );

        if( args.length < 2 ) printUsageAndQuit( "Usage: [add|rm]-ctype <ctype> [, <ctype>]", 1 );
        for( int i = 1; i < args.length; i++ ){
            if( add ){
                localConfig.addCtype( args[ i ] );
            }else{
                localConfig.removeCtype( args[ i ] );
            }
            out.printf( "%s origin '%s'%n", add ? "added" : "removed", args[ i ] );
        }//end for

    }//end ctype


    private void dir( String... args ){
        boolean add = args[ 0 ].startsWith( "add-" );

        if( args.length < 2 ) printUsageAndQuit( "Usage: [add|rm]-dir <path> [, <path>]", 1 );
        for( int i = 1; i < args.length; i++ ){
            if( add ){
                localConfig.addDir( args[ i ] );
            }else{
                localConfig.addDir( args[ i ] );
            }
            out.printf( "%s dir '%s'%n", add ? "added" : "removed", args[ i ] );
        }//end for

    }//end ctype


    private void pull( String... args ){
        try{
            connector.getResource( localConfig.getCourseUrl(), ( ctype, name, in ) -> {
                if( isCtypeAccepted( ctype ) ){
                    String path = userDir + File.separator + name;
                    CybeUtils.saveResource( path, in );
                    localConfig.putFileRef( CybeUtils.getUniqueFileId( path ), name );
                }
            }, null );
        }catch( Exception e ){
            e.printStackTrace();
        }
    }//end pull

    //-------------------------------------------------------------


    private boolean loadLocalConfig(){
        localConfig = ( LocalConfig ) GsonUtils.getJsonFromFile( //
                userDir.concat( File.separator ).concat( LOCAL_CONF_NAME ), //
                new LocalConfig() );
        return localConfig != null && !CybeUtils.isNullOrEmpty( localConfig.getCourseUrl() );
    }


    private boolean isCtypeAccepted( String ctype ){
        for( String defaultCtype : defaultCtypes ){
            if( defaultCtype.contains( ctype ) ) return true;
        }//end for
        return localConfig.isCtypeAccepted( ctype );
    }//end isCtypeAccepted


    private static void printUsageAndQuit( String s, int exitStatus ){
        System.out.println( s );
        System.exit( exitStatus );
    }
}//end class
