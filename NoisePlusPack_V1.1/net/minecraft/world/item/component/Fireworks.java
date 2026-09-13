package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record Fireworks(int flightDuration, List<FireworkExplosion> explosions) implements TooltipProvider {
   public static final int MAX_EXPLOSIONS = 256;
   public static final Codec<Fireworks> CODEC = RecordCodecBuilder.create(
      p_329042_ -> p_329042_.group(
            ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("flight_duration", 0).forGetter(Fireworks::flightDuration),
            FireworkExplosion.CODEC.sizeLimitedListOf(256).optionalFieldOf("explosions", List.of()).forGetter(Fireworks::explosions)
         )
         .apply(p_329042_, Fireworks::new)
   );
   public static final StreamCodec<ByteBuf, Fireworks> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, Fireworks::flightDuration, FireworkExplosion.STREAM_CODEC.apply(ByteBufCodecs.list(256)), Fireworks::explosions, Fireworks::new
   );

   public Fireworks {
      if (explosions.size() > 256) {
         throw new IllegalArgumentException("Got " + explosions.size() + " explosions, but maximum is 256");
      }
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_328344_, Consumer<Component> p_335967_, TooltipFlag p_328360_, DataComponentGetter p_396195_) {
      if (this.flightDuration > 0) {
         p_335967_.accept(
            Component.translatable("item.minecraft.firework_rocket.flight")
               .append(CommonComponents.SPACE)
               .append(String.valueOf(this.flightDuration))
               .withStyle(ChatFormatting.GRAY)
         );
      }

      FireworkExplosion fireworkexplosion = null;
      int i = 0;

      for (FireworkExplosion fireworkexplosion1 : this.explosions) {
         if (fireworkexplosion == null) {
            fireworkexplosion = fireworkexplosion1;
            i = 1;
         } else if (fireworkexplosion.equals(fireworkexplosion1)) {
            i++;
         } else {
            addExplosionTooltip(p_335967_, fireworkexplosion, i);
            fireworkexplosion = fireworkexplosion1;
            i = 1;
         }
      }

      if (fireworkexplosion != null) {
         addExplosionTooltip(p_335967_, fireworkexplosion, i);
      }
   }

   private static void addExplosionTooltip(Consumer<Component> p_395079_, FireworkExplosion p_396802_, int p_392383_) {
      Component component = p_396802_.shape().getName();
      if (p_392383_ == 1) {
         p_395079_.accept(Component.translatable("item.minecraft.firework_rocket.single_star", component).withStyle(ChatFormatting.GRAY));
      } else {
         p_395079_.accept(Component.translatable("item.minecraft.firework_rocket.multiple_stars", p_392383_, component).withStyle(ChatFormatting.GRAY));
      }

      p_396802_.addAdditionalTooltip(p_329354_ -> p_395079_.accept(Component.literal("  ").append(p_329354_)));
   }
}
