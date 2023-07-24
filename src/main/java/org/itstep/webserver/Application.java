package org.itstep.webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Application {
    public static void main(String[] args) throws IOException {
        WebServer webServer = new WebServer(8080);
        webServer.start();
    }
}

class WebServer {
    private final ServerSocket serverSocket;
    private final static Logger log = LoggerFactory.getLogger(WebServer.class);

    WebServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    void start() {
        log.info("Start server...");
        try (Socket client = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream writer = new PrintStream(client.getOutputStream(), true)) {
            log.info("Start handle client {}", client.getInetAddress());

            var httpRequest = parseRequest(reader);
            log.info("Request: {}", httpRequest);

            log.info("Write response");
            makeResponse(writer);

        } catch (IOException e) {
            log.error("Handle client error", e);
            throw new RuntimeException(e);
        }
    }

    private void makeResponse(PrintStream writer) {
        String html = """
                <!doctype html>
                <html lang='en'>
                    <head>
                        <title>Hello Web World!</title>
                    </head>
                    <body>
                        <h1>Hello World!</h1>
                        <p>Now: %s</p>
                    </body>
                </html>
                """.formatted(LocalDateTime.now());
        writer.printf("""
                HTTP/1.1 200 OK
                Content-type: text/html
                Content-length: %d
                Connection: close

                %s""", html.length(), html);
    }

    private HttpRequest parseRequest(BufferedReader reader) throws IOException {
        String line;
        StringBuilder httpRequestBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            httpRequestBuilder.append(line).append("\n");
        }

        String[] httpRequestParts = httpRequestBuilder.toString().split("(\\r)?\\n");
        String[] requestLineParts = httpRequestParts[0].split("\\s");
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < httpRequestParts.length; i++) {
            String[] headerParts = httpRequestParts[i].split(":", 2);
            headers.put(headerParts[0], headerParts[1].trim());
        }
        return new HttpRequest(requestLineParts[0], requestLineParts[1], requestLineParts[2], headers);
    }
}

class HttpRequest {
    String method;
    String requestUri;
    String httpProtocol;
    Map<String, String> headers;

    public HttpRequest(String method, String requestUri, String httpProtocol, Map<String, String> headers) {
        this.method = method;
        this.requestUri = requestUri;
        this.httpProtocol = httpProtocol;
        this.headers = headers;
    }
}