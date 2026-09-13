package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OminousItemSpawnerRenderer extends EntityRenderer<OminousItemSpawner, ItemClusterRenderState> {
   private static final float ROTATION_SPEED = 40.0F;
   private static final int TICKS_SCALING = 50;
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   protected OminousItemSpawnerRenderer(EntityRendererProvider.Context p_332134_) {
      super(p_332134_);
      this.itemModelResolver = p_332134_.getItemModelResolver();
   }

   public ItemClusterRenderState createRenderState() {
      return new ItemClusterRenderState();
   }

   public void extractRenderState(OminousItemSpawner p_370185_, ItemClusterRenderState p_377250_, float p_369603_) {
      super.extractRenderState(p_370185_, p_377250_, p_369603_);
      ItemStack itemstack = p_370185_.getItem();
      p_377250_.extractItemGroupRenderState(p_370185_, itemstack, this.itemModelResolver);
   }

   public void submit(ItemClusterRenderState p_378520_, PoseStack p_330642_, SubmitNodeCollector p_427152_, CameraRenderState p_423589_) {
      if (!p_378520_.item.isEmpty()) {
         p_330642_.pushPose();
         if (p_378520_.ageInTicks <= 50.0F) {
            float f = Math.min(p_378520_.ageInTicks, 50.0F) / 50.0F;
            p_330642_.scale(f, f, f);
         }

         float f1 = Mth.wrapDegrees(p_378520_.ageInTicks * 40.0F);
         p_330642_.mulPose(Axis.YP.rotationDegrees(f1));
         ItemEntityRenderer.submitMultipleFromCount(p_330642_, p_427152_, 15728880, p_378520_, this.random);
         p_330642_.popPose();
      }
   }
}
