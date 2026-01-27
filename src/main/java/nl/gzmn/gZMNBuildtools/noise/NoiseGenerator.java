package nl.gzmn.gZMNBuildtools.noise;


public interface NoiseGenerator {


    String getId();


    String getDisplayName();


    double noise(double x, double y);


    double noise(double x, double y, double z);


    double fractalNoise(double x, double y, double z, int octaves, double persistence, double lacunarity);


    void setSeed(long seed);


    long getSeed();


    NoiseGenerator withSeed(long seed);
}
