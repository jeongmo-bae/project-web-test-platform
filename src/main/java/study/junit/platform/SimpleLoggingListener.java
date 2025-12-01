// src/main/java/example/SimpleLoggingListener.java
package study.junit.platform;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;

public class SimpleLoggingListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        System.out.println("[Listener] testPlanExecutionStarted");
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        System.out.println("[Listener] testPlanExecutionFinished");
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        System.out.println("[Listener] START: " + simpleInfo(testIdentifier));
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult testExecutionResult) {
        System.out.println("[Listener] FINISH: " + simpleInfo(testIdentifier)
                + " -> " + testExecutionResult.getStatus()
                + " (" + testExecutionResult.getThrowable().orElse(null) + ")");
    }

    private String simpleInfo(TestIdentifier id) {
        return "id=" + id.getUniqueId()
                + ", displayName=" + id.getDisplayName()
                + ", type=" + id.getType();
    }
}