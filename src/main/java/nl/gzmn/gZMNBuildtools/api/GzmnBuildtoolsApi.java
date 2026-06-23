package nl.gzmn.gZMNBuildtools.api;

/**
 * Public extension API for GZMNBuildtools, registered with Bukkit's
 * {@link org.bukkit.plugin.ServicesManager}. Other plugins can obtain it with:
 *
 * <pre>{@code
 * RegisteredServiceProvider<GzmnBuildtoolsApi> rsp =
 *         getServer().getServicesManager().getRegistration(GzmnBuildtoolsApi.class);
 * if (rsp != null) {
 *     GzmnBuildtoolsApi api = rsp.getProvider();
 *     api.presets().register(myPreset);
 *     api.blockFamilies().register("my_material", myDefinition);
 * }
 * }</pre>
 */
public interface GzmnBuildtoolsApi {

    Messages messages();

    PresetRegistry presets();

    BlockFamilyRegistry blockFamilies();
}
