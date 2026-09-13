package net.minecraft.client.renderer.blockentity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface BlockEntityRendererProvider<T extends BlockEntity, S extends BlockEntityRenderState> {
   BlockEntityRenderer<T, S> create(BlockEntityRendererProvider.Context context);

   record Context(
      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      BlockModelResolver blockModelResolver,
      ItemModelResolver itemModelResolver,
      EntityRenderDispatcher entityRenderer,
      EntityModelSet entityModelSet,
      Font font,
      SpriteGetter sprites,
      PlayerSkinRenderCache playerSkinRenderCache
   ) {
      public ModelPart bakeLayer(final ModelLayerLocation id) {
         return this.entityModelSet.bakeLayer(id);
      }
   }
}
