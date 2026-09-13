package net.minecraft.client.renderer.item;

import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemModelResolver {
   private final Function<Identifier, ItemModel> modelGetter;
   private final Function<Identifier, ClientItem.Properties> clientProperties;

   public ItemModelResolver(ModelManager p_377509_) {
      this.modelGetter = p_377509_::getItemModel;
      this.clientProperties = p_377509_::getItemProperties;
   }

   public void updateForLiving(ItemStackRenderState p_375741_, ItemStack p_375493_, ItemDisplayContext p_375922_, LivingEntity p_376619_) {
      this.updateForTopItem(p_375741_, p_375493_, p_375922_, p_376619_.level(), p_376619_, p_376619_.getId() + p_375922_.ordinal());
   }

   public void updateForNonLiving(ItemStackRenderState p_375853_, ItemStack p_378808_, ItemDisplayContext p_378112_, Entity p_376596_) {
      this.updateForTopItem(p_375853_, p_378808_, p_378112_, p_376596_.level(), null, p_376596_.getId());
   }

   public void updateForTopItem(
      ItemStackRenderState p_376095_,
      ItemStack p_376083_,
      ItemDisplayContext p_378127_,
      @Nullable Level p_378324_,
      @Nullable ItemOwner p_425076_,
      int p_377306_
   ) {
      p_376095_.clear();
      if (!p_376083_.isEmpty()) {
         p_376095_.displayContext = p_378127_;
         this.appendItemLayers(p_376095_, p_376083_, p_378127_, p_378324_, p_425076_, p_377306_);
      }
   }

   public void appendItemLayers(
      ItemStackRenderState p_376475_,
      ItemStack p_375988_,
      ItemDisplayContext p_377575_,
      @Nullable Level p_376809_,
      @Nullable ItemOwner p_424315_,
      int p_377982_
   ) {
      Identifier identifier = p_375988_.get(DataComponents.ITEM_MODEL);
      if (identifier != null) {
         p_376475_.setOversizedInGui(this.clientProperties.apply(identifier).oversizedInGui());
         this.modelGetter
            .apply(identifier)
            .update(p_376475_, p_375988_, this, p_377575_, p_376809_ instanceof ClientLevel clientlevel ? clientlevel : null, p_424315_, p_377982_);
      }
   }

   public boolean shouldPlaySwapAnimation(ItemStack p_375787_) {
      Identifier identifier = p_375787_.get(DataComponents.ITEM_MODEL);
      return identifier == null ? true : this.clientProperties.apply(identifier).handAnimationOnSwap();
   }

   public float swapAnimationScale(ItemStack p_451230_) {
      Identifier identifier = p_451230_.get(DataComponents.ITEM_MODEL);
      return identifier == null ? 1.0F : this.clientProperties.apply(identifier).swapAnimationScale();
   }
}
