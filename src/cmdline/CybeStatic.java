package cmdline;

import gson.GsonUtils;
import network.CybeConnector;
import network.CybeParser;
import org.apache.http.NameValuePair;
import props.GlobalConfig;
import props.LocalConfig;
import props.PlatformLinks;
import utils.CybeUtils;
import utils.SuperSimpleLogger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Predicate;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class CybeStatic implements Closeable{

    private static final String LOCAL_CONF_NAME = ".cybe";
    private static final int PULL_TIMEOUT_MS = 10000;
    private static final List<String> supportedPlatforms = Arrays.asList( "cyberlearn.hes-so", "moodle.unil" );
    private static final List<String> defaultCtypes = Arrays.asList( "pdf", "text/plain" );

    private static Scanner in = new Scanner( System.in );

    private String userDir;
    private LocalConfig localConfig;

    private CybeConnector connector;
    private CybeParser parser;

    private SuperSimpleLogger logger;
    private boolean lastCmdret;


    @Override
    public void close() throws IOException{
        if( localConfig != null ) localConfig.close();
    }


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


    public static void main( String[] args ) throws Exception{
        try( CybeStatic cybe = new CybeStatic( args ) ){
            ;
        }
    }//end main


    public CybeStatic( String[] argv ){

        if( argv.length == 0 ) printUsageAndQuit( "Usage: cybe command [arg [, args]]", 1 );

        userDir = new File( "tests/out" ).getAbsolutePath();//System.getProperty( "user.dir" );

        List<String> params = new ArrayList<>( Arrays.asList( argv ) );

        // parse options
        boolean silent = params.stream().anyMatch( "-s"::equals );
        boolean interactive = params.stream().anyMatch( "-i"::equals );
        boolean debug = params.stream().anyMatch( "-d"::equals );

        logger = silent ? SuperSimpleLogger.silentInstance() : SuperSimpleLogger.defaultInstance();
        if( !debug ) logger.setDebug( SuperSimpleLogger.SILENT_OPT );

        logger.debug.printf( "Options: s = %b, i = %b, d = %b%n", silent, interactive, debug );

        params.removeIf( p -> p.startsWith( "-" ) );

        String command = params.remove( 0 );
        boolean cmdAlreadyExecuted = false;

        if( "init".equals( command ) ){
            createConnectorAndParser();
            logger.debug.printf( "Executing init.%n" );
            lastCmdret = init( Arrays.asList( argv ).subList( 1, argv.length ) );
            cmdAlreadyExecuted = true;
        }

        loadLocalConfig();

        if( !isInitialised() ){
            if( interactive ){
                System.out.println( "Please, run cybe init." );
                cmdAlreadyExecuted = true;
            }else{
                printUsageAndQuit( "Directory not initialised. Try cybe init.", 1 );
            }

        }

        connectionlessHandlers = new HashMap<>();
        connectionlessHandlers.put( "init-global", CybeStatic::initGlobal );

        connectionlessHandlers.put( "dump", p -> {
            System.out.println( localConfig );
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


        if( !cmdAlreadyExecuted ) lastCmdret = execute( command, params );
        if( interactive ) interactivePrompt();

        System.exit( lastCmdret ? 0 : 1 );
    }


    private void interactivePrompt(){

        List<String> params;
        String command;

        while( true ){
            String input = prompt( "> ", i -> true );
            params = new ArrayList<>( Arrays.asList( input.split( " +" ) ) );
            command = params.remove( 0 );

            if( CybeUtils.isNullOrEmpty( command ) || command.equals( "quit" ) ) break;

            lastCmdret = execute( command, params );

        }

    }//end interactivePrompt


    private boolean add( VarargFunction<Boolean, String> method, List<String> params ){
        params.stream().map( p -> p + " : " + ( method.apply( p ) ? "added." : "failed." ) ) //
                .forEach( logger.info::printf );
        return true;
    }//end add


    private boolean remove( VarargFunction<Boolean, String> method, List<String> params ){
        int sum = params.stream().mapToInt( p -> ( method.apply( p ) ? 1 : 0 ) ).sum();
        logger.info.printf( "%d items removed.%n", sum );
        return true;
    }//end remove


    public boolean execute( String cmd, List<String> args ){

        if( !cmd.equals( "init" ) && !isInitialised() ){
            logger.info.printf( "Directory not initialised. Try cybe init.%n" );
            return false;
        }

        if( connectionlessHandlers.containsKey( cmd ) ){
            return connectionlessHandlers.get( cmd ).process( args );

        }else if( connectionfullHandlers.containsKey( cmd ) ){
            if( !createConnectorAndParser() ) printUsageAndQuit( "Could not connect...", 1 );
            return connectionfullHandlers.get( cmd ).process( args );

        }

        return false;

    }//end execute


    private static boolean initGlobal( List<String> args ){
        GlobalConfig config = new GlobalConfig();

        config.setPlatform( choice( "For which platform do you want to use Cybe ?", supportedPlatforms ) );
        config.setUsername( prompt( "Your username: ", i -> true ) );
        config.setPassword( prompt( "Your password: ", i -> true ) );

        return config.save();
    }//end initGlobal


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


    private boolean init( List<String> args ){
        try{

            if( localConfigFileExists() ){
                String answer = prompt( "A local config already exist. Override ? [Y|n]", //
                        s -> ( s.isEmpty() || s.matches( "[y|n|Y|N]" ) ) );

                if( !( answer.isEmpty() || answer.matches( "Y|y" ) ) ) return true;
            }

            localConfig = new LocalConfig();
            Map<String, String> courses = parser.getListOfCourses();
            String selectedCourse = choice( "Select a course", new ArrayList<>( courses.keySet() ) );
            localConfig.setCourse( selectedCourse );
            localConfig.setCourseUrl( courses.get( selectedCourse ) );
            localConfig.save( getLocalConfigFilePath() );

        }catch( Exception e ){
            logger.error.printf( "error while init.%n" );
            e.printStackTrace();
            return false;
        }

        return true;
    }//end initGlobal


    private boolean pull( List<String> args ){
        try{
            List<Future<NameValuePair>> futures = parser.findCourseResources( //
                    localConfig.getCourseUrl(), ( ctype, name, in ) -> {

                logger.debug.printf( "=== %s%n", name );
                if( isCtypeAccepted( ctype ) ){
                    String path = userDir + File.separator + name;
                    CybeUtils.saveResource( path, in );
                    logger.debug.printf( "SAVING %s%n", name );
                    localConfig.putFileRef( CybeUtils.getUniqueFileId( path ), name );
                }
            }, ( url, e ) -> System.err.println( url + ": " + e.getStatusLine() ) );

            parser.futuresToMap( futures, PULL_TIMEOUT_MS );

        }catch( Exception e ){
            logger.error.printf( "error while pooling.%n" );
            e.printStackTrace();
            return false;
        }

        return true;
    }//end pull

    //-------------------------------------------------------------


    private boolean loadLocalConfig(){
        File localCybe = new File( getLocalConfigFilePath() );
        if( localCybe.exists() ){
            localConfig = ( LocalConfig ) GsonUtils.getJsonFromFile( localCybe, new LocalConfig() );
        }
        return isInitialised();
    }


    private boolean isCtypeAccepted( String ctype ){
        return defaultCtypes.stream().anyMatch( ctype::contains ) ||  //
                localConfig.isCtypeAccepted( ctype );
    }//end isCtypeAccepted


    public boolean isInitialised(){
        return localConfig != null && !CybeUtils.isNullOrEmpty( localConfig.getCourseUrl() );
    }//end isInitialised
    //----------------------------------------------------


    private boolean localConfigFileExists(){
        return new File( getLocalConfigFilePath() ).exists();
    }//end localConfigFileExists


    private String getLocalConfigFilePath(){
        return userDir.concat( File.separator ).concat( LOCAL_CONF_NAME );
    }//end getLocalConfigFilePath


    private boolean createConnectorAndParser(){

        if( connector != null && connector.isConnected() ) return true;

        GlobalConfig globalConfig = GlobalConfig.getInstance();
        if( globalConfig == null ){
            printUsageAndQuit( "No credentials found.\nUse cybe init-global to specify your username and password", 1 );
        }

        if( globalConfig.getPlatform() == null ){
            System.out.println( "The platform was not specified." );
            globalConfig.setPlatform( choice( "Which platform do you use ?", supportedPlatforms ) );
            globalConfig.save();
        }

        String platform = globalConfig.getPlatform();

        try{
            connector = new CybeConnector( PlatformLinks.getInstance( platform ) );
            parser = new CybeParser( connector );
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
