package net.minecraft.network.chat.contents.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.item.component.ResolvableProfile;

public record PlayerSprite(ResolvableProfile player, boolean hat) implements ObjectInfo {
   public static final MapCodec<PlayerSprite> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            ResolvableProfile.CODEC.fieldOf("player").forGetter(PlayerSprite::player), Codec.BOOL.optionalFieldOf("hat", true).forGetter(PlayerSprite::hat)
         )
         .apply(i, PlayerSprite::new)
   );

   @Override
   public FontDescription fontDescription() {
      return new FontDescription.PlayerSprite(this.player, this.hat);
   }

   @Override
   public String defaultFallback() {
      return this.player.name().map(name -> "[" + name + " head]").orElse("[unknown player head]");
   }

   @Override
   public MapCodec<PlayerSprite> codec() {
      return MAP_CODEC;
   }
}
