package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ItemModel {
   void update(
      ItemStackRenderState var1,
      ItemStack var2,
      ItemModelResolver var3,
      ItemDisplayContext var4,
      @Nullable ClientLevel var5,
      @Nullable ItemOwner var6,
      int var7
   );

   @OnlyIn(Dist.CLIENT)
   record BakingContext(
      ModelBaker blockModelBaker,
      EntityModelSet entityModelSet,
      MaterialSet materials,
      PlayerSkinRenderCache playerSkinRenderCache,
      ItemModel missingItemModel,
      @Nullable RegistryContextSwapper contextSwapper
   ) implements SpecialModelRenderer.BakingContext {
   }

   @OnlyIn(Dist.CLIENT)
   interface Unbaked extends ResolvableModel {
      MapCodec<? extends ItemModel.Unbaked> type();

      ItemModel bake(ItemModel.BakingContext var1);
   }
}
