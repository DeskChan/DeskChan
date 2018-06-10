pluginName = 'system'

setResourceBundle("resources")

class PluginData{
    def instance
    enum OS{ WINDOWS, UNIX, MACOS, ANDROID, IOS }
    def system
    def dataPath
    PluginData(instance){
        this.instance = instance
        def os = System.getProperty("os.name").toLowerCase()
        if (os.indexOf("win") >= 0)
            system = OS.WINDOWS
        else if (os.indexOf("mac") >= 0)
            system = OS.MACOS
        else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0)
            system = OS.UNIX
        else if (os.indexOf("android") >= 0)
            system = OS.ANDROID
        else if (os.indexOf("ios") >=0)
            system = OS.IOS
        else log("Error: unknows system, "+OS)
        dataPath = instance.getDataDirPath()
    }
}

def data = new PluginData(this)
BootCommands.initialize(pluginName, data)
OpenCommand.initialize(pluginName, data)
InternetSearch.initialize(pluginName, data)
TerminalAddon.initialize(pluginName, data)
Pinger.initialize(pluginName, data)