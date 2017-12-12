package info.deskchan.core;

import java.util.Timer;
import java.util.TimerTask;

public class Debug {
    /** Track time of function work. Prints current stack trace if work time above 10 seconds. **/
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

    /** Print current traceback to console. **/
    public static void printTraceBack(){
        StackTraceElement[] traceback = Thread.currentThread().getStackTrace();
        for(StackTraceElement element : traceback)
            PluginManager.log(element.toString());
    }
}
