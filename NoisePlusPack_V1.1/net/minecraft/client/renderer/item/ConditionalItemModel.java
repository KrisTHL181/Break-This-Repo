package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConditionalItemModel implements ItemModel {
   private final ItemModelPropertyTest property;
   private final ItemModel onTrue;
   private final ItemModel onFalse;

   public ConditionalItemModel(ItemModelPropertyTest p_393537_, ItemModel p_377974_, ItemModel p_378783_) {
      this.property = p_393537_;
      this.onTrue = p_377974_;
      this.onFalse = p_378783_;
   }

   @Override
   public void update(
      ItemStackRenderState p_376429_,
      ItemStack p_375999_,
      ItemModelResolver p_375782_,
      ItemDisplayContext p_376170_,
      @Nullable ClientLevel p_378740_,
      @Nullable ItemOwner p_424211_,
      int p_376970_
   ) {
      p_376429_.appendModelIdentityElement(this);
      (this.property.get(p_375999_, p_378740_, p_424211_ == null ? null : p_424211_.asLivingEntity(), p_376970_, p_376170_) ? this.onTrue : this.onFalse)
         .update(p_376429_, p_375999_, p_375782_, p_376170_, p_378740_, p_424211_, p_376970_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
      public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_375749_ -> p_375749_.group(
               ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
               ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
               ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
            )
            .apply(p_375749_, ConditionalItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<ConditionalItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_376462_) {
         return new ConditionalItemModel(
            this.adaptProperty(this.property, p_376462_.contextSwapper()), this.onTrue.bake(p_376462_), this.onFalse.bake(p_376462_)
         );
      }

      private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty p_392924_, @Nullable RegistryContextSwapper p_395371_) {
         if (p_395371_ == null) {
            return p_392924_;
         }

         CacheSlot<ClientLevel, ItemModelPropertyTest> cacheslot = new CacheSlot<>(p_389533_ -> swapContext(p_392924_, p_395371_, p_389533_));
         return (p_389526_, p_389527_, p_389528_, p_389529_, p_389530_) -> {
            ItemModelPropertyTest itemmodelpropertytest = p_389527_ == null ? p_392924_ : cacheslot.compute(p_389527_);
            return itemmodelpropertytest.get(p_389526_, p_389527_, p_389528_, p_389529_, p_389530_);
         };
      }

      private static <T extends ConditionalItemModelProperty> T swapContext(T p_395914_, RegistryContextSwapper p_391417_, ClientLevel p_394762_) {
         return (T)p_391417_.swapTo(p_395914_.type().codec(), p_395914_, p_394762_.registryAccess()).result().orElse(p_395914_);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_376408_) {
         this.onTrue.resolveDependencies(p_376408_);
         this.onFalse.resolveDependencies(p_376408_);
      }
   }
}
