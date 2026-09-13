package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ComponentMatches(DataComponentPredicate.Single<?> predicate) implements ConditionalItemModelProperty {
   public static final MapCodec<ComponentMatches> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_393387_ -> p_393387_.group(DataComponentPredicate.singleCodec("predicate").forGetter(ComponentMatches::predicate))
         .apply(p_393387_, ComponentMatches::new)
   );

   @Override
   public boolean get(ItemStack p_394936_, @Nullable ClientLevel p_391603_, @Nullable LivingEntity p_393036_, int p_397384_, ItemDisplayContext p_394587_) {
      return this.predicate.predicate().matches(p_394936_);
   }

   @Override
   public MapCodec<ComponentMatches> type() {
      return MAP_CODEC;
   }
}
