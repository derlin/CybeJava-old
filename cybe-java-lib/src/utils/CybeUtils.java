package utils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A bunch of useful  methods to deal with urls, files and such.
 * User: lucy
 * Date: 20/06/14
 * Version: 0.1
 */
public class CybeUtils{

    /**
     * Write the content of an {@link InputStream} into a file.
     * Note that the inputstream won't be closed.
     *
     * @param path the filepath
     * @param in   the inputstream
     */
    public static void saveResource( String path, InputStream in ){
        try( FileOutputStream out = new FileOutputStream( new File( path ) ) ){
            IOUtils.copy( in, out );
        }catch( Exception e ){
            e.printStackTrace();
        }
    }//end saveResource


    /**
     * Return the last part of an url, i.e. everything after the last "/" and before the first # (anchor) or ?.
     * The potential parameters and anchors will be stripped.
     * <p/>
     *
     * @param url the url
     * @return the last part or null
     */
    public static String lastPartOfUrl( String url ){
        return url.replaceAll( "(#|\\?).*", "" ).replaceAll( ".*/", "" );
    }//end lastPartOf


    /**
     * Join the
     *
     * @param subpaths
     * @return
     */
    public static String pathJoin( String... subpaths ){
        StringBuilder builder = new StringBuilder();
        for( String s : subpaths ){
            if( builder.length() > 0 ) builder.append( File.separator );
            builder.append( s );
        }//end for

        return builder.toString();
    }//end pathJoin


    /**
     * Remove all the characters except [accented] letters, numbers, ".", "_" and " " from the given string.
     *
     * @param filename the string
     * @return the normalised string
     */
    public static String normaliseFilname( String filename ){
        return filename.replaceAll( "[^\\p{L}0-9\\. _-]", "" );
    }


    /**
     * @param s the string
     * @return true if s is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty( String s ){
        return s == null || s.isEmpty();
    }//end isNullOrEmpty


    /**
     * TODO
     *
     * @param filepath
     * @return
     * @throws IOException
     */
    public static String getUniqueFileId( String filepath ) throws IOException{
        return Files.readAttributes( Paths.get( filepath ), BasicFileAttributes.class ).fileKey().toString();
    }//end getUniqueFileId


}//end class
