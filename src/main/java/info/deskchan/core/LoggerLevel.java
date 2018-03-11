package info.deskchan.core;

public enum LoggerLevel {
    /**
     *  The highest possible rank and is intended to turn off logging.
     */
    OFF(0),
    /**
     * Designates error events that might still allow the application to continue running.
     */
    ERROR(1),
    /**
     * Designates potentially harmful situations.
     */
    WARN(2),
    /**
     * Designates informational messages that highlight the progress
     * of the application at coarse-grained level.
     */
    INFO(3),
    /**
     * Designates fine-grained informational events that are most useful to debug an application.
     */
    DEBUG(4),
    /**
     * Designates finer-grained informational events than the DEBUG.
     */
    TRACE(5);

    private final int level;

    LoggerLevel(int level) {
        this.level = level;
    }

    public int getValue(){
        return level;
    }
}
