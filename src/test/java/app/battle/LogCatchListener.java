package app.battle;

import io.qameta.allure.Attachment;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCatchListener implements TestExecutionListener {

    Logger log = LoggerFactory.getLogger(LogCatchListener.class);

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.getType().isContainer()) {
            log.info("::executionStarted() {}", testIdentifier.getDisplayName());
            MultithreadedConsoleOutputCatcher.startCatch();
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.getType().isTest()) {
            log.info("::executionFinished() {}", testIdentifier.getDisplayName());
            stopCatch();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @Attachment(value = "Test Log", type = "text/plain")
    public String stopCatch() {
        String result = MultithreadedConsoleOutputCatcher.getContent();
        //MultithreadedConsoleOutputCatcher.stopCatch();
        return result;
    }
}
