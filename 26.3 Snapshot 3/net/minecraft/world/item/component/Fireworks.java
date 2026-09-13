package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record Fireworks(int flightDuration, List<FireworkExplosion> explosions) implements TooltipProvider {
   public static final int MAX_EXPLOSIONS = 256;
   public static final Codec<Fireworks> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("flight_duration", 0).forGetter(Fireworks::flightDuration),
            FireworkExplosion.CODEC.sizeLimitedListOf(256).optionalFieldOf("explosions", List.of()).forGetter(Fireworks::explosions)
         )
         .apply(i, Fireworks::new)
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
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      if (this.flightDuration > 0) {
         consumer.accept(Component.translatable("item.minecraft.firework_rocket.flight", this.flightDuration).withStyle(ChatFormatting.GRAY));
      }

      FireworkExplosion current = null;
      int count = 0;

      for (FireworkExplosion explosion : this.explosions) {
         if (current == null) {
            current = explosion;
            count = 1;
         } else if (current.equals(explosion)) {
            count++;
         } else {
            addExplosionTooltip(consumer, current, count);
            current = explosion;
            count = 1;
         }
      }

      if (current != null) {
         addExplosionTooltip(consumer, current, count);
      }
   }

   private static void addExplosionTooltip(final Consumer<Component> consumer, final FireworkExplosion explosion, final int count) {
      Component shapeName = explosion.shape().getName();
      if (count == 1) {
         consumer.accept(Component.translatable("item.minecraft.firework_rocket.single_star", shapeName).withStyle(ChatFormatting.GRAY));
      } else {
         consumer.accept(Component.translatable("item.minecraft.firework_rocket.multiple_stars", count, shapeName).withStyle(ChatFormatting.GRAY));
      }

      explosion.addAdditionalTooltip(component -> consumer.accept(Component.literal("  ").append(component)));
   }
}
