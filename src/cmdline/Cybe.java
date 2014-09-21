package cmdline;

import cmdline.parsing.CliFlag;
import cmdline.parsing.CliParser;
import gson.GsonUtils;
import network.CybeConnector;
import network.CybeParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
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
import java.util.function.Function;

import static utils.SuperSimpleLogger.*;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class Cybe implements Closeable{

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
    private Map<String, String> courses;


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
        try( Cybe cybe = new Cybe( args ) ){
            ;
        }
    }//end main

    //----------------------------------------------------


    public Cybe( String[] args ){

        userDir = System.getProperty( "user.dir" );
        doc = new CmdDoc( this.getClass().getResourceAsStream( "/resources/man.json" ) );

        fillCommandMaps();
        loadLocalConfig();
        addShutdownHook();

        // parse options

        CliParser parser = new CliParser();
        parser.registerOption( "-s", new CliFlag( () -> {  // silent
            logger = SuperSimpleLogger.silentInstance();
            logger.debug.printf( "silent mode on%n." );
        } ) );

        parser.registerOption( "-i", new CliFlag( () -> { // interactive mode
            logger = SuperSimpleLogger.silentInstance();
        } ) );

        CliFlag  interactiveFlag = new CliFlag( () -> {  // debug
            logger.setDebug( SYSOUT_OPT );
            logger.debug.printf( "debug mode on%n." );
        } );
        parser.registerOption( "-d", interactiveFlag );


        List<String> params;
        try{
            params = parser.parse( args );
        }catch( Exception e ){
            logger.debug.printf( "Error while parsing arguments%n" );
            params = new ArrayList<>();
        }

        // prepare command and params
        params.removeIf( p -> p.startsWith( "-" ) );

        if( params.isEmpty() ){
            interactivePrompt();

        }else if( params.size() == 1 && params.get( 0 ).matches( "update-all" ) ){

            Collection<File> files = FileUtils.listFiles( FileUtils.getFile( userDir ),
                    FileFilterUtils.nameFileFilter( LOCAL_CONF_NAME ), TrueFileFilter.INSTANCE );

            files.forEach( confFile -> {
                userDir = confFile.getParent();
                logger.info.printf( "%n-------------------------------%n" );
                logger.info.printf( "Changed working directory to %s%n", userDir );

                if( loadLocalConfig() ){
                    execute( "pull", null );
                    localConfig.save();
                    logger.debug.printf( "Saved local config %s%n", getLocalConfigFilePath() );

                }else{
                    logger.debug.printf( "Could not load local config (%s)%n", getLocalConfigFilePath() );
                }

            } );

            localConfig = null; // don't save the localConfig in the shutdown hook
            lastCmdret = true;

        }else{
            String command = params.remove( 0 );
            lastCmdret = execute( command, params );
            if( interactiveFlag.getValue() ) interactivePrompt();
        }

        System.exit( lastCmdret ? 0 : 1 );
    }


    //----------------------------------------------------


    private void fillCommandMaps(){
        connectionlessHandlers = new HashMap<>();
        connectionlessHandlers.put( "init-global", Cybe::initGlobal );

        connectionlessHandlers.put( "dump", p -> {
            System.out.println( GsonUtils.toJson( localConfig ).replaceAll( "\\\"|\\{|\\}|\\[|\\]|,", "" ) );
            return true;
        } );

        connectionlessHandlers.put( "help", args -> helpOrMan( args, false ) );
        connectionlessHandlers.put( "man", args -> helpOrMan( args, true ) );

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
            String input = CmdUtils.prompt( in, "> ", i -> true );
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

        config.setPlatform( CmdUtils.choice( in, "For which platform do you want to use Cybe ?", supportedPlatforms,
                ( s ) -> CmdUtils.ChoiceMismatchAction.QUIT ) );
        config.setUsername( CmdUtils.prompt( in, "Your username: ", i -> true ) );
        config.setPassword( CmdUtils.prompt( in, "Your password: ", i -> true ) );

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
                String answer = CmdUtils.prompt( in, "A local config already exist. Override ? [Y|n] ", //
                        s -> ( s.isEmpty() || s.matches( "[y|n|Y|N]" ) ) );

                if( !( answer.isEmpty() || answer.matches( "Y|y" ) ) ) return true;
            }

            // get the list of courses
            if( courses == null ) courses = parser.getListOfCourses();
            // create the config file
            localConfig = new LocalConfig( getLocalConfigFilePath() );

            String selectedCourse = CmdUtils.choice( in, "Select a course", new ArrayList<>( courses.keySet() ),
                    ( s ) -> CmdUtils.ChoiceMismatchAction.RETURN_NULL );

            if( selectedCourse == null ){
                System.out.println( "Initialisation cancelled." );
                return true;
            }

            localConfig.setCourse( selectedCourse );
            localConfig.setCourseUrl( courses.get( selectedCourse ) );
            localConfig.save();
            isLocalConfigLoaded = true;

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
                        logger.info.printf( "  --> SAVING %s (thread: %s)%n", name, Thread.currentThread().getId() );
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


    private boolean helpOrMan( List<String> args, boolean isMan ){
        // no arguments, print the list of available commands
        if( args.size() == 0 ){
            System.out.println( isMan ? doc.man() : doc.help() );
        }else{
            // if a command name was specified, print its description
            String cmd = args.get( 0 );
            CmdDoc.CmdDescription descr = doc.get( cmd );

            if( descr != null ){
                System.out.println( descr.fullDescription() );
            }else{
                // the command does not exist -> print the closest command name available
                System.out.printf( "No such command %s. Did you mean %s ?%n", cmd, doc.betterMatch( cmd ) );
            }
        }
        return true;

    }//end help

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

            String choice = CmdUtils.choice( in, "Which platform do you use ?", supportedPlatforms, ( s ) -> {
                if( CmdUtils.isQuitInput( s ) ) printUsageAndQuit( "Quitting.", 0 );
                System.out.println( "Wrong input" );
                return CmdUtils.ChoiceMismatchAction.IGNORE;
            } );

            globalConfig.setPlatform( choice );
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


    private void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook( new Thread( () -> {
            logger.info.printf( "Cleaning up.%n" );
            if( localConfig != null ){
                localConfig.close();
            }
            if( connector != null ) connector.close();
            logger.info.printf( "Done.%n" );
        } ) );
    }//end addShutdownHook


    private static void printUsageAndQuit( String s, int exitStatus ){
        System.out.println( s );
        System.exit( exitStatus );
    }
}//end class
