class BootCommands{
    static void initialize(pluginName, data) {
        def instance = data.instance
        instance.sendMessage("core:add-command", [tag: pluginName + ':turn-off'])
        instance.sendMessage("core:add-command", [tag: pluginName + ':reboot'])

        instance.addMessageListener(pluginName + ':turn-off', { sender, tag, d ->
            String shutdown_info = "shutdown"
            long delay = 2000

            Map m = d
            if (m.containsKey("datetime")) {
                long _delay = ((long) m.get("datetime") - Calendar.instance.getTimeInMillis())
                if (_delay >= 0) delay = _delay
            }

            switch(data.system){
                case PluginData.OS.WINDOWS:
                    shutdown_info += ".exe -s -t 1"
                    break
                case PluginData.OS.UNIX: case PluginData.OS.MACOS:
                    shutdown_info += " -h -t 1"
                    break
            }
            if (delay > 0) {
                instance.sendMessage('DeskChan:say', 'Отключаю компьютер через ' + Math.abs(delay/1000) + ' секунды')
                instance.setTimer(delay, { s, d2 ->
                    instance.sendMessage('DeskChan:say', 'Ну, пришло время выключаться! Пока!')
                    Runtime.getRuntime().exec(shutdown_info)
                    instance.sendMessage('core:quit', 2000)
                })
            } else {
                instance.sendMessage('DeskChan:say', 'Отключаю компьютер прямо сейчас! Пока!')
                Runtime.getRuntime().exec(shutdown_info)
                instance.sendMessage('core:quit', 2000)
            }
        })

        instance.addMessageListener(pluginName + ':reboot', { sender, tag, d ->
            String shutdown_info = "shutdown"
            long delay = 2000

            Map m = d
            if (m.containsKey("datetime")) {
                long _delay = ((long) m.get("datetime") - Calendar.instance.getTimeInMillis())
                if (_delay >= 0) delay = _delay
            }

            switch(data.system){
                case PluginData.OS.WINDOWS:
                    shutdown_info += ".exe -r -t 1"
                    break
                case PluginData.OS.UNIX: case PluginData.OS.MACOS:
                    shutdown_info += " -r -t 1"
                    break
            }

            if (delay > 0) {
                instance.sendMessage('DeskChan:say', 'Перезагружаю компьютер через ' + Math.abs(delay/1000) + ' секунды')
                instance.setTimer(delay, { s, d2 ->
                    instance.sendMessage('DeskChan:say', 'Ну, пришло время перезагружаться! Пока!')
                    Runtime.getRuntime().exec(shutdown_info)
                    instance.sendMessage('core:quit', 2000)
                })
            } else {
                instance.sendMessage('DeskChan:say', 'Перезагружаю компьютер прямо сейчас! Пока!')
                Runtime.getRuntime().exec(shutdown_info)
                instance.sendMessage('core:quit', 2000)
            }
        })

        instance.sendMessage("core:set-event-link",
                [
                        eventName  : 'speech:get',
                        commandName: pluginName + ':turn-off',
                        rule       : 'выключи компьютер {datetime:DateTime}'
                ]
        )

        instance.sendMessage("core:set-event-link",
                [
                        eventName  : 'speech:get',
                        commandName: pluginName + ':reboot',
                        rule       : 'перезагрузи компьютер {datetime:DateTime}'
                ]
        )
    }
}
