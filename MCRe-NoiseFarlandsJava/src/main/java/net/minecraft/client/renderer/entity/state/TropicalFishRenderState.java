package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TropicalFishRenderState extends LivingEntityRenderState {
    public TropicalFish.Pattern pattern = TropicalFish.Pattern.FLOPPER;
    public int baseColor = -1;
    public int patternColor = -1;
}