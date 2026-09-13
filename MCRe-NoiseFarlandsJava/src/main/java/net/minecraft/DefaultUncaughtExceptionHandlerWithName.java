package net.minecraft;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;

public class DefaultUncaughtExceptionHandlerWithName implements UncaughtExceptionHandler {
    private final Logger logger;

    public DefaultUncaughtExceptionHandlerWithName(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        this.logger.error("Caught previously unhandled exception :");
        this.logger.error(t.getName(), e);
    }
}