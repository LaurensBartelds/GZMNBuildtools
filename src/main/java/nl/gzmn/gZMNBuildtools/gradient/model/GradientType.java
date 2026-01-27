package nl.gzmn.gZMNBuildtools.gradient.model;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;


public interface GradientType {


    String getId();


    String getDisplayName();


    double calculatePosition(BlockVector3 position, Region region, GradientContext context);


    boolean usesNoise();


    String getDescription();
}
