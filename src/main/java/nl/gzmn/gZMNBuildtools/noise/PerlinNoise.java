package nl.gzmn.gZMNBuildtools.noise;


public class PerlinNoise implements NoiseGenerator {

    private static final String ID = "perlin";
    private static final String DISPLAY_NAME = "Perlin";

    private long seed;
    private int[] permutation;

    private static final int[] P_ORIGINAL = {
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
            140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148,
            247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175,
            74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
            60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54,
            65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169,
            200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64,
            52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212,
            207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213,
            119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
            129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104,
            218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157,
            184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93,
            222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
    };

    public PerlinNoise() {
        this(System.currentTimeMillis());
    }

    public PerlinNoise(long seed) {
        this.seed = seed;
        this.permutation = new int[512];
        initPermutation();
    }

    private void initPermutation() {
        int[] p = new int[256];
        System.arraycopy(P_ORIGINAL, 0, p, 0, 256);

        java.util.Random rand = new java.util.Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        for (int i = 0; i < 256; i++) {
            permutation[i] = p[i];
            permutation[256 + i] = p[i];
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public double noise(double x, double y) {
        return noise(x, y, 0);
    }

    @Override
    public double noise(double x, double y, double z) {
        int X = fastFloor(x) & 255;
        int Y = fastFloor(y) & 255;
        int Z = fastFloor(z) & 255;

        x -= fastFloor(x);
        y -= fastFloor(y);
        z -= fastFloor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = permutation[X] + Y;
        int AA = permutation[A] + Z;
        int AB = permutation[A + 1] + Z;
        int B = permutation[X + 1] + Y;
        int BA = permutation[B] + Z;
        int BB = permutation[B + 1] + Z;

        return lerp(w,
                lerp(v,
                        lerp(u, grad(permutation[AA], x, y, z),
                                grad(permutation[BA], x - 1, y, z)),
                        lerp(u, grad(permutation[AB], x, y - 1, z),
                                grad(permutation[BB], x - 1, y - 1, z))),
                lerp(v,
                        lerp(u, grad(permutation[AA + 1], x, y, z - 1),
                                grad(permutation[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(permutation[AB + 1], x, y - 1, z - 1),
                                grad(permutation[BB + 1], x - 1, y - 1, z - 1))));
    }

    @Override
    public double fractalNoise(double x, double y, double z, int octaves, double persistence, double lacunarity) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total / maxValue;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
        initPermutation();
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public NoiseGenerator withSeed(long seed) {
        return new PerlinNoise(seed);
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}
