package com.signalfx.signalflow;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SignalFlowClientTest {

    @Test
    public void shouldAutoClose() {
        StubTransport transport = new StubTransport();

        try (SignalFlowClient signalFlowClient = new SignalFlowClient(transport)) {}

        assertTrue(transport.isClosed());
    }

    private static class StubTransport implements SignalFlowTransport {

        private boolean closed = false;

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close(int code, String reason) {
            this.closed = true;
        }

        @Override
        public Channel attach(String handle, Map<String, String> parameters) {
            return null;
        }

        @Override
        public Channel execute(String program, Map<String, String> parameters) {
            return null;
        }

        @Override
        public Channel preflight(String program, Map<String, String> parameters) {
            return null;
        }

        @Override
        public void start(String program, Map<String, String> parameters) {}

        @Override
        public void stop(String handle, Map<String, String> parameters) {}

        @Override
        public void keepalive(String handle) {}
    }
}
