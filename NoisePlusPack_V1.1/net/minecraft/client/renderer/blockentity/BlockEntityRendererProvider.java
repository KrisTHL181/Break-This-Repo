package net.minecraft.client.renderer.blockentity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface BlockEntityRendererProvider<T extends BlockEntity, S extends BlockEntityRenderState> {
   BlockEntityRenderer<T, S> create(BlockEntityRendererProvider.Context var1);

   @OnlyIn(Dist.CLIENT)
   record Context(
      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      BlockRenderDispatcher blockRenderDispatcher,
      ItemModelResolver itemModelResolver,
      ItemRenderer itemRenderer,
      EntityRenderDispatcher entityRenderer,
      EntityModelSet entityModelSet,
      Font font,
      MaterialSet materials,
      PlayerSkinRenderCache playerSkinRenderCache
   ) {
      public ModelPart bakeLayer(ModelLayerLocation p_173583_) {
         return this.entityModelSet.bakeLayer(p_173583_);
      }
   }
}
