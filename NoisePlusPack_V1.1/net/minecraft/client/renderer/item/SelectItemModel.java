package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SelectItemModel<T> implements ItemModel {
   private final SelectItemModelProperty<T> property;
   private final SelectItemModel.ModelSelector<T> models;

   public SelectItemModel(SelectItemModelProperty<T> p_377510_, SelectItemModel.ModelSelector<T> p_396837_) {
      this.property = p_377510_;
      this.models = p_396837_;
   }

   @Override
   public void update(
      ItemStackRenderState p_377497_,
      ItemStack p_377617_,
      ItemModelResolver p_377247_,
      ItemDisplayContext p_376949_,
      @Nullable ClientLevel p_375690_,
      @Nullable ItemOwner p_427208_,
      int p_377086_
   ) {
      p_377497_.appendModelIdentityElement(this);
      T t = this.property.get(p_377617_, p_375690_, p_427208_ == null ? null : p_427208_.asLivingEntity(), p_377086_, p_376949_);
      ItemModel itemmodel = this.models.get(t, p_375690_);
      if (itemmodel != null) {
         itemmodel.update(p_377497_, p_377617_, p_377247_, p_376949_, p_375690_, p_427208_, p_377086_);
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ModelSelector<T> {
      @Nullable ItemModel get(@Nullable T var1, @Nullable ClientLevel var2);
   }

   @OnlyIn(Dist.CLIENT)
   public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
      public static <T> Codec<SelectItemModel.SwitchCase<T>> codec(Codec<T> p_378608_) {
         return RecordCodecBuilder.create(
            p_377942_ -> p_377942_.group(
                  ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(p_378608_)).fieldOf("when").forGetter(SelectItemModel.SwitchCase::values),
                  ItemModels.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
               )
               .apply(p_377942_, SelectItemModel.SwitchCase::new)
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked {
      public static final MapCodec<SelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_375671_ -> p_375671_.group(
               SelectItemModel.UnbakedSwitch.MAP_CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
               ItemModels.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
            )
            .apply(p_375671_, SelectItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<SelectItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_377555_) {
         ItemModel itemmodel = this.fallback.<ItemModel>map(p_378040_ -> p_378040_.bake(p_377555_)).orElse(p_377555_.missingItemModel());
         return this.unbakedSwitch.bake(p_377555_, itemmodel);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_377738_) {
         this.unbakedSwitch.resolveDependencies(p_377738_);
         this.fallback.ifPresent(p_376269_ -> p_376269_.resolveDependencies(p_377738_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {
      public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> MAP_CODEC = SelectItemModelProperties.CODEC
         .dispatchMap("property", p_377103_ -> p_377103_.property().type(), SelectItemModelProperty.Type::switchCodec);

      public ItemModel bake(ItemModel.BakingContext p_378637_, ItemModel p_378738_) {
         Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap();

         for (SelectItemModel.SwitchCase<T> switchcase : this.cases) {
            ItemModel.Unbaked itemmodel$unbaked = switchcase.model;
            ItemModel itemmodel = itemmodel$unbaked.bake(p_378637_);

            for (T t : switchcase.values) {
               object2objectmap.put(t, itemmodel);
            }
         }

         object2objectmap.defaultReturnValue(p_378738_);
         return new SelectItemModel<>(this.property, this.createModelGetter(object2objectmap, p_378637_.contextSwapper()));
      }

      private SelectItemModel.ModelSelector<T> createModelGetter(Object2ObjectMap<T, ItemModel> p_393306_, @Nullable RegistryContextSwapper p_394419_) {
         if (p_394419_ == null) {
            return (p_389535_, p_389536_) -> (ItemModel)p_393306_.get(p_389535_);
         }

         ItemModel itemmodel = (ItemModel)p_393306_.defaultReturnValue();
         CacheSlot<ClientLevel, Object2ObjectMap<T, ItemModel>> cacheslot = new CacheSlot<>(
            p_389553_ -> {
               Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap(p_393306_.size());
               object2objectmap.defaultReturnValue(itemmodel);
               p_393306_.forEach(
                  (p_448362_, p_448363_) -> p_394419_.swapTo(this.property.valueCodec(), p_448362_, p_389553_.registryAccess())
                     .ifSuccess(p_389544_ -> object2objectmap.put(p_389544_, p_448363_))
               );
               return object2objectmap;
            }
         );
         return (p_389548_, p_389549_) -> {
            if (p_389549_ == null) {
               return (ItemModel)p_393306_.get(p_389548_);
            } else {
               return p_389548_ == null ? itemmodel : (ItemModel)cacheslot.compute(p_389549_).get(p_389548_);
            }
         };
      }

      public void resolveDependencies(ResolvableModel.Resolver p_378532_) {
         for (SelectItemModel.SwitchCase<?> switchcase : this.cases) {
            switchcase.model.resolveDependencies(p_378532_);
         }
      }
   }
}
