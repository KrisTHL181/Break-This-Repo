package net.minecraft.client.renderer.item.properties.select;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SelectItemModelProperty<T> {
   @Nullable T get(ItemStack var1, @Nullable ClientLevel var2, @Nullable LivingEntity var3, int var4, ItemDisplayContext var5);

   Codec<T> valueCodec();

   SelectItemModelProperty.Type<? extends SelectItemModelProperty<T>, T> type();

   @OnlyIn(Dist.CLIENT)
   record Type<P extends SelectItemModelProperty<T>, T>(MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {
      public static <P extends SelectItemModelProperty<T>, T> SelectItemModelProperty.Type<P, T> create(MapCodec<P> p_376943_, Codec<T> p_376938_) {
         MapCodec<SelectItemModel.UnbakedSwitch<P, T>> mapcodec = RecordCodecBuilder.mapCodec(
            p_389556_ -> p_389556_.group(
                  p_376943_.forGetter(SelectItemModel.UnbakedSwitch::property),
                  createCasesFieldCodec(p_376938_).forGetter(SelectItemModel.UnbakedSwitch::cases)
               )
               .apply(p_389556_, SelectItemModel.UnbakedSwitch::new)
         );
         return new SelectItemModelProperty.Type<>(mapcodec);
      }

      public static <T> MapCodec<List<SelectItemModel.SwitchCase<T>>> createCasesFieldCodec(Codec<T> p_395409_) {
         return SelectItemModel.SwitchCase.codec(p_395409_).listOf().validate(SelectItemModelProperty.Type::validateCases).fieldOf("cases");
      }

      private static <T> DataResult<List<SelectItemModel.SwitchCase<T>>> validateCases(List<SelectItemModel.SwitchCase<T>> p_394436_) {
         if (p_394436_.isEmpty()) {
            return DataResult.error(() -> "Empty case list");
         }

         Multiset<T> multiset = HashMultiset.create();

         for (SelectItemModel.SwitchCase<T> switchcase : p_394436_) {
            multiset.addAll(switchcase.values());
         }

         return multiset.size() != multiset.entrySet().size()
            ? DataResult.error(
               () -> "Duplicate case conditions: "
                  + multiset.entrySet()
                     .stream()
                     .filter(p_378495_ -> p_378495_.getCount() > 1)
                     .map(p_378161_ -> p_378161_.getElement().toString())
                     .collect(Collectors.joining(", "))
            )
            : DataResult.success(p_394436_);
      }
   }
}
