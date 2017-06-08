package info.deskchan.core_utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class ResourceDistributor {
    public static void distribute(String resList){
        Path resPath;
        List<String> resources=null;
        try{
            resPath=Paths.get(resList);
            resources=Files.readAllLines(Paths.get(resList),  StandardCharsets.UTF_8);
        } catch (Exception e){
            Main.log("Cannot find file specified for resource distribution");
            return;
        }
        for(String resource : resources){
            String[] resource_data=resource.split(" ",3);
            Path resFile;
            try{
                resFile=Paths.get(resource_data[2]);
                if(!resFile.isAbsolute())
                    resFile=resPath.getParent().resolve(resource_data[2]);
                resource_data[2]=resFile.normalize().toString();
                Main.getPluginProxy().sendMessage(resource_data[0]+":supply-resource",new HashMap<String,Object>(){{
                    put(resource_data[1],resource_data[2]);
                }});
            } catch(Exception e){ Main.log(e);}
        }
    }
}
