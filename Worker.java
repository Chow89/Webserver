import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

class Worker implements Runnable {
    private final Socket client;
    private final Utils utils;
    private final String filePath = new File("").getAbsolutePath();

    public Worker(Socket client, Utils utils) {
        this.client = client;
        this.utils = utils;
    }

    private static void readCookie(HashMap<String, String> header, HashMap<String, String> cookie) {
        String[] kekse = header.get("Cookie").split("; ");
        for (String c : kekse) {
            String[] keyvalue = c.split("=");
            cookie.put(keyvalue[0], keyvalue[1]);
        }
    }

    private static String getType(String extension) {
        switch (extension) {
            case "bmp":
                return "image/bmp";
            case "gif":
                return "image/gif";
            case "jpg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "htm":
            case "html":
                return "text/html";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }

    private static String getRequestMethod(String line) {
        if (line.startsWith("GET")) {
            return "GET";
        } else if (line.startsWith("HEAD")) {
            return "HEAD";
        } else {
            return "undefined";
        }
    }

    private static void readRequestHeader(BufferedReader reader, HashMap<String, String> header) throws IOException {
        for (String line = reader.readLine(); !line.isEmpty(); line = reader.readLine()) {
            String[] keyvalue = line.split(": ");
            header.put(keyvalue[0], keyvalue[1]);
        }
    }

    private static void sendHeader(OutputStream out, String reqmethod, String type, long length, int session, String history) throws IOException {
        switch (reqmethod) {
            case "GET":
                out.write("HTTP/1.1 200 OK\r\n".getBytes());
                out.write(("Content-Type: " + type + "\r\n").getBytes());
                out.write("Server: MyServer\r\n".getBytes());
                out.write(("Set-Cookie: session=" + session + "; length=4096; path=/\r\n").getBytes());
                out.write(("Set-Cookie: history=" + history + "; length=4096; path=/\r\n").getBytes());
                out.write(("Content-Length: " + length + "\r\n").getBytes());
                out.write("\r\n".getBytes());
                break;
            case "HEAD":
                out.write("HTTP/1.1 200 OK\r\n".getBytes());
                out.write("Server: MyServer\r\n".getBytes());
                out.write("\r\n".getBytes());
                break;
            default:
                out.write("HTTP/1.1 501 Not implemented\r\n".getBytes());
                out.write("Server: MyServer\r\n".getBytes());
                out.write("\r\n".getBytes());
                break;
        }
        out.flush();
    }

    private static void send400(OutputStream out) throws IOException {
        out.write("HTTP/1.1 400 Bad Request\r\n".getBytes());
        out.write("Server: MyServer\r\n".getBytes());
        out.write("\r\n".getBytes());
        out.flush();
    }

    private static void send404(OutputStream out) throws IOException {
        out.write("HTTP/1.1 404 Not found\r\n".getBytes());
        out.write("Server: MyServer\r\n".getBytes());
        out.write("\r\n".getBytes());
        out.flush();
    }

    private static void sendContent(OutputStream out, File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(bytes, 0, bytes.length);
        out.write(bytes, 0, bytes.length);
        out.flush();
        bis.close();
        fis.close();
    }

    public void run() {
        try {
            //initialisieren
            InputStream in = client.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            OutputStream out = client.getOutputStream();
            HashMap<String, String> header = new HashMap<>();
            HashMap<String, String> cookie = new HashMap<>();

            //header lesen
            String firstline = reader.readLine();
            if (firstline != null) {
                String reqmethod = getRequestMethod(firstline);
                String ressource = firstline.substring(firstline.indexOf(" ") + 1, firstline.lastIndexOf(" "));
                if (ressource.equals("/"))
                    ressource = "/index.html";

                readRequestHeader(reader, header);
                if (!header.containsKey("Host") || header.get("Host").equals("")) {
                    send400(out);
                } else {
                    int session;
                    String history;
                    if (header.containsKey("Cookie")) {
                        readCookie(header, cookie);
                        session = cookie.containsKey("session") ? Integer.parseInt(cookie.get("session")) : utils.getNextSession();
                        history = cookie.containsKey("history") ? ressource + ", " + cookie.get("history") : ressource;
                    } else {
                        session = utils.getNextSession();
                        history = ressource;
                    }

                    try {
                        File f = new File(filePath + "/documents/" + ressource);
                        sendHeader(out, reqmethod, getType(ressource.substring(ressource.lastIndexOf(".") + 1)), f.length(), session, history);
                        sendContent(out, f);
                    } catch (Exception e) {
                        send404(out);
                    }

                    if (!header.get("Connection").equals("close")) {    //keep-alive für 10 sec
                        client.setKeepAlive(true);
                        //Thread.sleep(1000 * 10);
                    }
                }
                //log anlegen
                utils.addLog(header.get("Host") + " -- " + new Date().toString() + " " + firstline + " " + header.get("User-Agent"));
            }

            //ressourcen schließen
            client.close();
            out.close();
            reader.close();
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
