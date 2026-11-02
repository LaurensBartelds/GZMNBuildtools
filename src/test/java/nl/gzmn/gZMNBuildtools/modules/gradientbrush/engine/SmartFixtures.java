package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.palette.PaletteGenerator;

import java.util.List;
import java.util.Optional;

import static nl.gzmn.gZMNBuildtools.shared.build.BuildFixtures.*;

/** Palette context for gradient tests; the test world lives in {@link BuildFixtures}. */
final class SmartFixtures {

    static final PaletteSpec.Context CONTEXT = context(null);

    static PaletteSpec.Context context(String material) {
        return new PaletteSpec.Context() {
            public PaletteGenerator generator() {
                return GENERATOR;
            }

            public Optional<List<String>> preset(String id) {
                return id.equals("greys") ? Optional.of(List.of("white_concrete", "black_concrete")) : Optional.empty();
            }

            public String buildMaterial() {
                return material;
            }
        };
    }
}
