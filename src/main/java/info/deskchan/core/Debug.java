package info.deskchan.core;

import java.util.Timer;
import java.util.TimerTask;

public class Debug {
    public static abstract class TimeTest{
        private Thread thread;
        private class TestTask extends TimerTask {
            @Override
            public void run() {
                PluginManager.log("Function is running too long, maybe it's an error");
                StackTraceElement[] traceback = thread.getStackTrace();
                for(StackTraceElement element : traceback)
                    PluginManager.log(element.toString());
            }
        }
        TimeTest(){
            Timer timer = new Timer();
            TimerTask task = new TestTask();
            thread = Thread.currentThread();
            timer.schedule(task, 10000);
            run();
            timer.cancel();
        }
        abstract void run();
    }
}
