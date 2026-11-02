package nl.gzmn.gZMNBuildtools.modules.scatter.prefab;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Finds prefabs by name: the built-in procedural ones, and schematics in
 * WorldEdit's schematics folder (and the player's own folder when FAWE keeps
 * one per player). A name ending in {@code /} means every schematic in that folder.
 */
public final class PrefabLibrary {

    private record Cached(long modified, SchematicPrefab prefab) {
    }

    private final Path root;
    private final Predicate<String> terrain;
    private final int maxBlocks;
    private final Set<String> extensions;
    private final Map<Path, Cached> cache = new ConcurrentHashMap<>();

    public PrefabLibrary(Path root, Predicate<String> terrain, int maxBlocks) {
        this.root = root.toAbsolutePath().normalize();
        this.terrain = terrain;
        this.maxBlocks = maxBlocks;
        this.extensions = Arrays.stream(ClipboardFormats.getFileExtensionArray())
                .map(e -> e.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    /** Folders searched for a player, most specific first. */
    private List<Path> roots(UUID player) {
        List<Path> out = new ArrayList<>();
        if (player != null) {
            Path own = root.resolve(player.toString());
            if (Files.isDirectory(own)) {
                out.add(own);
            }
        }
        out.add(root);
        return out;
    }

    /**
     * Resolves a comma-separated list such as {@code oak1,oak2,rock} or {@code trees/}.
     * Repeating a name makes it more common.
     *
     * @throws IllegalArgumentException naming what could not be found or read
     */
    public List<Prefab> resolve(UUID player, String spec) {
        List<Prefab> out = new ArrayList<>();
        for (String raw : spec.split(",")) {
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            Optional<Prefab> builtIn = ProceduralPrefab.byName(name);
            if (builtIn.isPresent()) {
                out.add(builtIn.get());
                continue;
            }
            if (name.endsWith("/") || name.endsWith("/*")) {
                String folder = name.substring(0, name.lastIndexOf('/'));
                List<Path> files = folderFiles(player, folder);
                if (files.isEmpty()) {
                    throw new IllegalArgumentException("No schematics in folder '" + folder + "'");
                }
                for (Path file : files) {
                    out.add(load(file));
                }
                continue;
            }
            out.add(load(find(player, name).orElseThrow(() -> new IllegalArgumentException(
                    "No prefab or schematic called '" + name + "'. /gzscatter list shows what there is."))));
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("Name at least one prefab, e.g. tree:oak,rock or a schematic.");
        }
        return out;
    }

    private Optional<Path> find(UUID player, String name) {
        for (Path base : roots(player)) {
            Path direct = inside(base, name);
            if (direct == null) {
                continue;
            }
            if (Files.isRegularFile(direct) && isSchematic(direct)) {
                return Optional.of(direct);
            }
            for (String ext : extensions) {
                Path withExt = inside(base, name + "." + ext);
                if (withExt != null && Files.isRegularFile(withExt)) {
                    return Optional.of(withExt);
                }
            }
        }
        return Optional.empty();
    }

    private List<Path> folderFiles(UUID player, String folder) {
        for (Path base : roots(player)) {
            Path dir = folder.isEmpty() ? base : inside(base, folder);
            if (dir != null && Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    List<Path> found = files.filter(Files::isRegularFile).filter(this::isSchematic).sorted().toList();
                    if (!found.isEmpty()) {
                        return found;
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not read folder '" + folder + "'");
                }
            }
        }
        return List.of();
    }

    /** {@code base/name}, or null if that would leave {@code base}. */
    private static Path inside(Path base, String name) {
        if (name.contains("..") || name.startsWith("/") || name.contains("\\") || name.contains(":")) {
            return null;
        }
        Path resolved = base.resolve(name).normalize();
        return resolved.startsWith(base) ? resolved : null;
    }

    private boolean isSchematic(Path file) {
        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = n.lastIndexOf('.');
        return dot > 0 && extensions.contains(n.substring(dot + 1));
    }

    private SchematicPrefab load(Path file) {
        long modified = file.toFile().lastModified();
        Cached cached = cache.get(file);
        if (cached != null && cached.modified() == modified) {
            return cached.prefab();
        }
        String name = displayName(file);
        File f = file.toFile();
        ClipboardFormat format = ClipboardFormats.findByFile(f);
        if (format == null) {
            throw new IllegalArgumentException("'" + name + "' is not a schematic WorldEdit can read");
        }
        try (InputStream in = new FileInputStream(f); ClipboardReader reader = format.getReader(in)) {
            Clipboard clipboard = reader.read();
            try {
                SchematicPrefab prefab = SchematicPrefab.of(name, clipboard, terrain, maxBlocks);
                cache.put(file, new Cached(modified, prefab));
                return prefab;
            } finally {
                clipboard.close();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read '" + name + "': " + e.getMessage());
        }
    }

    private String displayName(Path file) {
        Path base = file.startsWith(root) ? root : file.getParent();
        String rel = base.relativize(file).toString().replace(File.separatorChar, '/');
        int dot = rel.lastIndexOf('.');
        return dot > 0 ? rel.substring(0, dot) : rel;
    }

    /** Everything a player can name: built-ins, schematics and folders (ending in /). */
    public List<String> names(UUID player) {
        List<String> out = new ArrayList<>(ProceduralPrefab.names());
        for (Path base : roots(player)) {
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(base, 3)) {
                walk.filter(p -> !p.equals(base)).sorted(Comparator.naturalOrder()).forEach(p -> {
                    String rel = base.relativize(p).toString().replace(File.separatorChar, '/');
                    if (base.equals(root) && player != null && rel.startsWith(player.toString())) {
                        return;
                    }
                    if (Files.isDirectory(p)) {
                        out.add(rel + "/");
                    } else if (isSchematic(p)) {
                        int dot = rel.lastIndexOf('.');
                        out.add(rel.substring(0, dot));
                    }
                });
            } catch (IOException ignored) {
                // An unreadable folder just lists nothing.
            }
        }
        return out.stream().distinct().toList();
    }
}
