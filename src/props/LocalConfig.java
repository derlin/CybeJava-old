package props;

import com.google.gson.annotations.SerializedName;
import gson.GsonContainable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class LocalConfig implements GsonContainable{
    public String course;

    @SerializedName( "course_url" )
    public String courseUrl;

    @SerializedName( "inodes_to_names_mapping" )
    public Map<String, String> inodesToNamesMapping;

    public List<String> dir;

    @SerializedName( "ctypes" )
    public ArrayList<String> ctypes;

}//end class
