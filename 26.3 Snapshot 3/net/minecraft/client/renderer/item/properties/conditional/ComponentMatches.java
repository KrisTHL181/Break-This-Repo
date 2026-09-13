package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ComponentMatches(DataComponentPredicate.Single<?> predicate) implements ConditionalItemModelProperty {
   public static final MapCodec<ComponentMatches> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(DataComponentPredicate.singleCodec("predicate").forGetter(ComponentMatches::predicate)).apply(i, ComponentMatches::new)
   );

   @Override
   public boolean get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      return this.predicate.predicate().matches(itemStack);
   }

   @Override
   public MapCodec<ComponentMatches> type() {
      return MAP_CODEC;
   }
}
