package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> {
   S createRenderState();

   default void extractRenderState(T p_426293_, S p_424933_, float p_423635_, Vec3 p_431515_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_426172_) {
      BlockEntityRenderState.extractBase(p_426293_, p_424933_, p_426172_);
   }

   void submit(S var1, PoseStack var2, SubmitNodeCollector var3, CameraRenderState var4);

   default boolean shouldRenderOffScreen() {
      return false;
   }

   default int getViewDistance() {
      return 64;
   }

   default boolean shouldRender(T p_173568_, Vec3 p_173569_) {
      return Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, this.getViewDistance());
   }
}
