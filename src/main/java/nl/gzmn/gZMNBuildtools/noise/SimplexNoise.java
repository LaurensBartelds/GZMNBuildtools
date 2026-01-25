package nl.gzmn.gZMNBuildtools.noise;


public class SimplexNoise implements NoiseGenerator {

    private static final String ID = "simplex";
    private static final String DISPLAY_NAME = "Simplex";

    private long seed;
    private short[] perm;
    private short[] permGradIndex3D;

    private static final double STRETCH_2D = -0.211324865405187;
    private static final double SQUISH_2D = 0.366025403784439;
    private static final double STRETCH_3D = -1.0 / 6;
    private static final double SQUISH_3D = 1.0 / 3;

    private static final double NORM_2D = 47.0;
    private static final double NORM_3D = 103.0;

    private static final byte[] gradients2D = new byte[] {
            5, 2, 2, 5,
            -5, 2, -2, 5,
            5, -2, 2, -5,
            -5, -2, -2, -5,
    };

    private static final byte[] gradients3D = new byte[] {
            -11, 4, 4, -4, 11, 4, -4, 4, 11,
            11, 4, 4, 4, 11, 4, 4, 4, 11,
            -11, -4, 4, -4, -11, 4, -4, -4, 11,
            11, -4, 4, 4, -11, 4, 4, -4, 11,
            -11, 4, -4, -4, 11, -4, -4, 4, -11,
            11, 4, -4, 4, 11, -4, 4, 4, -11,
            -11, -4, -4, -4, -11, -4, -4, -4, -11,
            11, -4, -4, 4, -11, -4, 4, -4, -11,
    };

    public SimplexNoise() {
        this(System.currentTimeMillis());
    }

    public SimplexNoise(long seed) {
        this.seed = seed;
        this.perm = new short[256];
        this.permGradIndex3D = new short[256];

        short[] source = new short[256];
        for (short i = 0; i < 256; i++) {
            source[i] = i;
        }

        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;

        for (int i = 255; i >= 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((seed + 31) % (i + 1));
            if (r < 0) r += (i + 1);
            perm[i] = source[r];
            permGradIndex3D[i] = (short) ((perm[i] % (gradients3D.length / 3)) * 3);
            source[r] = source[i];
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
        double stretchOffset = (x + y) * STRETCH_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;

        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);

        double squishOffset = (xsb + ysb) * SQUISH_2D;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;

        double xins = xs - xsb;
        double yins = ys - ysb;

        double inSum = xins + yins;

        double dx0 = x - xb;
        double dy0 = y - yb;

        double value = 0;

        double dx1 = dx0 - 1 - SQUISH_2D;
        double dy1 = dy0 - 0 - SQUISH_2D;
        double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, dx1, dy1);
        }

        double dx2 = dx0 - 0 - SQUISH_2D;
        double dy2 = dy0 - 1 - SQUISH_2D;
        double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, dx2, dy2);
        }

        if (inSum <= 1) {
            double zins = 1 - inSum;
            if (zins > xins || zins > yins) {
                if (xins > yins) {
                    double dx3 = dx0 - 1 - 2 * SQUISH_2D;
                    double dy3 = dy0 + 1 - 2 * SQUISH_2D;
                    double attn3 = 2 - dx3 * dx3 - dy3 * dy3;
                    if (attn3 > 0) {
                        attn3 *= attn3;
                        value += attn3 * attn3 * extrapolate(xsb + 1, ysb - 1, dx3, dy3);
                    }
                } else {
                    double dx3 = dx0 + 1 - 2 * SQUISH_2D;
                    double dy3 = dy0 - 1 - 2 * SQUISH_2D;
                    double attn3 = 2 - dx3 * dx3 - dy3 * dy3;
                    if (attn3 > 0) {
                        attn3 *= attn3;
                        value += attn3 * attn3 * extrapolate(xsb - 1, ysb + 1, dx3, dy3);
                    }
                }
            } else {
                double dx3 = dx0 - 1 - 2 * SQUISH_2D;
                double dy3 = dy0 - 1 - 2 * SQUISH_2D;
                double attn3 = 2 - dx3 * dx3 - dy3 * dy3;
                if (attn3 > 0) {
                    attn3 *= attn3;
                    value += attn3 * attn3 * extrapolate(xsb + 1, ysb + 1, dx3, dy3);
                }
            }

            double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
            }
        } else {
            double zins = 2 - inSum;
            if (zins < xins || zins < yins) {
                if (xins > yins) {
                    double dx3 = dx0 - 2 - 2 * SQUISH_2D;
                    double dy3 = dy0 + 0 - 2 * SQUISH_2D;
                    double attn3 = 2 - dx3 * dx3 - dy3 * dy3;
                    if (attn3 > 0) {
                        attn3 *= attn3;
                        value += attn3 * attn3 * extrapolate(xsb + 2, ysb + 0, dx3, dy3);
                    }
                } else {
                    double dx3 = dx0 + 0 - 2 * SQUISH_2D;
                    double dy3 = dy0 - 2 - 2 * SQUISH_2D;
                    double attn3 = 2 - dx3 * dx3 - dy3 * dy3;
                    if (attn3 > 0) {
                        attn3 *= attn3;
                        value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 2, dx3, dy3);
                    }
                }
            } else {
                double dx3 = dx0;
                double dy3 = dy0;
                double attn3 = 2 - dx3 * dx3 - dy3 * dy3;
                if (attn3 > 0) {
                    attn3 *= attn3;
                    value += attn3 * attn3 * extrapolate(xsb, ysb, dx3, dy3);
                }
            }

            double dx4 = dx0 - 1 - 2 * SQUISH_2D;
            double dy4 = dy0 - 1 - 2 * SQUISH_2D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 1, ysb + 1, dx4, dy4);
            }
        }

        return value / NORM_2D;
    }

    @Override
    public double noise(double x, double y, double z) {
        double stretchOffset = (x + y + z) * STRETCH_3D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;

        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);

        double squishOffset = (xsb + ysb + zsb) * SQUISH_3D;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;
        double zb = zsb + squishOffset;

        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;

        double inSum = xins + yins + zins;

        double dx0 = x - xb;
        double dy0 = y - yb;
        double dz0 = z - zb;

        double value = 0;

        if (inSum <= 1) {
            byte aPoint = 0x01;
            double aScore = xins;
            byte bPoint = 0x02;
            double bScore = yins;
            if (aScore >= bScore && zins > bScore) {
                bScore = zins;
                bPoint = 0x04;
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins;
                aPoint = 0x04;
            }

            double wins = 1 - inSum;
            if (wins > aScore || wins > bScore) {
                byte c = (bScore > aScore ? bPoint : aPoint);

                if ((c & 0x01) == 0) {
                    value += contribute3D(xsb - 1, ysb, zsb, dx0 + 1, dy0, dz0);
                } else {
                    value += contribute3D(xsb + 1, ysb, zsb, dx0 - 1, dy0, dz0);
                }

                if ((c & 0x02) == 0) {
                    value += contribute3D(xsb, ysb - 1, zsb, dx0, dy0 + 1, dz0);
                } else {
                    value += contribute3D(xsb, ysb + 1, zsb, dx0, dy0 - 1, dz0);
                }

                if ((c & 0x04) == 0) {
                    value += contribute3D(xsb, ysb, zsb - 1, dx0, dy0, dz0 + 1);
                } else {
                    value += contribute3D(xsb, ysb, zsb + 1, dx0, dy0, dz0 - 1);
                }
            }

            value += contribute3D(xsb, ysb, zsb, dx0, dy0, dz0);
            value += contribute3D(xsb + 1, ysb, zsb, dx0 - 1 - SQUISH_3D, dy0 - SQUISH_3D, dz0 - SQUISH_3D);
            value += contribute3D(xsb, ysb + 1, zsb, dx0 - SQUISH_3D, dy0 - 1 - SQUISH_3D, dz0 - SQUISH_3D);
            value += contribute3D(xsb, ysb, zsb + 1, dx0 - SQUISH_3D, dy0 - SQUISH_3D, dz0 - 1 - SQUISH_3D);

        } else if (inSum >= 2) {
            byte aPoint = 0x06;
            double aScore = xins;
            byte bPoint = 0x05;
            double bScore = yins;
            if (aScore <= bScore && zins < bScore) {
                bScore = zins;
                bPoint = 0x03;
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins;
                aPoint = 0x03;
            }

            double wins = 3 - inSum;
            if (wins < aScore || wins < bScore) {
                byte c = (bScore < aScore ? bPoint : aPoint);

                if ((c & 0x01) != 0) {
                    value += contribute3D(xsb + 2, ysb + 1, zsb + 1, dx0 - 2 - 3 * SQUISH_3D, dy0 - 1 - 3 * SQUISH_3D, dz0 - 1 - 3 * SQUISH_3D);
                } else {
                    value += contribute3D(xsb, ysb + 1, zsb + 1, dx0 - 3 * SQUISH_3D, dy0 - 1 - 3 * SQUISH_3D, dz0 - 1 - 3 * SQUISH_3D);
                }

                if ((c & 0x02) != 0) {
                    value += contribute3D(xsb + 1, ysb + 2, zsb + 1, dx0 - 1 - 3 * SQUISH_3D, dy0 - 2 - 3 * SQUISH_3D, dz0 - 1 - 3 * SQUISH_3D);
                } else {
                    value += contribute3D(xsb + 1, ysb, zsb + 1, dx0 - 1 - 3 * SQUISH_3D, dy0 - 3 * SQUISH_3D, dz0 - 1 - 3 * SQUISH_3D);
                }

                if ((c & 0x04) != 0) {
                    value += contribute3D(xsb + 1, ysb + 1, zsb + 2, dx0 - 1 - 3 * SQUISH_3D, dy0 - 1 - 3 * SQUISH_3D, dz0 - 2 - 3 * SQUISH_3D);
                } else {
                    value += contribute3D(xsb + 1, ysb + 1, zsb, dx0 - 1 - 3 * SQUISH_3D, dy0 - 1 - 3 * SQUISH_3D, dz0 - 3 * SQUISH_3D);
                }
            }

            value += contribute3D(xsb + 1, ysb + 1, zsb + 1, dx0 - 1 - 3 * SQUISH_3D, dy0 - 1 - 3 * SQUISH_3D, dz0 - 1 - 3 * SQUISH_3D);
            value += contribute3D(xsb + 1, ysb + 1, zsb, dx0 - 1 - 2 * SQUISH_3D, dy0 - 1 - 2 * SQUISH_3D, dz0 - 2 * SQUISH_3D);
            value += contribute3D(xsb + 1, ysb, zsb + 1, dx0 - 1 - 2 * SQUISH_3D, dy0 - 2 * SQUISH_3D, dz0 - 1 - 2 * SQUISH_3D);
            value += contribute3D(xsb, ysb + 1, zsb + 1, dx0 - 2 * SQUISH_3D, dy0 - 1 - 2 * SQUISH_3D, dz0 - 1 - 2 * SQUISH_3D);

        } else {
            double p1 = xins + yins;
            double p2 = xins + zins;
            double p3 = yins + zins;

            if (p1 > 1) {
                double aScore = p1 - 1;
                byte aPoint = 0x03;
                double bScore = Math.min(p2, p3);
                byte bPoint = (p2 < p3) ? (byte) 0x05 : (byte) 0x06;

                if (aScore <= bScore) {
                    aScore = bScore;
                    aPoint = bPoint;
                }

                if (1 - inSum >= aScore) {
                    value += contribute3D(xsb, ysb, zsb, dx0, dy0, dz0);
                } else if (aPoint == 0x03) {
                    value += contribute3D(xsb + 1, ysb + 1, zsb, dx0 - 1 - 2 * SQUISH_3D, dy0 - 1 - 2 * SQUISH_3D, dz0 - 2 * SQUISH_3D);
                } else if (aPoint == 0x05) {
                    value += contribute3D(xsb + 1, ysb, zsb + 1, dx0 - 1 - 2 * SQUISH_3D, dy0 - 2 * SQUISH_3D, dz0 - 1 - 2 * SQUISH_3D);
                } else {
                    value += contribute3D(xsb, ysb + 1, zsb + 1, dx0 - 2 * SQUISH_3D, dy0 - 1 - 2 * SQUISH_3D, dz0 - 1 - 2 * SQUISH_3D);
                }
            } else {
                value += contribute3D(xsb, ysb, zsb, dx0, dy0, dz0);
            }

            value += contribute3D(xsb + 1, ysb, zsb, dx0 - 1 - SQUISH_3D, dy0 - SQUISH_3D, dz0 - SQUISH_3D);
            value += contribute3D(xsb, ysb + 1, zsb, dx0 - SQUISH_3D, dy0 - 1 - SQUISH_3D, dz0 - SQUISH_3D);
            value += contribute3D(xsb, ysb, zsb + 1, dx0 - SQUISH_3D, dy0 - SQUISH_3D, dz0 - 1 - SQUISH_3D);
            value += contribute3D(xsb + 1, ysb + 1, zsb, dx0 - 1 - 2 * SQUISH_3D, dy0 - 1 - 2 * SQUISH_3D, dz0 - 2 * SQUISH_3D);
            value += contribute3D(xsb + 1, ysb, zsb + 1, dx0 - 1 - 2 * SQUISH_3D, dy0 - 2 * SQUISH_3D, dz0 - 1 - 2 * SQUISH_3D);
            value += contribute3D(xsb, ysb + 1, zsb + 1, dx0 - 2 * SQUISH_3D, dy0 - 1 - 2 * SQUISH_3D, dz0 - 1 - 2 * SQUISH_3D);
        }

        return value / NORM_3D;
    }

    private double contribute3D(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
        double attn = 2 - dx * dx - dy * dy - dz * dz;
        if (attn > 0) {
            attn *= attn;
            return attn * attn * extrapolate(xsb, ysb, zsb, dx, dy, dz);
        }
        return 0;
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
        short[] source = new short[256];
        for (short i = 0; i < 256; i++) {
            source[i] = i;
        }

        long s = seed * 6364136223846793005L + 1442695040888963407L;
        s = s * 6364136223846793005L + 1442695040888963407L;
        s = s * 6364136223846793005L + 1442695040888963407L;

        for (int i = 255; i >= 0; i--) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((s + 31) % (i + 1));
            if (r < 0) r += (i + 1);
            perm[i] = source[r];
            permGradIndex3D[i] = (short) ((perm[i] % (gradients3D.length / 3)) * 3);
            source[r] = source[i];
        }
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public NoiseGenerator withSeed(long seed) {
        return new SimplexNoise(seed);
    }

    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        int index = perm[(perm[xsb & 0xFF] + ysb) & 0xFF] & 0x0E;
        return gradients2D[index] * dx + gradients2D[index + 1] * dy;
    }

    private double extrapolate(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
        int index = permGradIndex3D[(perm[(perm[xsb & 0xFF] + ysb) & 0xFF] + zsb) & 0xFF];
        return gradients3D[index] * dx + gradients3D[index + 1] * dy + gradients3D[index + 2] * dz;
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}
