package net.minecraft.client.renderer.block.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockDisplayContext {
    private BlockDisplayContext() {
    }

    public static BlockDisplayContext create() {
        return new BlockDisplayContext();
    }
}