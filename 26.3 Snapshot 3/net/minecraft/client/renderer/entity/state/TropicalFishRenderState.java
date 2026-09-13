package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.fish.TropicalFish;

public class TropicalFishRenderState extends LivingEntityRenderState {
   public TropicalFish.Pattern pattern = TropicalFish.Pattern.FLOPPER;
   public int baseColor = -1;
   public int patternColor = -1;
}
