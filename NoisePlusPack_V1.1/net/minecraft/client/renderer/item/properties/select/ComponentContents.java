package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ComponentContents<T>(DataComponentType<T> componentType) implements SelectItemModelProperty<T> {
   private static final SelectItemModelProperty.Type<? extends ComponentContents<?>, ?> TYPE = createType();

   private static <T> SelectItemModelProperty.Type<ComponentContents<T>, T> createType() {
      Codec<DataComponentType<T>> codec = BuiltInRegistries.DATA_COMPONENT_TYPE
         .byNameCodec()
         .validate(p_392308_ -> p_392308_.isTransient() ? DataResult.error(() -> "Component can't be serialized") : DataResult.success(p_392308_));
      MapCodec<SelectItemModel.UnbakedSwitch<ComponentContents<T>, T>> mapcodec = codec.dispatchMap(
         "component",
         p_391524_ -> ((ComponentContents)p_391524_.property()).componentType,
         p_393420_ -> SelectItemModelProperty.Type.createCasesFieldCodec(p_393420_.codecOrThrow())
            .xmap(p_393375_ -> new SelectItemModel.UnbakedSwitch<>(new ComponentContents(p_393420_), p_393375_), SelectItemModel.UnbakedSwitch::cases)
      );
      return new SelectItemModelProperty.Type<>(mapcodec);
   }

   public static <T> SelectItemModelProperty.Type<ComponentContents<T>, T> castType() {
      return (SelectItemModelProperty.Type<ComponentContents<T>, T>)TYPE;
   }

   @Override
   public @Nullable T get(ItemStack p_393463_, @Nullable ClientLevel p_397707_, @Nullable LivingEntity p_394890_, int p_394350_, ItemDisplayContext p_393680_) {
      return p_393463_.get(this.componentType);
   }

   @Override
   public SelectItemModelProperty.Type<ComponentContents<T>, T> type() {
      return castType();
   }

   @Override
   public Codec<T> valueCodec() {
      return this.componentType.codecOrThrow();
   }
}
