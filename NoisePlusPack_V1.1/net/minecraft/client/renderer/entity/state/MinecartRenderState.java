package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MinecartRenderState extends EntityRenderState {
   public float xRot;
   public float yRot;
   public long offsetSeed;
   public int hurtDir;
   public float hurtTime;
   public float damageTime;
   public int displayOffset;
   public BlockState displayBlockState = Blocks.AIR.defaultBlockState();
   public boolean isNewRender;
   public @Nullable Vec3 renderPos;
   public @Nullable Vec3 posOnRail;
   public @Nullable Vec3 frontPos;
   public @Nullable Vec3 backPos;
}
