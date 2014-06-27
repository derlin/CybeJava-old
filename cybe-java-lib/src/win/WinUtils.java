package win;

import com.sun.jna.platform.win32.WinNT;

import static win.Kernel32.*;

/**
 * @author: Lucy Linder
 * @date: 27.06.2014
 */
public class WinUtils{
    /**
     * return a string uniquely identifying a file on a windows ntfs drive.
     *
     * @param filepath the path to the file
     * @return {@code "[volumes serial nbr]:[file index high]:[file index low]"}
     */
    public static String getUniqueFileId( String filepath ){
        // open the file
        WinNT.HANDLE hFile = com.sun.jna.platform.win32.Kernel32.INSTANCE.CreateFile( filepath, GENERIC_READ,
                FILE_SHARE_READ, null, OPEN_EXISTING, FILE_ATTRIBUTE_ARCHIVE, null );

        // get the native handle for this file
        win.Kernel32.BY_HANDLE_FILE_INFORMATION lpFileInformation = new Kernel32.BY_HANDLE_FILE_INFORMATION();
        Kernel32.INSTANCE.GetFileInformationByHandle( hFile, lpFileInformation ); // get handle infos

        String id = String.format( "%s:%s:%s", lpFileInformation.dwVolumeSerialNumber,
                lpFileInformation.nFileIndexHigh, lpFileInformation.nFileIndexLow );

        // don't forget to close the file
        com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle( hFile );
        return id;
    }

}//end class
