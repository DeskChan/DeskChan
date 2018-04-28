package info.deskchan.core_utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Browser {

    public static void browse(String value) throws Exception {
        String[] command;
        if (SystemUtils.IS_OS_WINDOWS) {
            command = new String[]{ "cmd", "/c", "start", "\"\"", wrap(value) };
        } else if (SystemUtils.IS_OS_MAC) {
            command = new String[]{"open", unwrap(value) };
        } else if (SystemUtils.IS_OS_LINUX) {
            command = new String[]{"xdg-open", unwrap(value) };
        } else {
            throw new Exception("System is not supported for links opening yet, platform: " + System.getProperty("os.name"));
        }
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line = "";
        while ((line = reader.readLine())!= null) {
            System.out.println(line + "\n");
        }
    }

    private static String wrap(String val){
        if(val.charAt(0) !='"') val = '"' + val;
        if(val.charAt(val.length()-1) != '"') val = val + '"';
        return val;
    }

    private static String unwrap(String val){
        if(val.charAt(0) =='"') val = val.substring(1);
        if(val.charAt(val.length()-1) == '"') val = val.substring(0, val.length()-1);
        return val;
    }
}
