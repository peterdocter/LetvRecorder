package com.letv.android.recorder.tool;

import com.letv.android.recorder.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Created by snile on 9/30/14.
 */
public class FileTool {

    public static boolean acceptNewFileName(String newFileName){

        File testFileDir = new File(Constants.TEST_TEMP_FILR_PATH);
        if(!testFileDir.exists()){
            testFileDir.mkdirs();
        }

        File testFile = new File(Constants.TEST_TEMP_FILR_PATH,newFileName+Constants.RECORD_FORMAT[0]);

        if(testFile.exists()){
           testFile.delete();
        }
        testFile = new File(Constants.TEST_TEMP_FILR_PATH,newFileName+Constants.RECORD_FORMAT[1]);
        if(testFile.exists()){
            testFile.delete();
        }
        try {
            testFile.createNewFile();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

}
