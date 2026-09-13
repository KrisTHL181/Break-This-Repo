package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class BundleSelectedItemSpecialRenderer implements ItemModel {
   private static final ItemModel INSTANCE = new BundleSelectedItemSpecialRenderer();

   @Override
   public void update(
      final ItemStackRenderState output,
      final ItemStack item,
      final ItemModelResolver resolver,
      final ItemDisplayContext displayContext,
      final @Nullable ClientLevel level,
      final @Nullable ItemOwner owner,
      final int seed
   ) {
      output.appendModelIdentityElement(this);
      ItemStackTemplate selectedItem = BundleItem.getSelectedItem(item);
      if (selectedItem != null) {
         resolver.appendItemLayers(output, selectedItem.create(), displayContext, level, owner, seed);
      }
   }

   public record Unbaked() implements ItemModel.Unbaked {
      public static final MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new BundleSelectedItemSpecialRenderer.Unbaked());

      @Override
      public MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation) {
         return BundleSelectedItemSpecialRenderer.INSTANCE;
      }

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
      }
   }
}
