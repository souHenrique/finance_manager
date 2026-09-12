package com.amorim.finance_manager.testsupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender;
    private final Level previousLevel;

    private LogCapture(Class<?> source, Level level) {
        logger = (Logger) LoggerFactory.getLogger(source);
        previousLevel = logger.getLevel();

        if (level != null) {
            logger.setLevel(level);
        }

        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    public static LogCapture forClass(Class<?> source) {
        return new LogCapture(source, null);
    }

    public static LogCapture forClassAtLevel(Class<?> source, Level level) {
        return new LogCapture(source, level);
    }

    public List<String> messages() {
        return appender.list
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        appender.stop();
    }
}
