package props;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import gson.GsonContainable;
import gson.GsonUtils;
import network.AuthContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class GlobalConfig implements GsonContainable, AuthContainer{

    public static final String GLOBAL_CONFIG_FILEPATH = //
            System.getProperty( "user.home" ) + File.separator + ".cybeconf";

    private String username;

    @SerializedName( "password" )
    private String pass;

    @SerializedName( "target platform" )
    private String platform;

    public static void main( String[] args ) throws IOException{
        Type collectionType = new TypeToken<Map<String, String>>(){
        }.getType();
        String json = new String( Files.readAllBytes( Paths.get( GLOBAL_CONFIG_FILEPATH ) ), StandardCharsets.UTF_8 );
        Map<String, String> map = new Gson().fromJson( json, collectionType );
        System.out.println( map );
    }//end main


    public static GlobalConfig getInstance(){
        return exists() ? ( GlobalConfig ) GsonUtils.getJsonFromFile( GLOBAL_CONFIG_FILEPATH,
                new GlobalConfig() ) : null;
    }


    public static boolean exists(){
        return new File( GLOBAL_CONFIG_FILEPATH ).exists();
    }//end exists


    public boolean save(){
        return GsonUtils.writeJsonFile( GLOBAL_CONFIG_FILEPATH, this );
    }//end save

    /* *****************************************************************
     * getter/setter
     * ****************************************************************/


    public String username(){
        return username;
    }


    public void setUsername( String username ){
        this.username = username;
    }


    public String password(){
        return perlPack( pass );
    }


    public void setPassword( String pass ){
        this.pass = perlUnpack( pass );
    }


    public String getPlatform(){
        return platform;
    }


    public void setPlatform( String platform ){
        this.platform = platform;
    }

    /* *****************************************************************
     * utils
     * ****************************************************************/


    /**
     * The java equivalent to
     * <code>
     * pack "H*", $vartoconvert
     * </code>
     *
     * @param hex the hexadecimal representation of str
     * @return str
     */
    public static String perlPack( String hex ){
        StringBuilder builder = new StringBuilder();

        for( int i = 0; i < hex.length() - 1; i += 2 ){
            //grab the hex in pairs
            String output = hex.substring( i, ( i + 2 ) );
            //convert hex to decimal
            int decimal = Integer.parseInt( output, 16 );
            //convert the decimal to character
            builder.append( ( char ) decimal );

        }
        return builder.toString();
    }//end packH

    //----------------------------------------------------


    /**
     * The java equivalent to
     * <code>
     * unpack "H*", $vartoconvert
     * </code>
     *
     * @param str
     * @return the hexadecimal representation of str
     */
    public static String perlUnpack( String str ){
        StringBuilder builder = new StringBuilder();

        for( char c : str.toCharArray() ){
            builder.append( Integer.toHexString( c ) );
        }//end for

        return builder.toString();
    }//end unpack


}//end class
