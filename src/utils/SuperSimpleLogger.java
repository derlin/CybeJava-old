package utils;

import java.io.PrintStream;

/**
 * @author: Lucy Linder
 * @date: 18.06.2014
 */
public class SuperSimpleLogger{

    private static final SuperSimpleLogger DEFAULT_LOGGER = SuperSimpleLogger.getInstance( System.out::printf,
            System.out::printf, System.out::printf, System.out::printf );
    private static final SuperSimpleLogger VERBOSE_DEFAULT_LOGGER = SuperSimpleLogger.getInstance( System.out::printf,
    System.out::printf, System.out::printf, System.out::printf, System.out::printf );

    @FunctionalInterface
    public interface Outputter{
        PrintStream printf( String format, Object... args );
    }

    public Outputter debug, verbose, warn, info, error;


    public static SuperSimpleLogger getInstance( Outputter debug, Outputter info, Outputter warn, Outputter error ){
        SuperSimpleLogger logger = new SuperSimpleLogger();
        logger.info = info;
        logger.warn = warn;
        logger.error = error;
        logger.debug = debug;
        logger.verbose = (f, o) -> { return null; };
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


    /* *****************************************************************
     * private constructors
     * ****************************************************************/


    private SuperSimpleLogger(){
    }


    public boolean isVerbose(){
        return this.verbose != null;
    }//end isVerbose
}//end class
