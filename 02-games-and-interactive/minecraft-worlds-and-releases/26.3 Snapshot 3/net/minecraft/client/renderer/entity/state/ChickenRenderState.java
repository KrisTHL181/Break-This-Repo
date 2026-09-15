package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import org.jspecify.annotations.Nullable;

public class ChickenRenderState extends LivingEntityRenderState {
   public float flap;
   public float flapSpeed;
   public @Nullable ChickenVariant variant;
}
