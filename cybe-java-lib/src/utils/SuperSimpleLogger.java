package utils;

import java.io.PrintStream;

/**
 * @author: Lucy Linder
 * @date: 18.06.2014
 */
public class SuperSimpleLogger{

    @FunctionalInterface
    public interface Outputter{
        PrintStream printf( String format, Object... args );
    }

    private static final SuperSimpleLogger DEFAULT_LOGGER = SuperSimpleLogger.getInstance( System.out::printf,
            System.out::printf, System.out::printf, System.err::printf );

    private static final SuperSimpleLogger VERBOSE_DEFAULT_LOGGER = SuperSimpleLogger.getInstance( System
            .out::printf, System.out::printf, System.out::printf, System.out::printf, System.err::printf );

    private static final Outputter silent = ( f, o ) -> null;
    private static final SuperSimpleLogger SILENT_LOGGER = SuperSimpleLogger.getInstance( silent, silent, silent,
            silent );


    public Outputter debug, verbose, warn, info, error;


    public static SuperSimpleLogger getInstance( Outputter debug, Outputter info, Outputter warn, Outputter error ){
        SuperSimpleLogger logger = new SuperSimpleLogger();
        logger.info = info;
        logger.warn = warn;
        logger.error = error;
        logger.debug = debug;
        logger.verbose = silent;
        return logger;
    }


    public static SuperSimpleLogger getInstance( Outputter verbose, Outputter debug, Outputter info, Outputter warn,
                                                 Outputter error ){
        SuperSimpleLogger logger = getInstance( debug, info, warn, error );
        logger.verbose = verbose;
        return logger;
    }


    public static SuperSimpleLogger defaultInstance(){
        return DEFAULT_LOGGER;
    }//end default


    public static SuperSimpleLogger defaultInstanceVerbose(){
        return DEFAULT_LOGGER;
    }//end default


    public static SuperSimpleLogger silentLogger(){
        return SILENT_LOGGER;
    }//end default

    /* *****************************************************************
     * setters
     * ****************************************************************/


    public void setDebug( Outputter debug ){
        if( debug == null ) debug = ( f, s ) -> null;
        this.debug = debug;
    }


    public void setVerbose( Outputter verbose ){
        this.verbose = verbose;
    }


    public void setWarn( Outputter warn ){
        this.warn = warn;
    }


    public void setInfo( Outputter info ){
        this.info = info;
    }


    public void setError( Outputter error ){
        this.error = error;
    }

    /* *****************************************************************
     * private
     * ****************************************************************/


    private SuperSimpleLogger(){
    }


    public boolean isVerbose(){
        return this.verbose != null;
    }//end isVerbose

}//end class
