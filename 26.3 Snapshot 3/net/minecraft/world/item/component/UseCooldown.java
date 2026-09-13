package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record UseCooldown(float seconds, Optional<Identifier> cooldownGroup) {
   public static final Codec<UseCooldown> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("seconds").forGetter(UseCooldown::seconds),
            Identifier.CODEC.optionalFieldOf("cooldown_group").forGetter(UseCooldown::cooldownGroup)
         )
         .apply(i, UseCooldown::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, UseCooldown> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, UseCooldown::seconds, Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), UseCooldown::cooldownGroup, UseCooldown::new
   );

   public UseCooldown(final float seconds) {
      this(seconds, Optional.empty());
   }

   public int ticks() {
      return (int)(this.seconds * 20.0F);
   }

   public void apply(final ItemStack stack, final LivingEntity user) {
      if (user instanceof Player player) {
         player.getCooldowns().addCooldown(stack, this.ticks());
      }
   }
}
