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
import org.jspecify.annotations.Nullable;

public interface SelectItemModelProperty<T> {
   @Nullable T get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext);

   Codec<T> valueCodec();

   SelectItemModelProperty.Type<? extends SelectItemModelProperty<T>, T> type();

   record Type<P extends SelectItemModelProperty<T>, T>(MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {
      public static <P extends SelectItemModelProperty<T>, T> SelectItemModelProperty.Type<P, T> create(
         final MapCodec<P> propertyMapCodec, final Codec<T> valueCodec
      ) {
         MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  propertyMapCodec.forGetter(SelectItemModel.UnbakedSwitch::property),
                  createCasesFieldCodec(valueCodec).forGetter(SelectItemModel.UnbakedSwitch::cases)
               )
               .apply(i, SelectItemModel.UnbakedSwitch::new)
         );
         return new SelectItemModelProperty.Type<>(switchCodec);
      }

      public static <T> MapCodec<List<SelectItemModel.SwitchCase<T>>> createCasesFieldCodec(final Codec<T> valueCodec) {
         return SelectItemModel.SwitchCase.codec(valueCodec).listOf().validate(SelectItemModelProperty.Type::validateCases).fieldOf("cases");
      }

      private static <T> DataResult<List<SelectItemModel.SwitchCase<T>>> validateCases(final List<SelectItemModel.SwitchCase<T>> cases) {
         if (cases.isEmpty()) {
            return DataResult.error(() -> "Empty case list");
         }

         Multiset<T> counts = HashMultiset.create();

         for (SelectItemModel.SwitchCase<T> c : cases) {
            counts.addAll(c.values());
         }

         return counts.size() != counts.entrySet().size()
            ? DataResult.error(
               () -> "Duplicate case conditions: "
                  + counts.entrySet().stream().filter(e -> e.getCount() > 1).map(e -> e.getElement().toString()).collect(Collectors.joining(", "))
            )
            : DataResult.success(cases);
      }
   }
}
