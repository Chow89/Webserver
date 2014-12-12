import java.util.concurrent.ConcurrentLinkedQueue;

class Utils {
    private final ConcurrentLinkedQueue<String> logs = new ConcurrentLinkedQueue<>();
    private int session = 1;

    public void addLog(String log) {
        logs.add(log);
    }

    public int getNextSession() {
        return session++;
    }

    public String getLog() {
        return logs.poll();
    }
}