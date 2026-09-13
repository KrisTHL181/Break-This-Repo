package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EndCrystalRenderState extends EntityRenderState {
   public boolean showsBottom = true;
   public @Nullable Vec3 beamOffset;
}
