package nl.gzmn.gZMNBuildtools.modules.pixelart.engine;

import nl.gzmn.gZMNBuildtools.shared.palette.MaterialClass;

import java.util.List;
import java.util.function.Predicate;

/** Which building blocks a picture may use. */
public final class BlockSets {

    private BlockSets() {
    }

    public static Predicate<String> of(List<String> names) {
        Predicate<String> any = id -> false;
        for (String name : names) {
            any = any.or(single(name));
        }
        return any;
    }

    private static Predicate<String> single(String name) {
        return switch (name) {
            case "wool" -> id -> id.endsWith("_wool");
            case "concrete" -> id -> id.endsWith("_concrete");
            case "terracotta" -> id -> id.endsWith("terracotta");
            case "flat" -> id -> id.endsWith("_wool") || id.endsWith("_concrete") || id.endsWith("terracotta");
            case "stone" -> id -> MaterialClass.of(id) == MaterialClass.STONE;
            case "wood" -> id -> MaterialClass.of(id) == MaterialClass.WOOD;
            default -> id -> true;
        };
    }
}
