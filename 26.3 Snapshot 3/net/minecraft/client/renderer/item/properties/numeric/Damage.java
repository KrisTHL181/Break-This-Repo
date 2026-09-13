package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record Damage(boolean normalize) implements RangeSelectItemModelProperty {
   public static final MapCodec<Damage> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Damage::normalize)).apply(i, Damage::new)
   );

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      float damage = itemStack.getDamageValue();
      float maxDamage = itemStack.getMaxDamage();
      return this.normalize ? Mth.clamp(damage / maxDamage, 0.0F, 1.0F) : Mth.clamp(damage, 0.0F, maxDamage);
   }

   @Override
   public MapCodec<Damage> type() {
      return MAP_CODEC;
   }
}
