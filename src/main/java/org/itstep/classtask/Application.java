package org.itstep.classtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Модифікувавти сервер, що був продемонстрований на занятті наступним чином:
 * Сервер працює в нескінченому циклі таким чином він дозволяє обробити послідовно запити браузера
 * Запити обробляються в незалежних обчислювальних потоках
 * При запиті / повертати html сторінку <h1>Home</h1>, а при запиті /about повертати сторінку <h1>About</h1>
 * При запитах інших ніж /, або /about повертати сторінку <h1>Not found</h1> з кодом 404
 */
public class Application {
    public static void main(String[] args) throws IOException {
        WebServer webServer = new WebServer(8080);
        webServer.start();
    }
}

class WebServer {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final static Logger log = LoggerFactory.getLogger(WebServer.class);

    WebServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    void start() {
        log.info("Start server...");
        while (true) {
            try {
                Socket client = serverSocket.accept();
                executorService.submit(() -> handleClient(client));
            } catch (IOException e) {
                log.error("Handle client error", e);
            }
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream writer = new PrintStream(client.getOutputStream(), true)) {
            log.info("Start handle client {}", client.getInetAddress());

            var httpRequest = parseRequest(reader);
            log.info("Request: {}", httpRequest);

            log.info("Write response");
            makeResponse(httpRequest, writer);

        } catch (IOException e) {
            log.error("Handle client error", e);
            throw new RuntimeException(e);
        }
    }

    private void makeResponse(HttpRequest request, PrintStream writer) {
        String response;
        int statusCode;
        if (HttpRequest.REQUEST_URI_HOME.equals(request.getRequestUri())) {
            response = "<h1>Home</h1>";
            statusCode = 200;
        } else if (HttpRequest.REQUEST_URI_ABOUT.equals(request.getRequestUri())) {
            response = "<h1>About</h1>";
            statusCode = 200;
        } else {
            response = "<h1>Not found</h1>";
            statusCode = 404;
        }

        String html = """
            <!doctype html>
            <html lang='en'>
                <head>
                    <title>Hello Web World!</title>
                </head>
                <body>
                    %s
                    <p>Now: %s</p>
                </body>
            </html>
            """.formatted(response, LocalDateTime.now());

        writer.printf("""
            HTTP/1.1 %d %s
            Content-type: text/html
            Content-length: %d
            Connection: close

            %s""", statusCode, getHttpStatusText(statusCode), html.length(), html);
    }

    private String getHttpStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            default -> "Unknown";
        };
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
        private final String method;
        private final String requestUri;
        private final String httpProtocol;
        private final Map<String, String> headers;

    public static final String REQUEST_URI_HOME = "/";
    public static final String REQUEST_URI_ABOUT = "/about";

    public HttpRequest(String method, String requestUri, String httpProtocol, Map<String, String> headers) {
        this.method = method;
        this.requestUri = requestUri;
        this.httpProtocol = httpProtocol;
        this.headers = headers;
    }

    public String getRequestUri() {
        return requestUri;
    }
}
