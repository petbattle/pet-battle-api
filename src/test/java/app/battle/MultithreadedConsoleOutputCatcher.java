package app.battle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

public class MultithreadedConsoleOutputCatcher {

    private static PrintStream originalOutStream;
    private static PrintStream originalErrStream;
    private static OutputStreamRouter routerOut;
    private static OutputStreamRouter routerErr;

    public static synchronized void startCatch() {
        if (routerOut == null) {
            originalOutStream = System.out;
            routerOut = new OutputStreamRouter(originalOutStream);
            System.setOut(new PrintStream(routerOut));
        }
        if (routerErr == null) {
            originalErrStream = System.err;
            routerErr = new OutputStreamRouter(originalErrStream);
            System.setOut(new PrintStream(routerErr));
        }
        routerOut.registerThread(Thread.currentThread());
        routerErr.registerThread(Thread.currentThread());
    }

    public static String getContent() {
        return getContent(true);
    }

    public static synchronized String getContent(boolean clear) {
        if (routerOut == null || routerErr == null) {
            throw new RuntimeException();
        }
        return routerOut.fetchThreadContent(Thread.currentThread(), clear) +
                routerErr.fetchThreadContent(Thread.currentThread(), clear);
    }

    public static synchronized void stopCatch() {
        if (routerOut == null || routerErr == null) {
            throw new RuntimeException();
        }
        routerOut.unregisterThread(Thread.currentThread());
        routerErr.unregisterThread(Thread.currentThread());
        if (routerOut.getActiveRoutes() == 0) {
            System.setOut(originalOutStream);
            originalOutStream = null;
            routerOut = null;
        }
        if (routerErr.getActiveRoutes() == 0) {
            System.setErr(originalErrStream);
            originalErrStream = null;
            routerErr = null;
        }
    }

    static class OutputStreamRouter extends OutputStream {

        private HashMap<Thread, ByteArrayOutputStream> loggerStreams = new HashMap<>();
        private OutputStream original;

        public OutputStreamRouter(OutputStream original) {
            this.original = original;
        }

        public void registerThread(Thread thread) {
            if (loggerStreams.containsKey(thread)) {
                //throw new RuntimeException();
                return;
            }
            loggerStreams.put(thread, new ByteArrayOutputStream(4096));
        }

        public void unregisterThread(Thread thread) {
            if (!loggerStreams.containsKey(thread)) {
                return;
                //throw new RuntimeException();
            }
            loggerStreams.remove(thread);
        }

        public int getActiveRoutes() {
            return loggerStreams.size();
        }

        public String fetchThreadContent(Thread thread, boolean clear) {
            if (!loggerStreams.containsKey(thread))
                throw new RuntimeException();
            String result = loggerStreams.get(thread).toString();
            if (clear) {
                loggerStreams.get(thread).reset();
            }
            return result;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            original.write(b);
            if (loggerStreams.containsKey(Thread.currentThread())) {
                loggerStreams.get(Thread.currentThread()).write(b);
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            original.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            original.close();
        }
    }

}
