class BootCommands{
    static void initialize(pluginName, data) {
        def instance = data.instance
        instance.sendMessage("core:add-command", [tag: pluginName + ':search'])

        instance.addMessageListener(pluginName + ':search', { sender, tag, d ->
            String shutdown_info = "shutdown"
            long delay = 2

            Map m = data
            if (m.containsKey("datetime")) {
                long _delay = ((long) m.get("datetime") - Calendar.instance.getTimeInMillis()) / 1000
                if (_delay >= 0) delay = _delay
            }

            switch(data.system){
                case PluginData.OS.WINDOWS:
                    shutdown_info += ".exe -s -t " + delay
                    break
                case PluginData.OS.UNIX: case PluginData.OS.MACOS:
                    shutdown_info += " -h " + delay
                    break
            }
            if (delay > 0) {
                instance.sendMessage('DeskChan:say', 'Отключаю компьютер через ' + delay + ' секунды')
                instance.setTimer(delay, { s, d2 ->
                    sendMessage('DeskChan:say', 'Ну, пришло время выключаться! Пока!')
                    Runtime.getRuntime().exec(shutdown_info)
                    sendMessage('core:quit')
                })
            } else {
                instance.sendMessage('DeskChan:say', 'Отключаю компьютер прямо сейчас! Пока!')
                Runtime.getRuntime().exec(shutdown_info)
                instance.sendMessage('core:quit')
            }
        })

        instance.addMessageListener(pluginName + ':reboot', { sender, tag, d ->
            String shutdown_info = "shutdown"
            long delay = 2

            Map m = d
            if (m.containsKey("datetime")) {
                long _delay = ((long) m.get("datetime") - Calendar.instance.getTimeInMillis()) / 1000
                if (_delay >= 0) delay = _delay
            }

            switch(data.system){
                case PluginData.OS.WINDOWS:
                    shutdown_info += ".exe -r -t " + delay
                    break
                case PluginData.OS.UNIX: case PluginData.OS.MACOS:
                    shutdown_info += " -r " + delay
                    break
            }

            if (delay > 0) {
                instance.sendMessage('DeskChan:say', 'Перезагружаю компьютер через ' + delay + ' секунды')
                instance.setTimer(delay, { s, d2 ->
                    instance.sendMessage('DeskChan:say', 'Ну, пришло время перезагружаться! Пока!')
                    Runtime.getRuntime().exec(shutdown_info)
                    instance.sendMessage('core:quit')
                })
            } else {
                instance.sendMessage('DeskChan:say', 'Перезагружаю компьютер прямо сейчас! Пока!')
                Runtime.getRuntime().exec(shutdown_info)
                instance.sendMessage('core:quit')
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
