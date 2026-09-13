package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GuardianRenderState extends LivingEntityRenderState {
   public float spikesAnimation;
   public float tailAnimation;
   public Vec3 eyePosition = Vec3.ZERO;
   public @Nullable Vec3 lookDirection;
   public @Nullable Vec3 lookAtPosition;
   public @Nullable Vec3 attackTargetPosition;
   public float attackTime;
   public float attackScale;
}
