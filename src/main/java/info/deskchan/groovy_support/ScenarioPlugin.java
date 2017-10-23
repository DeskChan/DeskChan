package info.deskchan.groovy_support;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioPlugin implements Plugin {
    static PluginProxyInterface pluginProxy;

    Thread scenarioThread = null;
    Scenario currentScenario = null;
    @Override
    public boolean initialize(PluginProxyInterface pluginProxyInterface) {
        pluginProxy = pluginProxyInterface;
        pluginProxy.addMessageListener("groovy:run-scenario", (sender, tag, dat) -> {
            String path = null;
            if(dat instanceof String)
                path = (String) dat;
            else if(dat instanceof Map) {
                Map<String, Object> map = (Map) dat;
                path = (String) map.get("path");
            }
            if(path == null){
                pluginProxy.log("Path not specified for scenario");
                return;
            }
            currentScenario = createScenario(path);
            runScenario();
        });
        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("scenario"));
            put("msgTag", "scenario:menu");
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "path");
                put("type", "FileField");
                put("label", pluginProxy.getString("file"));
            }});
            put("controls", list);
        }});
        pluginProxy.addMessageListener("scenario:menu", (sender, tag, dat) -> {
            Map<String, Object> map = (Map) dat;

            currentScenario = createScenario((String) map.get("path"));
            runScenario();
        });
        return true;
    }

    public static Scenario createScenario(String pathString){
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setSourceEncoding("UTF-8");
        compilerConfiguration.setScriptBaseClass("info.deskchan.groovy_support.Scenario");
        Path path = Paths.get(pathString);
        compilerConfiguration.setClasspath(path.getParent().toString());
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("info.deskchan.groovy_support.Sugar");
        compilerConfiguration.addCompilationCustomizers(importCustomizer);
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        List<String> scriptLines = null;
        try {
            scriptLines = Files.readAllLines(path, Charset.forName("UTF-8"));
        } catch (Exception e){
            pluginProxy.log("Invalid path specified for scenario");
            return null;
        }
        StringBuilder scriptText = new StringBuilder();
        for(int i = 0; i<scriptLines.size(); i++){
            String line = scriptLines.get(i).trim();
            switch (line.charAt(0)){
                case '<':
                    scriptLines.set(i, line.substring(1).trim()+" = receive()");
                    break;
                case '>':
                    scriptLines.set(i, "say('"+line.substring(1).trim()+"')");
                    break;
                case '$':{
                    List<String> matches = new ArrayList<String>();
                    Matcher m = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2.)*?\\1|[^\\s]+").matcher(line.substring(1));
                    while (m.find() && matches.size()<4) {
                        matches.add(m.group());
                    }
                    if(matches.size()==0){
                        scriptLines.remove(i);
                        i--;
                        continue;
                    }
                    StringBuilder sb = new StringBuilder("sendMessage(");
                    for(int u=0; u<matches.size(); u++) {
                        String arg = matches.get(u);
                        if(arg.charAt(0)!='"' && arg.charAt(0)!='\''){
                            if(u>0) sb.append(arg.replace("\"","\\\""));
                            else sb.append('"'+arg.replace("\"","\\\"")+'"');
                        }

                        else sb.append(arg);
                        sb.append(',');
                    }
                    sb.deleteCharAt(sb.length()-1);
                    sb.append(')');
                    scriptLines.set(i, sb.toString());
                } break;
            }
            scriptText.append(scriptLines.get(i));
            scriptText.append("\n");
        }

        Script script = groovyShell.parse(scriptText.toString());
        return (Scenario) script;
    }

    public void runScenario(){
        if(currentScenario!=null) {
            if(scenarioThread!=null)
                scenarioThread.interrupt();

            scenarioThread = new Thread(){
                public void run() {
                    currentScenario.run();
                    scenarioThread = null;
                }
            };
            scenarioThread.start();
        } else pluginProxy.sendMessage("talk:request", "TECHPROBLEM");
    }
}
