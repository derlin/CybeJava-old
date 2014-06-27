package cmdline;

import gson.GsonUtils;
import network.CybeConnector;
import network.CybeParser;
import org.apache.http.NameValuePair;
import props.GlobalConfig;
import props.LocalConfig;
import props.PlatformLinks;
import utils.CmdDoc;
import utils.CybeUtils;
import utils.SuperSimpleLogger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;

import static utils.SuperSimpleLogger.*;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class CybeStatic implements Closeable{

    private static final String LOCAL_CONF_NAME = ".cybe";
    private static final int PULL_TIMEOUT_SEC = 15;
    private static final List<String> supportedPlatforms = Arrays.asList( "cyberlearn.hes-so", "moodle.unil" );
    private static final List<String> defaultCtypes = Arrays.asList( "pdf", "text/plain" );

    private static Scanner in = new Scanner( System.in );

    private String userDir;
    private LocalConfig localConfig;
    private boolean isLocalConfigLoaded;

    private Collection<String> existingResources;
    private CybeConnector connector;
    private CybeParser parser;
    private CmdDoc doc;
    private SuperSimpleLogger logger = getInstance( SILENT_OPT, SYSOUT_OPT, SYSOUT_OPT, SYSERR_OPT );
    private boolean lastCmdret;


    @FunctionalInterface
    public interface CommandExecutor<String>{
        boolean process( List<String> args );
    }

    @FunctionalInterface
    public interface VarargFunction<A, B>{
        A apply( B... args );
    }


    private Map<String, CommandExecutor<String>> connectionlessHandlers = new HashMap<>(),
            connectionfullHandlers = new HashMap<>();

    //----------------------------------------------------


    public static void main( String[] args ) throws Exception{
        try( CybeStatic cybe = new CybeStatic( args ) ){
            ;
        }
    }//end main

    //----------------------------------------------------


    public CybeStatic( String[] argv ){

        //userDir = new File( "/home/lucy/Dropbox/projets/cybe-java/tests/out" ).getAbsolutePath();//new File
        userDir = System.getProperty( "user.dir" );        // ( "/tmp/out" ).getAbsolutePath();

        Runtime.getRuntime().addShutdownHook( new Thread( () -> {
            if( localConfig != null ){
                localConfig.close();
                logger.info.printf( "Saved config file.%n" );
            }
            if( connector != null ) connector.close();
            logger.info.printf( "Done.%n" );
        } ) );

        doc = new CmdDoc( this.getClass().getResourceAsStream( "/resources/man.json" ) );

        fillCommandMaps();
        loadLocalConfig();

        // parse options
        List<String> params = new ArrayList<>( Arrays.asList( argv ) );
        boolean silent = params.stream().anyMatch( "-s"::equals );
        boolean interactive = params.stream().anyMatch( "-i"::equals );
        boolean debug = params.stream().anyMatch( "-d"::equals );

        if( silent ) logger = SuperSimpleLogger.silentInstance();
        if( debug ) logger.setDebug( SYSOUT_OPT );

        logger.debug.printf( "Options: s = %b, i = %b, d = %b%n", silent, interactive, debug );

        // prepare command and params
        params.removeIf( p -> p.startsWith( "-" ) );

        if( params.size() == 0 ){
            interactivePrompt();

        }else{
            String command = params.remove( 0 );
            lastCmdret = execute( command, params );
            if( interactive ) interactivePrompt();
        }
        System.exit( lastCmdret ? 0 : 1 );
    }


    //----------------------------------------------------


    private void fillCommandMaps(){
        connectionlessHandlers = new HashMap<>();
        connectionlessHandlers.put( "init-global", CybeStatic::initGlobal );

        connectionlessHandlers.put( "dump", p -> {
            System.out.println( GsonUtils.toJson( localConfig ).replaceAll( "\\\"|\\{|\\}|\\[|\\]|,", "" ) );
            return true;
        } );

        connectionlessHandlers.put( "help", args -> {
            System.out.println( doc.help() );
            return true;
        } );
        connectionlessHandlers.put( "man", args -> {
            System.out.println( doc.man() );
            return true;
        } );

        connectionlessHandlers.put( "add-origin", args -> add( localConfig::addOrigin, args ) );  //
        connectionlessHandlers.put( "rm-origin", args -> remove( localConfig::removeOrigin, args ) );  //

        connectionlessHandlers.put( "add-ctype", args -> add( localConfig::addCtype, args ) );  //
        connectionlessHandlers.put( "rm-ctype", args -> remove( localConfig::removeCtype, args ) );  //

        connectionlessHandlers.put( "add-dir", args -> add( localConfig::addDir, args ) );  //
        connectionlessHandlers.put( "rm-dir", args -> remove( localConfig::removeDir, args ) );  //

        connectionfullHandlers.put( "init", this::init );
        connectionfullHandlers.put( "pull", this::pull );
    }

    //----------------------------------------------------


    @Override
    public void close() throws IOException{
        if( localConfig != null ) localConfig.close();
    }

    //----------------------------------------------------


    private void interactivePrompt(){

        List<String> params;
        String command;

        while( true ){
            String input = prompt( "> ", i -> true );
            params = new ArrayList<>( Arrays.asList( input.split( " +" ) ) );
            command = params.remove( 0 );

            if( CybeUtils.isNullOrEmpty( command ) ||   //
                    command.equalsIgnoreCase( "quit" ) || command.equalsIgnoreCase( "exit" ) ){
                break;
            }

            lastCmdret = execute( command, params );
        }

    }//end interactivePrompt

    //----------------------------------------------------


    public boolean execute( String cmd, List<String> args ){
        boolean ok;

        if( !cmd.equals( "init" ) && !isLocalConfigLoaded ){
            logger.info.printf( "Directory not initialised. Try cybe init.%n" );
            return false;
        }

        if( connectionlessHandlers.containsKey( cmd ) ){
            ok = connectionlessHandlers.get( cmd ).process( args );

        }else if( connectionfullHandlers.containsKey( cmd ) ){
            if( !createConnectorAndParser() ) printUsageAndQuit( "Could not connect...", 1 );
            ok = connectionfullHandlers.get( cmd ).process( args );

        }else{
            System.out.println( "Unknown command: " + cmd );
            System.out.println( "did you mean " + doc.betterMatch( cmd ).getName() + "?" );
            return false;
        }
        if( !ok ) System.out.println( "Usage: " + doc.get( cmd ).syntax() );

        return ok;

    }//end execute


    /* *****************************************************************
     * lambda commands
     * ****************************************************************/


    private boolean add( VarargFunction<Boolean, String> method, List<String> params ){
        if( params.size() == 0 ) return false;
        params.stream().map( p -> p + " : " + ( method.apply( p ) ? "added.%n" : "failed.%n" ) ) //
                .forEach( logger.info::printf );
        return true;
    }//end add


    private boolean remove( VarargFunction<Boolean, String> method, List<String> params ){
        if( params.size() == 0 ) return false;
        int sum = params.stream().mapToInt( p -> ( method.apply( p ) ? 1 : 0 ) ).sum();
        logger.info.printf( "%d items removed.%n", sum );
        return true;
    }//end remove


    private static boolean initGlobal( List<String> args ){
        GlobalConfig config = new GlobalConfig();

        config.setPlatform( choice( "For which platform do you want to use Cybe ?", supportedPlatforms ) );
        config.setUsername( prompt( "Your username: ", i -> true ) );
        config.setPassword( prompt( "Your password: ", i -> true ) );

        return config.save();
    }//end initGlobal


    /*
     * create a .cybe file in the current userDir with the course and courseUrl infos:
     * - if the file already exists, prompt for a confirmation
     * - get the available courses by parsing the welcome page
     * - let the user choose a course
     */
    private boolean init( List<String> args ){
        try{

            if( localConfigFileExists() ){
                // if the file already exists, prompt for a confirmation
                String answer = prompt( "A local config already exist. Override ? [Y|n]", //
                        s -> ( s.isEmpty() || s.matches( "[y|n|Y|N]" ) ) );

                if( !( answer.isEmpty() || answer.matches( "Y|y" ) ) ) return true;
            }

            // get the list of courses
            Map<String, String> courses = parser.getListOfCourses();
            // create the config file
            localConfig = new LocalConfig( getLocalConfigFilePath() );
            String selectedCourse = choice( "Select a course", new ArrayList<>( courses.keySet() ) );
            localConfig.setCourse( selectedCourse );
            localConfig.setCourseUrl( courses.get( selectedCourse ) );
            localConfig.save();

        }catch( Exception e ){
            logger.error.printf( "error while init.%n" );
            e.printStackTrace();
            return false;
        }

        return true;
    }//end init


    private boolean pull( List<String> args ){
        try{
            List<Future<NameValuePair>> futures = parser.findCourseResources( //
                    localConfig.getCourseUrl(), ( ctype, name, in ) -> {
                try{
                    logger.debug.printf( "=== %s%n", name );
                    if( isCtypeAccepted( ctype ) && !existingResources.contains( name ) ){
                        existingResources.add( name ); // mark this file as handled
                        String path = CybeUtils.concatPath( userDir, name );
                        CybeUtils.saveResource( path, in ); // save the resource
                        logger.debug.printf( "SAVING %s (thread: %s)%n", name, Thread.currentThread().getId() );
                        // add its unique id to the inodesToNameMapping
                        localConfig.putFileRef( CybeUtils.getUniqueFileId( path ), name );

                    }

                }catch( Exception e ){
                    logger.warn.printf( "Error while downloading resource %s%n", name );
                    logger.error.printf( "Exception inside pull handler : %s%s%n", e, e.getMessage() );
                }
            }, ( url, e ) -> System.err.println( url + ": " + e.getStatusLine() ) );

            parser.futuresToMap( futures, PULL_TIMEOUT_SEC );
            logger.debug.printf( "FUTURES GATHERED%n" );
        }catch( Exception e ){
            logger.error.printf( "error while pulling.%n" );
            e.printStackTrace();
            return false;
        }

        return true;
    }//end pull

    /* *****************************************************************
     * utils to deal with input cmdline
     * ****************************************************************/


    private static String prompt( String message, Predicate<String> validator ){
        String answer;
        do{
            System.out.print( message );
            answer = in.nextLine();
        }while( !validator.test( answer ) );

        return answer;
    }//end prompt


    private static String choice( String prompt, List<String> choices ){
        int i = 0, answer = -1;

        System.out.println( prompt + " " );
        for( String choice : choices )
            System.out.printf( "  [%d] %s%n", i++, choice );

        System.out.println();

        while( answer < 0 || answer >= i ){
            System.out.printf( "Your choice [0-%d] ", i - 1 );
            answer = in.nextInt();
            in.nextLine();
        }//end while

        return choices.get( answer );
    }//end choice



    /* *****************************************************************
     * config files management
     * ****************************************************************/


    private boolean loadLocalConfig(){
        if( isLocalConfigLoaded ) return true;
        File localCybe = new File( getLocalConfigFilePath() );
        if( localCybe.exists() ){
            localConfig = ( LocalConfig ) GsonUtils.getJsonFromFile( localCybe, new LocalConfig() );
            if( localConfig != null && !CybeUtils.isNullOrEmpty( localConfig.getCourseUrl() ) ){
                isLocalConfigLoaded = true;
                existingResources = new HashSet<>( getExistingResources( userDir, localConfig::getFileFromId ).values
                        () );
            }
        }
        isLocalConfigLoaded = localConfig != null && !CybeUtils.isNullOrEmpty( localConfig.getCourseUrl() );
        return isLocalConfigLoaded;

    }


    private boolean localConfigFileExists(){
        return new File( getLocalConfigFilePath() ).exists();
    }


    private String getLocalConfigFilePath(){
        return userDir.concat( File.separator ).concat( LOCAL_CONF_NAME );
    }

    /* *****************************************************************
     * private utils
     * ****************************************************************/


    private boolean isCtypeAccepted( String ctype ){
        return defaultCtypes.stream().anyMatch( ctype::contains ) ||  //
                localConfig.isCtypeAccepted( ctype );
    }


    public static Map<String, String> getExistingResources( String directory, Function<String,
            String> inodeToNameResolver ){
        File rootDir = new File( directory );
        Map<String, String> results = new HashMap<>();
        if( !rootDir.isDirectory() ) return results;

        for( File file : rootDir.listFiles( f -> !f.isDirectory() ) ){
            String id = CybeUtils.getUniqueFileId( file.getAbsolutePath() );
            String resolvedName = inodeToNameResolver.apply( id );
            results.put( id, resolvedName != null ? resolvedName : file.getName() );
        }//end for
        return results;
    }//end getExistingResources


    private boolean createConnectorAndParser(){

        if( connector != null && connector.isConnected() ) return true;

        GlobalConfig globalConfig = GlobalConfig.getInstance();
        if( globalConfig == null ){
            printUsageAndQuit( "No credentials found.\nUse cybe init-global to specify your username and " +
                    "password", 1 );
        }

        if( globalConfig.getPlatform() == null ){
            System.out.println( "The platform was not specified." );
            globalConfig.setPlatform( choice( "Which platform do you use ?", supportedPlatforms ) );
            globalConfig.save();
        }

        String platform = globalConfig.getPlatform();

        try{
            // TODO
            connector = new CybeConnector( PlatformLinks.getInstance( platform ) );
            parser = new CybeParser( connector, logger );
            connector.connect( globalConfig );
        }catch( Exception e ){
            logger.error.printf( "error while creating connector and parser.%n" );
            e.printStackTrace();
            return false;
        }
        return true;

    }//end createConnectorAndParser


    private static void printUsageAndQuit( String s, int exitStatus ){
        System.out.println( s );
        System.exit( exitStatus );
    }
}//end class
