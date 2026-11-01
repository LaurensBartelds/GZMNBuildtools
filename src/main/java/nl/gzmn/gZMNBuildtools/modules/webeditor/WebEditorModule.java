package nl.gzmn.gZMNBuildtools.modules.webeditor;

import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.core.module.ModuleDisabledException;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SmartGradientService;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.WebEditorLink;
import nl.gzmn.gZMNBuildtools.modules.webeditor.server.WebApi;
import nl.gzmn.gZMNBuildtools.modules.webeditor.server.WebEditorServer;
import nl.gzmn.gZMNBuildtools.modules.webeditor.server.WebSessions;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The browser editor for the gradient brush. Serves the page and its API, and
 * plugs personal links into the brush's /gb web command.
 */
public final class WebEditorModule implements GzmnModule {

    private WebEditorServer server;
    private SmartGradientService brushes;

    @Override
    public String id() {
        return "web-editor";
    }

    @Override
    public String description() {
        return "Browser editor for the gradient brush with personal /gb web links.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("gradient-brush");
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        if (!context.plugin().getConfig().getBoolean("web-editor.enabled", true)) {
            throw new ModuleDisabledException("web-editor.enabled is false");
        }
        brushes = context.require(SmartGradientService.class);
        WebSessions sessions = new WebSessions(
                Math.max(1, context.plugin().getConfig().getInt("web-editor.session-minutes", 30)) * 60_000L);
        WebApi api = new WebApi(context.plugin(), context.core().messages(), brushes, context.core().presets(),
                sessions);
        server = new WebEditorServer(context.plugin(), api);
        server.start();
        if (!server.isRunning()) {
            throw new IllegalStateException("the web server could not start (is port "
                    + context.plugin().getConfig().getInt("web-editor.port", 8080) + " in use?)");
        }
        brushes.setWebEditor(new WebEditorLink() {
            @Override
            public String create(Player player) {
                return server.baseUrl() + "/?token=" + sessions.create(player.getUniqueId());
            }

            @Override
            public long lifetimeMinutes() {
                return sessions.lifetimeMinutes();
            }
        });
    }

    @Override
    public void disable() {
        if (brushes != null) {
            brushes.setWebEditor(null);
        }
        if (server != null) {
            server.stop();
        }
    }
}
