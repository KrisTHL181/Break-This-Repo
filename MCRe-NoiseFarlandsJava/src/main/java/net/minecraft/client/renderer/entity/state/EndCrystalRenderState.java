package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderState extends EntityRenderState {
    public boolean showsBottom = true;
    public @Nullable Vec3 beamOffset;
}