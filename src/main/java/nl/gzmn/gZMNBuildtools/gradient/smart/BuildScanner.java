package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.HashMap;
import java.util.Map;

/** Flood-fills the build connected to a clicked block (26-neighbour connectivity). */
public final class BuildScanner {

    private BuildScanner() {
    }

    public static BuildScan scan(BlockSource source, int x, int y, int z, TargetPolicy policy, BlockRules rules,
                                 int maxBlocks, int maxRadius) {
        String start = source.blockAt(x, y, z);
        String clickedGroup = start == null ? null : rules.group(start);
        LongHashSet members = new LongHashSet(Math.min(maxBlocks, 4096));
        Map<String, Integer> groups = new HashMap<>();
        if (start == null || !policy.member(start)) {
            return new BuildScan(members, Bounds.of(x, y, z), groups, false, clickedGroup);
        }

        long[] queue = new long[Math.min(maxBlocks, 1 << 16)];
        int head = 0;
        int tail = 0;
        int queued = 0;
        Bounds bounds = Bounds.of(x, y, z);
        boolean truncated = false;
        long startKey = LongHashSet.pack(x, y, z);
        members.add(startKey);
        groups.merge(clickedGroup, 1, Integer::sum);
        queue[tail++] = startKey;
        queued++;
        int minY = source.minY();
        int maxY = source.maxY();

        while (queued > 0) {
            long key = queue[head];
            head = (head + 1) % queue.length;
            queued--;
            int cx = LongHashSet.unpackX(key);
            int cy = LongHashSet.unpackY(key);
            int cz = LongHashSet.unpackZ(key);
            bounds = bounds.include(cx, cy, cz);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = cy + dy;
                    if (ny < minY || ny > maxY) {
                        continue;
                    }
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        int nx = cx + dx;
                        int nz = cz + dz;
                        if (Math.abs(nx - x) > maxRadius || Math.abs(ny - y) > maxRadius
                                || Math.abs(nz - z) > maxRadius) {
                            truncated = true;
                            continue;
                        }
                        long next = LongHashSet.pack(nx, ny, nz);
                        if (members.contains(next)) {
                            continue;
                        }
                        String id = source.blockAt(nx, ny, nz);
                        if (!policy.member(id)) {
                            continue;
                        }
                        if (members.size() >= maxBlocks) {
                            truncated = true;
                            continue;
                        }
                        members.add(next);
                        groups.merge(rules.group(id), 1, Integer::sum);
                        if (queued == queue.length) {
                            queue = grow(queue, head, queued);
                            head = 0;
                            tail = queued;
                        }
                        queue[tail] = next;
                        tail = (tail + 1) % queue.length;
                        queued++;
                    }
                }
            }
        }
        return new BuildScan(members, bounds, groups, truncated, clickedGroup);
    }

    private static long[] grow(long[] queue, int head, int count) {
        long[] bigger = new long[queue.length * 2];
        for (int i = 0; i < count; i++) {
            bigger[i] = queue[(head + i) % queue.length];
        }
        return bigger;
    }
}
