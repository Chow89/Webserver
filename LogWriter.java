import java.io.File;
import java.io.FileWriter;

class LogWriter implements Runnable {
    private final Utils u;

    public LogWriter(Utils u) {
        this.u = u;
    }

    public void run() {
        while (true) {
            try {
                FileWriter f = new FileWriter(new File("access.log"), true);
                String log = u.getLog();
                if (log != null) {
                    f.write(log + "\r\n");
                    f.flush();
                } else {
                    Thread.sleep(60 * 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}