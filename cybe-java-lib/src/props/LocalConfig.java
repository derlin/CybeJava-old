package props;

import com.google.gson.annotations.SerializedName;
import gson.DoNotSerialize;
import gson.GsonContainable;
import gson.GsonUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class LocalConfig implements GsonContainable, Closeable{
    public static final String LOCAL_CONF_FILENAME = ".cybe";

    private String course;

    @SerializedName( "course_url" )
    private String courseUrl;

    @SerializedName( "inodes_to_names_mapping" )
    private Map<String, String> inodesToNamesMapping = new ConcurrentHashMap<>();

    @SerializedName( "dir" )
    private Set<String> dir = new TreeSet<>();

    @SerializedName( "ctype" )
    private Set<String> ctypes = new TreeSet<>();

    @SerializedName( "origin" )
    private Set<String> origin = new TreeSet<>();

    // -- not serialized
    @DoNotSerialize
    private String filepath;

    @DoNotSerialize
    private boolean modified;

    //-------------------------------------------------------------


    public LocalConfig(){
        this.filepath = LOCAL_CONF_FILENAME;
    }


    public LocalConfig( String path ){
        this.filepath = path;
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


    public void setFilepath( String filepath ){
        this.filepath = filepath;
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


    public String getFileFromId( String id ){
        return inodesToNamesMapping.containsKey( id ) ? inodesToNamesMapping.get( id ) : null;
    }//end getFileFromId


    public void removeFileRef( String uniqueId ){
        modified |= this.inodesToNamesMapping.remove( uniqueId ) != null;
    }


    public void cleanFileRefs( List<String> fileIds ){
        fileIds.forEach( this::removeFileRef );
    }//end cleanFileRefs


    public boolean addDir( String... dir ){
        boolean m = this.dir.addAll( Arrays.asList( dir ) );
        modified |= m;
        return m;
    }


    public boolean removeDir( String... dir ){
        boolean m = this.dir.removeAll( Arrays.asList( dir ) );
        modified |= m;
        return m;
    }


    public boolean addCtype( String... ctype ){
        boolean ret = false;
        for( String c : ctype ){
            ret |= this.ctypes.add( c );
        }//end for
        modified |= ret;
        return ret;
    }


    public boolean removeCtype( String... ctype ){
        boolean ret = false;
        for( String c : ctype ){
            ret |= this.ctypes.remove( c );
        }//end for
        modified |= ret;
        return ret;
    }


    public boolean addOrigin( String... origins ){
        boolean ret = false;
        for( String o : origins ){
            if( UrlValidator.getInstance().isValid( o ) ) ret |= this.origin.add( o );
        }//end for
        modified |= ret;
        return ret;
    }


    public boolean removeOrigin( String... origins ){
        boolean ret = false;
        for( String o : origins ){
            ret |= this.origin.remove( o );
        }//end for
        modified |= ret;
        return ret;
    }


    public void setCourse( String course ){
        this.course = course;
    }


    public void setCourseUrl( String courseUrl ){
        this.courseUrl = courseUrl;
    }


    //-------------------------------------------------------------


    public boolean save(){
        return GsonUtils.writeJsonFile( this.filepath, this );
    }//end save


    public boolean save( String filepath ){
        return GsonUtils.writeJsonFile( filepath, this );
    }//end save


    public static LocalConfig loadInstance( String path ){
        LocalConfig config = ( LocalConfig ) GsonUtils.getJsonFromFile( path, new LocalConfig() );
        if( config != null ) config.filepath = path;
        return config;
    }//end loadInstance


    public boolean isCtypeAccepted( String ctype ){
        for( String ct : ctypes ){
            if( ct.contains( ctype ) ) return true;
        }//end for
        return false;
    }

    //----------------------------------------------------


    @Override
    public void close(){
        if( modified ) save();
    }
}//end class
