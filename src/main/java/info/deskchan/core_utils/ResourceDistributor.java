package info.deskchan.core_utils;

import info.deskchan.core.Path;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ResourceDistributor {
    public static void distribute(Path resList){
        List<String> resources = new LinkedList<>();
        try {
            resources = resList.readAllLines();
        } catch (Exception e){
            Main.log(new FileNotFoundException("Cannot find file specified for resource distribution"));
            return;
        }
        for(String resource : resources){
            String[] resource_data=resource.split(" ",3);
            try{
                Main.getPluginProxy().sendMessage(resource_data[0]+":supply-resource",new HashMap<String,Object>(){{
                    put(resource_data[1],resource_data[2]);
                }});
            } catch(Exception e){ Main.log(e);}
        }
    }
}
