package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BundleSelectedItemSpecialRenderer implements ItemModel {
   static final ItemModel INSTANCE = new BundleSelectedItemSpecialRenderer();

   @Override
   public void update(
      ItemStackRenderState p_375851_,
      ItemStack p_377952_,
      ItemModelResolver p_377301_,
      ItemDisplayContext p_378331_,
      @Nullable ClientLevel p_375875_,
      @Nullable ItemOwner p_429831_,
      int p_378061_
   ) {
      p_375851_.appendModelIdentityElement(this);
      ItemStack itemstack = BundleItem.getSelectedItemStack(p_377952_);
      if (!itemstack.isEmpty()) {
         p_377301_.appendItemLayers(p_375851_, itemstack, p_378331_, p_375875_, p_429831_, p_378061_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked() implements ItemModel.Unbaked {
      public static final MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new BundleSelectedItemSpecialRenderer.Unbaked());

      @Override
      public MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_375854_) {
         return BundleSelectedItemSpecialRenderer.INSTANCE;
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_378142_) {
      }
   }
}
