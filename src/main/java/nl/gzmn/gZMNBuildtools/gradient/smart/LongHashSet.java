package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.Arrays;
import java.util.function.LongConsumer;

/** Compact open-addressing set of longs (packed block positions). */
public final class LongHashSet {

    private static final long EMPTY = Long.MIN_VALUE;

    private long[] table;
    private int size;

    public LongHashSet(int expected) {
        int capacity = Integer.highestOneBit(Math.max(16, expected * 2) - 1) << 1;
        table = new long[capacity];
        Arrays.fill(table, EMPTY);
    }

    private static int mix(long v) {
        v ^= v >>> 33;
        v *= 0xFF51AFD7ED558CCDL;
        v ^= v >>> 33;
        return (int) v;
    }

    public boolean add(long value) {
        if (size * 2 >= table.length) {
            grow();
        }
        int mask = table.length - 1;
        int i = mix(value) & mask;
        while (table[i] != EMPTY) {
            if (table[i] == value) {
                return false;
            }
            i = (i + 1) & mask;
        }
        table[i] = value;
        size++;
        return true;
    }

    public boolean contains(long value) {
        int mask = table.length - 1;
        int i = mix(value) & mask;
        while (table[i] != EMPTY) {
            if (table[i] == value) {
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    public int size() {
        return size;
    }

    public void forEach(LongConsumer consumer) {
        for (long v : table) {
            if (v != EMPTY) {
                consumer.accept(v);
            }
        }
    }

    private void grow() {
        long[] old = table;
        table = new long[old.length * 2];
        Arrays.fill(table, EMPTY);
        size = 0;
        for (long v : old) {
            if (v != EMPTY) {
                add(v);
            }
        }
    }

    public static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }

    public static int unpackX(long key) {
        return (int) (key >> 38);
    }

    public static int unpackY(long key) {
        return (int) (key << 52 >> 52);
    }

    public static int unpackZ(long key) {
        return (int) (key << 26 >> 38);
    }
}
