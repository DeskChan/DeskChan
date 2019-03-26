public class EventsInit {

    static def proxy
    static void initialize(def proxy){
        this.proxy = proxy
        proxy.sendMessage("core:add-event", [
                tag: "organizer:add-shedule",
                info: proxy.getString("add-shedule-info"),
                ruleInfo: proxy.getString("add-shedule-rule-info")
        ])

        proxy.addMessageListener("core-events:update-links:organizer:add-shedule", {sender, tag, data ->
            updateCommandsList((List) data)
        })
    }

    static void updateCommandsList(List<Map<String,Object>> commandsInfo){
        for (TimeStampCommand com : commands)
            com.cancel()
        commands.clear()
        try {
            for (int i = 0; i < commandsInfo.size(); i++)
                parseRule(commandsInfo.get(i))

            for (TimeStampCommand com : commands)
                com.start()
        } catch (Exception e){
            proxy.log("Error while updating links list")
        }
    }

    static List<TimeStampCommand> commands = new ArrayList<>()

    static void parseRule(Map<String,Object> command){
        String tag = (String) command.get("tag")
        String rule = (String) command.get("rule")
        Object msgData = command.get("msgData")

        String[] sheduleParts = rule.replaceAll('[^,:0-9]+', '').split(",")
        for (String part : sheduleParts){
            String[] items = sheduleParts.split('+')

            // exact time
            // 00:00:00
            if (items.length == 1){
                TimeStampCommand stamp = new TimeStampCommand(items[0])
                stamp.msgTag = tag
                stamp.msgData = msgData
                commands.add(stamp)
                continue
            }

            // delay
            // +00:00:00
            if (items[0].length() == 0){
                TimeStampCommand stamp = new TimeStampCommand()
                stamp.msgTag = tag
                stamp.msgData = msgData
                stamp.offset = new TimeStampCommand(items[0].substring(1))
                commands.add(stamp)
            }

            // exact + delay
            // 00:00:00+00:00:00
            else {
                TimeStampCommand stamp = new TimeStampCommand(items[0])
                stamp.msgTag = tag
                stamp.msgData = msgData
                stamp.offset = new TimeStampCommand(items[1].substring(1))
                commands.add(stamp)
            }
        }
    }

    static class TimeStampCommand {
        int hours, minutes, seconds, milseconds
        long stamp, next

        TimeStampCommand() {
            Calendar cal = Calendar.instance
            hours = cal.get(Calendar.HOUR_OF_DAY)
            minutes = cal.get(Calendar.MINUTE)
            seconds = cal.get(Calendar.SECOND)
            milseconds = cal.get(Calendar.MILLISECOND)
            stamp = (hours * 3600 * minutes * 60 + seconds) * 1000 + milseconds
        }

        TimeStampCommand(String text) {
            String[] items = text.split(':')
            hours = Integer.parseInt(items[0])
            minutes = items.length > 1 ? Integer.parseInt(items[1]) : 0
            seconds = items.length > 2 ? Integer.parseInt(items[2]) : 0
            milseconds = items.length > 3 ? Integer.parseInt(items[3]) : 0
            stamp = (hours * 3600 * minutes * 60 + seconds) * 1000 + milseconds
        }

        TimeStampCommand offset
        String msgTag
        Object msgData
        Integer timerId = null

        void start(){
            Calendar start = Calendar.instance
            start.set(Calendar.HOUR_OF_DAY, hours)
            start.set(Calendar.MINUTE, minutes)
            start.set(Calendar.SECOND, seconds)
            start.set(Calendar.MILLISECOND, milseconds)
            Calendar now = Calendar.instance

            next = offset != null ? offset.stamp : 86400000
            while (now > start){
                start.setTimeInMillis(start.getTimeInMillis() + next)
            }

            timerId = proxy.setTimer(start.getTimeInMillis() - now.getTimeInMillis(), listener)
        }

        def listener = { sender, data ->
            proxy.sendMessage(msgTag, msgData)
            timerId = proxy.setTimer(next, listener)
        }

        void cancel(){
            proxy.cancelTimer(timerId)
            timerId = null
        }
    }

}