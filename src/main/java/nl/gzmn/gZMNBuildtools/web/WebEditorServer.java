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
    private final WebApi api;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public WebEditorServer(JavaPlugin plugin, WebApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    public boolean isRunning() {
        return server != null;
    }

    /** The address players open, from web-editor.public-url or the server's own address. */
    public String baseUrl() {
        String configured = plugin.getConfig().getString("web-editor.public-url", "");
        if (configured != null && !configured.isBlank()) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        String ip = plugin.getServer().getIp();
        String host = ip == null || ip.isBlank() || ip.equals("0.0.0.0") ? "localhost" : ip;
        return "http://" + host + ":" + plugin.getConfig().getInt("web-editor.port", 8080);
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
            server.createContext("/api/", api::handle);
            server.createContext("/", new StaticFileHandler());
            executor = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "GZMN-WebEditor");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);
            server.start();
            plugin.getLogger().info("Web Editor Server started on " + host + ":" + port);
        } catch (IOException e) {
            server = null;
            plugin.getLogger().log(Level.SEVERE, "Failed to start Web Editor Server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            plugin.getLogger().info("Web Editor Server stopped.");
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
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
