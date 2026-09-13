package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public interface ItemModel {
   void update(
      ItemStackRenderState output,
      ItemStack item,
      ItemModelResolver resolver,
      ItemDisplayContext displayContext,
      @Nullable ClientLevel level,
      @Nullable ItemOwner owner,
      int seed
   );

   record BakingContext(
      ModelBaker blockModelBaker,
      EntityModelSet entityModelSet,
      SpriteGetter sprites,
      PlayerSkinRenderCache playerSkinRenderCache,
      MissingItemModel missingItemModel,
      @Nullable RegistryContextSwapper contextSwapper
   ) implements SpecialModelRenderer.BakingContext {
      public MissingItemModel missingItemModel(final Matrix4fc transformation) {
         return this.missingItemModel.withTransform(transformation);
      }
   }

   interface Unbaked extends ResolvableModel {
      MapCodec<? extends ItemModel.Unbaked> type();

      ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation);
   }
}
