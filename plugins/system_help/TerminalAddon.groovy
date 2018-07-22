class TerminalAddon{

    static String startSymbols = './'
    static int startLen = startSymbols.length()

    static void initialize(pluginName, data) {
        def instance = data.instance

        def command = pluginName + ":run-in-terminal"
        instance.setAlternative("DeskChan:user-said", command, 200)

        instance.addMessageListener(command, { sender, tag, d ->
            String text
            if (d instanceof Map)
                text = (String) d.getOrDefault("value", "")
            else text = d.toString()

            if (text == null || text.length() < startLen || text.substring(0, startLen) != startSymbols){
                instance.sendMessage("DeskChan:user-said#" + command, d)
                return
            }
            text = text.substring(startLen)
            def commandText
            switch(data.system){
                case PluginData.OS.WINDOWS:
                    commandText = 'cmd.exe /c "start "DeskChan" call ' + text + ' & pause" /k'
                    break
                case PluginData.OS.UNIX: case PluginData.OS.MACOS:
                    commandText = 'xterm -e "' + text + '"'
                    break
                default: return
            }
            println(commandText)
            Runtime.getRuntime().exec(commandText)
        })
    }
}
