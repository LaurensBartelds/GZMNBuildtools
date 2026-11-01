package nl.gzmn.gZMNBuildtools.modules.pixelart.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Loads pictures for pixel art from the plugin's images folder or, when
 * allowed, from a web address. Web downloads only go to public addresses (no
 * localhost or private networks), never follow redirects, and are capped in
 * size; images are checked for their dimensions before they are decoded.
 */
public final class ImageLoader {

    /** Safety limits for loading pictures. */
    public record Limits(long maxBytes, int maxDimension, boolean allowUrls, boolean allowPrivateAddresses) {
    }

    private static final List<String> EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp");

    private final Path folder;
    private final Limits limits;

    public ImageLoader(Path folder, Limits limits) {
        this.folder = folder.toAbsolutePath().normalize();
        this.limits = limits;
    }

    public Path folder() {
        return folder;
    }

    /** Image files in the images folder, for tab completion. */
    public List<String> localImages() {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> EXTENSIONS.stream().anyMatch(e -> n.toLowerCase(Locale.ROOT).endsWith(e)))
                    .sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Loads "name.png" from the images folder or an http(s) address.
     *
     * @throws IllegalArgumentException for anything the player should fix
     * @throws IOException              when reading or downloading fails
     */
    public BufferedImage load(String source) throws IOException {
        String lower = source.toLowerCase(Locale.ROOT);
        byte[] data = lower.startsWith("http://") || lower.startsWith("https://") ? download(source) : local(source);
        return decode(data, limits.maxDimension());
    }

    private byte[] local(String name) throws IOException {
        Path file = folder.resolve(name).normalize();
        if (!file.startsWith(folder)) {
            throw new IllegalArgumentException("Images must be inside the images folder.");
        }
        if (EXTENSIONS.stream().noneMatch(e -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(e))) {
            throw new IllegalArgumentException("Use a .png, .jpg, .gif or .bmp image.");
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("No image called '" + name + "' in plugins/GZMNBuildtools/images.");
        }
        if (Files.size(file) > limits.maxBytes()) {
            throw new IllegalArgumentException("That image is larger than " + limits.maxBytes() / 1024 + " KB.");
        }
        return Files.readAllBytes(file);
    }

    private byte[] download(String address) throws IOException {
        if (!limits.allowUrls()) {
            throw new IllegalArgumentException("Loading images from the web is turned off; put the file in "
                    + "plugins/GZMNBuildtools/images instead.");
        }
        URI uri;
        try {
            uri = URI.create(address);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("That is not a valid web address.");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("That is not a valid web address.");
        }
        if (!limits.allowPrivateAddresses()) {
            for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
                if (!isPublic(a)) {
                    throw new IllegalArgumentException("Images can only be loaded from public addresses.");
                }
            }
        }
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "GZMNBuildtools pixel art").GET().build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
        try (InputStream body = response.body()) {
            if (response.statusCode() / 100 == 3) {
                throw new IllegalArgumentException("That address redirects; use the direct link to the image.");
            }
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("The server answered " + response.statusCode() + ".");
            }
            byte[] data = body.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, limits.maxBytes() + 1));
            if (data.length > limits.maxBytes()) {
                throw new IllegalArgumentException("That image is larger than " + limits.maxBytes() / 1024 + " KB.");
            }
            return data;
        }
    }

    /** Loopback, private, link-local, multicast and unspecified addresses are not public. */
    public static boolean isPublic(InetAddress a) {
        if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()
                || a.isMulticastAddress()) {
            return false;
        }
        byte[] b = a.getAddress();
        if (a instanceof Inet6Address) {
            // Unique local addresses fc00::/7.
            return (b[0] & 0xFE) != 0xFC;
        }
        // 100.64.0.0/10 (carrier-grade NAT) is not public either.
        return !((b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64);
    }

    /** Decodes an image after checking its dimensions, so huge images cannot exhaust memory. */
    public static BufferedImage decode(byte[] data, int maxDimension) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("That file is not an image this server can read.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > maxDimension || height > maxDimension) {
                    throw new IllegalArgumentException("That image is " + width + "x" + height
                            + "; the most this server accepts is " + maxDimension + "x" + maxDimension + ".");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /** The image scaled to {@code width} x {@code height} with smooth averaging, as ARGB pixels. */
    public static int[] scaledPixels(BufferedImage image, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        try {
            g.drawImage(image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        } finally {
            g.dispose();
        }
        return target.getRGB(0, 0, width, height, null, 0, width);
    }
}
