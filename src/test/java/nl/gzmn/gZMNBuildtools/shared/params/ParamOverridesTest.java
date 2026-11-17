package nl.gzmn.gZMNBuildtools.shared.params;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParamOverridesTest {

    private static List<BrushParam> base() {
        return List.of(
                new BrushParam("overhang", 1, 0, 8, "eaves"),
                BrushParam.choice("style", "shape", "gable", "hip", "pyramid", "flat"));
    }

    private static List<String> load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return ParamOverrides.load(config);
    }

    @Test
    void noParamsSectionLeavesSpecsUntouched() throws Exception {
        assertTrue(load("other: 1").isEmpty());
        List<BrushParam> params = ParamOverrides.apply("roof", base());
        assertEquals(1, params.get(0).defaultValue());
        assertEquals(8, params.get(0).max());
        assertEquals(0, params.get(1).defaultValue());
    }

    @Test
    void overridesDefaultMinAndMax() throws Exception {
        List<String> problems = load("""
                params:
                  roof:
                    overhang: { default: 2, min: 0, max: 16 }
                """);
        assertTrue(problems.isEmpty());
        List<BrushParam> params = ParamOverrides.apply("roof", base());
        assertEquals(2, params.get(0).defaultValue());
        assertEquals(16, params.get(0).max());
        assertEquals(0, params.get(0).min());
        assertEquals(0, params.get(1).defaultValue(), "untouched parameter keeps its default");
    }

    @Test
    void choiceDefaultByNameAndDefaultClampedToRange() throws Exception {
        load("""
                params:
                  roof:
                    style: { default: hip }
                    overhang: { default: 99, max: 4 }
                """);
        List<BrushParam> params = ParamOverrides.apply("roof", base());
        assertEquals(1, params.get(1).defaultValue(), "hip is index 1");
        assertEquals(4, params.get(0).defaultValue(), "clamped to the overridden max");
    }

    @Test
    void groupedShapesUseModuleDotShapeKeys() throws Exception {
        load("""
                params:
                  terrain-brushes:
                    smooth:
                      kernel: { default: 3, min: 1, max: 8 }
                """);
        List<BrushParam> smooth = List.of(new BrushParam("kernel", 2, 1, 8, "size"));
        List<BrushParam> out = ParamOverrides.apply("terrain-brushes.smooth", smooth);
        assertEquals(3, out.get(0).defaultValue());
        assertEquals(8, out.get(0).max());
        assertEquals(2, ParamOverrides.apply("smooth", smooth).get(0).defaultValue(),
                "the plain id is not affected by the grouped key");
    }

    @Test
    void badNumbersAreReportedAndIgnored() throws Exception {
        List<String> problems = load("""
                params:
                  roof:
                    overhang: { max: lots }
                """);
        assertEquals(1, problems.size());
        List<BrushParam> params = ParamOverrides.apply("roof", base());
        assertEquals(8, params.get(0).max());
    }
}
