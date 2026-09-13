package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jspecify.annotations.Nullable;

public record Potion(int defaultColor) implements ItemTintSource {
   public static final MapCodec<Potion> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Potion::defaultColor)).apply(i, Potion::new)
   );

   public Potion() {
      this(-13083194);
   }

   @Override
   public int calculate(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner) {
      PotionContents contents = itemStack.get(DataComponents.POTION_CONTENTS);
      return contents != null ? ARGB.opaque(contents.getColorOr(this.defaultColor)) : ARGB.opaque(this.defaultColor);
   }

   @Override
   public MapCodec<Potion> type() {
      return MAP_CODEC;
   }
}
