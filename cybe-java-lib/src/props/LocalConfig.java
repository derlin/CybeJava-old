package props;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import gson.GsonContainable;
import gson.GsonUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.*;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class LocalConfig implements GsonContainable {
    private String course;

    @SerializedName( "course_url" )
    private String courseUrl;

    @SerializedName( "inodes_to_names_mapping" )
    private Map<String, String> inodesToNamesMapping = new TreeMap<>();

    @SerializedName( "dir" )
    private Set<String> dir = new TreeSet<>();

    @SerializedName( "ctype" )
    private Set<String> ctypes = new TreeSet<>();

    @SerializedName( "origin" )
    private Set<String> origin = new TreeSet<>();

    // -- not serialized
    @Expose( serialize = false, deserialize = false )
    private String filepath;
    @Expose( serialize = false, deserialize = false )
    private boolean modified;

    //-------------------------------------------------------------


    public LocalConfig(){
    }


    public LocalConfig( String fullpath, String course, String courseUrl ){
        this.filepath = fullpath;
        this.course = course;
        this.courseUrl = courseUrl;
    }


    //----------------------------------------------------


    public String getFilepath(){
        return filepath;
    }


    public String getCourse(){
        return course;
    }


    public String getCourseUrl(){
        return courseUrl;
    }


    public void putFileRef( String uniqueId, String filename ){
        modified |= this.inodesToNamesMapping.put( uniqueId, filename ) != null;
    }


    public void removeFileRef( String uniqueId ){
        this.inodesToNamesMapping.remove( uniqueId );
    }


    public void cleanFileRefs( List<String> fileIds ){
        fileIds.forEach( this.inodesToNamesMapping::remove );
    }//end cleanFileRefs


    public boolean addDir( String... dir ){
        return this.dir.addAll( Arrays.asList( dir ) );
    }


    public boolean rmDir( String... dir ){
        return this.dir.addAll( Arrays.asList( dir ) );
    }


    public boolean addCtype( String... ctype ){
        boolean ret = false;
        for( String c : ctype ){
            ret |= this.ctypes.add( c );
        }//end for
        return ret;
    }


    public boolean removeCtype( String... ctype ){
        boolean ret = false;
        for( String c : ctype ){
            ret |= this.ctypes.remove( c );
        }//end for
        return ret;
    }


    public boolean addOrigin( String... origins ){
        boolean ret = false;
        for( String o : origins ){
            ret |= this.origin.add( o );
        }//end for
        return ret;
    }


    public boolean removeOrigin( String... origins ){
        boolean ret = false;
        for( String o : origins ){
            if( UrlValidator.getInstance().isValid( o ) ) ret |= this.origin.remove( o );
        }//end for
        return ret;
    }


    //-------------------------------------------------------------


    public boolean save(){
        return GsonUtils.writeJsonFile( this.filepath, this );
    }//end save


    public static void loadInstance( String path ){
        LocalConfig config = ( LocalConfig ) GsonUtils.getJsonFromFile( path, new LocalConfig() );
        config.filepath = path;
    }//end loadInstance


    public boolean isCtypeAccepted( String ctype ){
        for( String ct : ctypes ){
            if( ct.contains( ctype ) ) return true;
        }//end for
        return false;
    }
}//end class
