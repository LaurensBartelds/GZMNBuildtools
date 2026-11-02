package nl.gzmn.gZMNBuildtools.shared.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The connected build found from a clicked block. */
public record BuildScan(LongHashSet members, Bounds bounds, Map<String, Integer> groups, boolean truncated,
                        String clickedGroup) {

    public boolean contains(int x, int y, int z) {
        return members.contains(LongHashSet.pack(x, y, z));
    }

    public int size() {
        return members.size();
    }

    /** Material groups by block count, most common first. */
    public List<Map.Entry<String, Integer>> rankedGroups() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(groups.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return list;
    }

    /** The groups that together make up at least 75% of the build (each at least 4%). */
    public Set<String> mainGroups() {
        int total = groups.values().stream().mapToInt(Integer::intValue).sum();
        Set<String> main = new LinkedHashSet<>();
        int covered = 0;
        for (Map.Entry<String, Integer> e : rankedGroups()) {
            if (covered >= total * 0.75 || e.getValue() < total * 0.04) {
                break;
            }
            main.add(e.getKey());
            covered += e.getValue();
        }
        return Collections.unmodifiableSet(main);
    }

    public String mainGroup() {
        List<Map.Entry<String, Integer>> ranked = rankedGroups();
        return ranked.isEmpty() ? clickedGroup : ranked.get(0).getKey();
    }
}
