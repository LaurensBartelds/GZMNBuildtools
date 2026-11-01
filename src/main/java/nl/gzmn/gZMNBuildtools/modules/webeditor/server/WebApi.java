package nl.gzmn.gZMNBuildtools.modules.webeditor.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.PaletteBlock;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.PaletteView;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SettingsView;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SmartGradientService;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JSON endpoints behind the web editor. Reading blocks, presets and palettes
 * is open; reading or changing a player's brush needs their session token.
 */
public final class WebApi {

    private static final int MAX_BODY = 16 * 1024;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final SmartGradientService service;
    private final PresetRegistry presets;
    private final WebSessions sessions;
    private volatile String blocksJson;

    public WebApi(JavaPlugin plugin, Messages messages, SmartGradientService service, PresetRegistry presets,
                  WebSessions sessions) {
        this.plugin = plugin;
        this.messages = messages;
        this.service = service;
        this.presets = presets;
        this.sessions = sessions;
    }

    public WebSessions sessions() {
        return sessions;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
        try {
            switch (path) {
                case "/api/blocks" -> send(exchange, 200, blocks());
                case "/api/presets" -> send(exchange, 200, presetsJson().toString());
                case "/api/palette" -> send(exchange, 200, palette(query).toString());
                case "/api/session" -> session(exchange, query);
                case "/api/apply" -> apply(exchange, query);
                default -> send(exchange, 404, error("Unknown endpoint").toString());
            }
        } catch (IllegalArgumentException e) {
            send(exchange, 400, error(e.getMessage()).toString());
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Web editor request failed: " + path, e);
            send(exchange, 500, error("Server error").toString());
        }
    }

    private String blocks() {
        String cached = blocksJson;
        if (cached == null) {
            JsonArray array = new JsonArray();
            for (PaletteBlock block : service.paletteBlocks()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", block.id());
                o.addProperty("color", hex(block.rgb()));
                o.addProperty("top", hex(block.topRgb()));
                o.addProperty("material", block.material());
                o.addProperty("stairs", block.stairs());
                o.addProperty("slab", block.slab());
                array.add(o);
            }
            cached = blocksJson = array.toString();
        }
        return cached;
    }

    private JsonArray presetsJson() {
        JsonArray array = new JsonArray();
        for (String id : presets.ids()) {
            GradientPreset preset = presets.get(id).orElseThrow();
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("name", preset.getDisplayName());
            o.addProperty("description", preset.getDescription());
            JsonArray blocks = new JsonArray();
            preset.getBlockIds().forEach(b -> blocks.add(BlockColors.normalize(b)));
            o.add("blocks", blocks);
            array.add(o);
        }
        return array;
    }

    /** Resolves a palette spec; {@code material} stands in for the build when previewing "auto". */
    private JsonObject palette(Map<String, String> query) {
        PaletteView palette = service.previewPalette(query.getOrDefault("spec", "auto"),
                parseInt(query.getOrDefault("steps", "0")), query.get("material"));
        JsonObject out = new JsonObject();
        JsonArray stops = new JsonArray();
        for (List<PaletteView.Entry> stop : palette.stops()) {
            JsonArray blocks = new JsonArray();
            for (PaletteView.Entry entry : stop) {
                JsonObject b = new JsonObject();
                b.addProperty("block", entry.block());
                b.addProperty("weight", entry.weight());
                b.addProperty("color", hex(entry.rgb()));
                blocks.add(b);
            }
            stops.add(blocks);
        }
        out.add("stops", stops);
        out.addProperty("text", palette.text());
        return out;
    }

    private void session(HttpExchange exchange, Map<String, String> query) throws IOException {
        Optional<UUID> player = sessions.resolve(query.get("token"));
        if (player.isEmpty()) {
            send(exchange, 401, error("This link has expired. Run /gb web in game for a new one.").toString());
            return;
        }
        JsonObject out = new JsonObject();
        Player online = Bukkit.getPlayer(player.get());
        out.addProperty("player", online != null ? online.getName() : player.get().toString());
        out.add("settings", settingsJson(service.settingsView(player.get())));
        out.addProperty("maxRadius", service.maxRadius());
        send(exchange, 200, out.toString());
    }

    private static JsonObject settingsJson(SettingsView s) {
        JsonObject o = new JsonObject();
        o.addProperty("palette", s.palette());
        o.addProperty("steps", s.steps());
        o.addProperty("axis", s.axis());
        o.addProperty("target", s.target());
        o.addProperty("dither", s.dither());
        o.addProperty("softness", s.softness());
        o.addProperty("shapes", s.shapes());
        o.addProperty("radius", s.radius());
        return o;
    }

    private void apply(HttpExchange exchange, Map<String, String> query) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, error("Use POST").toString());
            return;
        }
        Optional<UUID> player = sessions.resolve(query.get("token"));
        if (player.isEmpty()) {
            send(exchange, 401, error("This link has expired. Run /gb web in game for a new one.").toString());
            return;
        }
        JsonObject body = readBody(exchange);
        JsonElement palette = body.get("palette");
        Map<String, String> options = new LinkedHashMap<>();
        for (String key : service.optionNames()) {
            JsonElement value = body.get(key);
            if (value != null && !value.isJsonNull()) {
                options.put(key, value.getAsString());
            }
        }
        UUID id = player.get();
        service.applyOptions(id, palette == null || palette.isJsonNull() ? null : palette.getAsString(), options);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                return;
            }
            service.syncBrushSize(online);
            messages.success(online, "The web editor updated your gradient brush.");
            online.sendMessage(Component.text("  palette: ", NamedTextColor.GRAY)
                    .append(service.paletteSwatches(id))
                    .append(Component.text(" " + service.settingsView(id).palette(), NamedTextColor.DARK_GRAY)));
        });
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.add("settings", settingsJson(service.settingsView(id)));
        send(exchange, 200, out.toString());
    }

    private static JsonObject readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] data = in.readNBytes(MAX_BODY + 1);
            if (data.length > MAX_BODY) {
                throw new IllegalArgumentException("Request too large");
            }
            JsonElement parsed = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Expected a JSON object");
            }
            return parsed.getAsJsonObject();
        } catch (com.google.gson.JsonParseException e) {
            throw new IllegalArgumentException("Invalid JSON");
        }
    }

    static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(key, value);
        }
        return out;
    }

    private static int parseInt(String s) {
        try {
            return Math.max(0, Math.min(32, Integer.parseInt(s)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("steps must be a number");
        }
    }

    private static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static JsonObject error(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        return o;
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
