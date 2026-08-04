package nl.gzmn.gZMNBuildtools.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Level;

public class WebEditorServer {

    private final JavaPlugin plugin;
    private HttpServer server;

    public WebEditorServer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        boolean enabled = plugin.getConfig().getBoolean("web-editor.enabled", true);
        if (!enabled) {
            return;
        }

        int port = plugin.getConfig().getInt("web-editor.port", 8080);
        String host = plugin.getConfig().getString("web-editor.host", "0.0.0.0");

        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", new StaticFileHandler());
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("Web Editor Server started on " + host + ":" + port);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start Web Editor Server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web Editor Server stopped.");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Prevent directory traversal
            if (path.contains("..")) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            String resourcePath = "web" + path;
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, 0);

                try (OutputStream os = exchange.getResponseBody()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error serving web file: " + path, e);
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            return "application/octet-stream";
        }
    }
}
