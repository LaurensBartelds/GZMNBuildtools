package nl.gzmn.gZMNBuildtools.noise;


public class NoiseSettings {

    private final double scale;
    private final double strength;
    private final int octaves;
    private final double persistence;
    private final double lacunarity;
    private final long seed;
    private final NoiseAlgorithm algorithm;

    
    public enum NoiseAlgorithm {
        SIMPLEX("simplex", "Simplex"),
        PERLIN("perlin", "Perlin");

        private final String id;
        private final String displayName;

        NoiseAlgorithm(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private NoiseSettings(Builder builder) {
        this.scale = clamp(builder.scale, 0.01, 1.0);
        this.strength = clamp(builder.strength, 0.0, 1.0);
        this.octaves = Math.max(1, Math.min(8, builder.octaves));
        this.persistence = clamp(builder.persistence, 0.1, 0.9);
        this.lacunarity = clamp(builder.lacunarity, 1.5, 4.0);
        this.seed = builder.seed;
        this.algorithm = builder.algorithm;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    
    public static NoiseSettings defaults() {
        return new Builder().build();
    }

    
    public static NoiseSettings subtle() {
        return new Builder()
                .scale(0.05)
                .strength(0.15)
                .octaves(2)
                .build();
    }

    
    public static NoiseSettings strong() {
        return new Builder()
                .scale(0.15)
                .strength(0.5)
                .octaves(4)
                .build();
    }

    
    public double getScale() {
        return scale;
    }

    
    public double getStrength() {
        return strength;
    }

    
    public int getOctaves() {
        return octaves;
    }

    
    public double getPersistence() {
        return persistence;
    }

    
    public double getLacunarity() {
        return lacunarity;
    }

    
    public long getSeed() {
        return seed;
    }

    
    public NoiseAlgorithm getAlgorithm() {
        return algorithm;
    }

    
    public NoiseSettings withScale(double newScale) {
        return new Builder(this).scale(newScale).build();
    }

    
    public NoiseSettings withStrength(double newStrength) {
        return new Builder(this).strength(newStrength).build();
    }

    
    public NoiseSettings withSeed(long newSeed) {
        return new Builder(this).seed(newSeed).build();
    }

    
    public NoiseSettings withAlgorithm(NoiseAlgorithm newAlgorithm) {
        return new Builder(this).algorithm(newAlgorithm).build();
    }

    
    public static Builder builder() {
        return new Builder();
    }

    
    public static class Builder {
        private double scale = 0.1;
        private double strength = 0.3;
        private int octaves = 3;
        private double persistence = 0.5;
        private double lacunarity = 2.0;
        private long seed = System.currentTimeMillis();
        private NoiseAlgorithm algorithm = NoiseAlgorithm.SIMPLEX;

        public Builder() {}

        public Builder(NoiseSettings copy) {
            this.scale = copy.scale;
            this.strength = copy.strength;
            this.octaves = copy.octaves;
            this.persistence = copy.persistence;
            this.lacunarity = copy.lacunarity;
            this.seed = copy.seed;
            this.algorithm = copy.algorithm;
        }

        public Builder scale(double scale) {
            this.scale = scale;
            return this;
        }

        public Builder strength(double strength) {
            this.strength = strength;
            return this;
        }

        public Builder octaves(int octaves) {
            this.octaves = octaves;
            return this;
        }

        public Builder persistence(double persistence) {
            this.persistence = persistence;
            return this;
        }

        public Builder lacunarity(double lacunarity) {
            this.lacunarity = lacunarity;
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder algorithm(NoiseAlgorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public NoiseSettings build() {
            return new NoiseSettings(this);
        }
    }

    @Override
    public String toString() {
        return String.format("NoiseSettings{scale=%.2f, strength=%.2f, octaves=%d, algorithm=%s, seed=%d}",
                scale, strength, octaves, algorithm.getId(), seed);
    }
}
