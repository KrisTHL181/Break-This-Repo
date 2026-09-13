package net.minecraft.client.model.geom.builders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MaterialDefinition {
    final int xTexSize;
    final int yTexSize;

    public MaterialDefinition(final int xTexSize, final int yTexSize) {
        this.xTexSize = xTexSize;
        this.yTexSize = yTexSize;
    }
}