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
      ClientAsset.Texture p_428656_, ClientAsset.@Nullable Texture p_423598_, ClientAsset.@Nullable Texture p_430792_, PlayerModelType p_425395_
   ) {
      return new PlayerSkin(p_428656_, p_423598_, p_430792_, p_425395_, false);
   }

   public PlayerSkin with(PlayerSkin.Patch p_425362_) {
      return p_425362_.equals(PlayerSkin.Patch.EMPTY)
         ? this
         : insecure(
            (ClientAsset.Texture)DataFixUtils.orElse(p_425362_.body, this.body),
            (ClientAsset.Texture)DataFixUtils.orElse(p_425362_.cape, this.cape),
            (ClientAsset.Texture)DataFixUtils.orElse(p_425362_.elytra, this.elytra),
            p_425362_.model.orElse(this.model)
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
         p_430599_ -> p_430599_.group(
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("texture").forGetter(PlayerSkin.Patch::body),
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("cape").forGetter(PlayerSkin.Patch::cape),
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("elytra").forGetter(PlayerSkin.Patch::elytra),
               PlayerModelType.CODEC.optionalFieldOf("model").forGetter(PlayerSkin.Patch::model)
            )
            .apply(p_430599_, PlayerSkin.Patch::create)
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
         Optional<ClientAsset.ResourceTexture> p_425356_,
         Optional<ClientAsset.ResourceTexture> p_426994_,
         Optional<ClientAsset.ResourceTexture> p_422308_,
         Optional<PlayerModelType> p_427416_
      ) {
         return p_425356_.isEmpty() && p_426994_.isEmpty() && p_422308_.isEmpty() && p_427416_.isEmpty()
            ? EMPTY
            : new PlayerSkin.Patch(p_425356_, p_426994_, p_422308_, p_427416_);
      }
   }
}
