package props;

import com.google.gson.annotations.SerializedName;
import gson.GsonContainable;
import gson.GsonUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author: Lucy Linder
 * @date: 19.06.2014
 */
public class PlatformLinks implements GsonContainable{

    @SerializedName("home_url")
    String homeUrl;

    @SerializedName("idp")
    String rootIdp;

    @SerializedName("organisation_name")
    String name;


    public static PlatformLinks getInstance( String organisation ) throws FileNotFoundException{

        InputStream stream = PlatformLinks.class.getResourceAsStream( "../resources/" +
                organisation + ".json" );
        if( stream != null ){
            return ( PlatformLinks ) GsonUtils.getJsonFromFile( stream, new PlatformLinks() );
        }else{
            throw new FileNotFoundException( organisation + ".json file could not be found" );
        }
    }//end getInstance


    PlatformLinks(){
    }


    public PlatformLinks( String homeUrl, String rootIdp, String name ){
        this.homeUrl = homeUrl;
        this.rootIdp = rootIdp;
        this.name = name;
    }


    public String homeUrl(){
        return homeUrl;
    }


    public String organisationFormUrl(){
        return "https://wayf.switch.ch/SWITCHaai/WAYF?entityID=https%3A%2F%2Fcyberlearn.hes-so" +
                ".ch%2Fshibboleth&return=https%3A%2F%2Fcyberlearn.hes-so.ch%2FShibboleth" +
                ".sso%2FLogin%3FSAMLDS%3D1%26target%3Dhttps%253A%252F%252Fcyberlearn.hes-so" +
                ".ch%252Fauth%252Fshibboleth%252Findex.php";
    }


    public String organisationFormIdp(){
        return String.format( "%s/idp/shibboleth", rootIdp );
    }


    public String authFormUrl(){
        return String.format( "%s/idp/Authn/UserPassword", rootIdp );
    }


    public String confirmAuthUrl(){
        //return String.format( "%s/Shibboleth.sso/SAML2/POST", homeUrl );
        return "https://cyberlearn.hes-so.ch/Shibboleth.sso/SAML2/POST";
    }


    public String logoutUrl(){
        return String.format( "%s/login/logout.php", homeUrl );
    }//end getLogoutUrl


    public String organisationName(){
        return name;
    }
}//end class
