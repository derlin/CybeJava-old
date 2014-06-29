package cmdline;

import gson.GsonUtils;
import utils.LevenshteinDistance;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Lucy Linder
 * @date: 27.06.2014
 */
public class CmdDoc{
    private List<CmdDescription> commandsUsage;
    private static final String NEW_LINE = System.getProperty( "line.separator" );


    public CmdDoc( InputStream stream ){
        CmdDescription[] descr = ( CmdDescription[] ) GsonUtils.getJsonFromFile( stream, new CmdDescription[ 0 ] );
        commandsUsage = new ArrayList<>( Arrays.asList( descr ) );
    }


    public String man(){
        return commandsUsage.stream()   //
                .map( CmdDescription::fullDescription )   //
                .collect( Collectors.joining( NEW_LINE + NEW_LINE ) );

    }


    public String help(){
        return "Available commands: " + commandsUsage.stream()   //
                .map( CmdDescription::toString )   //
                .collect( Collectors.joining( ", " ) );
    }


    public CmdDescription get( String cmd ){
        Optional<CmdDescription> descr = commandsUsage.stream()  //
                .filter( s -> s.name.equals( cmd ) )  //
                .findFirst();

        return descr.isPresent() ? descr.get() : null;
    }//end usage


    public CmdDescription betterMatch( String cmd ){
        return betterMatch( cmd, Integer.MAX_VALUE );
    }


    public CmdDescription betterMatch( String cmd, int threshold ){
        int minDist = Integer.MAX_VALUE;
        CmdDescription betterMatch = null;

        for( CmdDescription s : commandsUsage ){
            int distance = LevenshteinDistance.getDistance( s.name, cmd );
            if( distance < minDist ){
                minDist = distance;
                betterMatch = s;
            }
        }//end for

        return minDist < threshold ? betterMatch : null;
    }//end betterMatch


    public static void main( String[] args ){
        //System.out.println( man() );
        //System.out.println( help() );
        CmdDoc doc = new CmdDoc(CmdDoc
                .class.getResourceAsStream( "../resources/man.json" ));
        System.out.println( doc.get( "init" ) );
        System.out.println( doc.get( "inita" ) );

        Scanner in = new Scanner( System.in );
        while( true ){
            String first = in.nextLine().split( " " )[ 0 ];
            if( first.isEmpty() ) return;
            System.out.println( "did you mean " + doc.betterMatch( first ).getName() + "?" );
        }//end while
    }//end main


    public static class CmdDescription implements Comparable<CmdDescription>{
        private String name, args, descr;


        public String getName(){
            return name;
        }


        public String getArgs(){
            return args;
        }


        public String getDescr(){
            return descr;
        }


        @Override
        public String toString(){
            return name;
        }


        public String fullDescription(){
            return String.format( "%s%n   %s ", name + " " + args, descr );
        }


        public String syntax(){
            return name + " " + args;
        }


        @Override
        public int compareTo( CmdDescription o ){
            return name.compareTo( o.name );
        }
    }
}//end class
