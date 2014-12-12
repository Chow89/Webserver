import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Server {
    public static void main(String[] args) throws IOException {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            port = 80;
        }
        System.out.println("Server started on port " + port);
        Utils u = new Utils();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.execute(new LogWriter(u));
        ServerSocket socket = new ServerSocket(port);
        while (true) {
            executor.execute(new Worker(socket.accept(), u));
        }
    }
}