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

    public static final Outputter SILENT_OPT = ( f, o ) -> null;
    public static final Outputter SYSOUT_OPT = System.out::printf;
    public static final Outputter SYSERR_OPT = System.err::printf;


    public Outputter debug, verbose, warn, info, error;


    public static SuperSimpleLogger getInstance( Outputter debug, Outputter info, Outputter warn, Outputter error ){
        SuperSimpleLogger logger = new SuperSimpleLogger();
        logger.info = info;
        logger.warn = warn;
        logger.error = error;
        logger.debug = debug;
        logger.verbose = SILENT_OPT;
        return logger;
    }


    public static SuperSimpleLogger defaultInstance(){
        SuperSimpleLogger ssl = defaultInstanceVerbose();
        ssl.verbose = SILENT_OPT;
        return ssl;
    }//end default


    public static SuperSimpleLogger defaultInstanceVerbose(){
        SuperSimpleLogger ssl = new SuperSimpleLogger();
        ssl.setAll( SYSOUT_OPT );
        ssl.setError( SYSERR_OPT );
        return ssl;
    }//end default


    public static SuperSimpleLogger silentInstance(){
        SuperSimpleLogger ssl = new SuperSimpleLogger();
        ssl.setAll( SILENT_OPT );
        return ssl;
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


    private void setAll( Outputter out ){
        info = debug = error = verbose = warn = out;
    }//end setAll

    public boolean isVerbose(){
        return this.verbose != null;
    }//end isVerbose

}//end class
