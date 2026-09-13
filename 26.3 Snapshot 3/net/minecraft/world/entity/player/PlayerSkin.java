package net.minecraft.world.entity.player;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public record PlayerSkin(
   ClientAsset.Texture body, ClientAsset.@Nullable Texture cape, ClientAsset.@Nullable Texture elytra, PlayerModelType model, boolean secure
) {
   public static PlayerSkin insecure(
      final ClientAsset.Texture body, final ClientAsset.@Nullable Texture cape, final ClientAsset.@Nullable Texture elytra, final PlayerModelType model
   ) {
      return new PlayerSkin(body, cape, elytra, model, false);
   }

   public PlayerSkin with(final PlayerSkin.Patch patch) {
      return patch.equals(PlayerSkin.Patch.EMPTY)
         ? this
         : insecure(
            (ClientAsset.Texture)DataFixUtils.orElse(patch.body, this.body),
            (ClientAsset.Texture)DataFixUtils.orElse(patch.cape, this.cape),
            (ClientAsset.Texture)DataFixUtils.orElse(patch.elytra, this.elytra),
            patch.model.orElse(this.model)
         );
   }

   public record Patch(
      Optional<ClientAsset.ResourceTexture> body,
      Optional<ClientAsset.ResourceTexture> cape,
      Optional<ClientAsset.ResourceTexture> elytra,
      Optional<PlayerModelType> model
   ) {
      public static final PlayerSkin.Patch EMPTY = new PlayerSkin.Patch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
      public static final MapCodec<PlayerSkin.Patch> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("texture").forGetter(PlayerSkin.Patch::body),
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("cape").forGetter(PlayerSkin.Patch::cape),
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("elytra").forGetter(PlayerSkin.Patch::elytra),
               PlayerModelType.CODEC.optionalFieldOf("model").forGetter(PlayerSkin.Patch::model)
            )
            .apply(i, PlayerSkin.Patch::create)
      );
      public static final StreamCodec<ByteBuf, PlayerSkin.Patch> STREAM_CODEC = StreamCodec.composite(
         ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::body,
         ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::cape,
         ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::elytra,
         PlayerModelType.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::model,
         PlayerSkin.Patch::create
      );

      public static PlayerSkin.Patch create(
         final Optional<ClientAsset.ResourceTexture> texture,
         final Optional<ClientAsset.ResourceTexture> capeTexture,
         final Optional<ClientAsset.ResourceTexture> elytraTexture,
         final Optional<PlayerModelType> model
      ) {
         return texture.isEmpty() && capeTexture.isEmpty() && elytraTexture.isEmpty() && model.isEmpty()
            ? EMPTY
            : new PlayerSkin.Patch(texture, capeTexture, elytraTexture, model);
      }
   }
}
