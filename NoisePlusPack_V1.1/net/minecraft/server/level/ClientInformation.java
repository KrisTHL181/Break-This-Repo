package net.minecraft.server.level;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;

public record ClientInformation(
   String language,
   int viewDistance,
   ChatVisiblity chatVisibility,
   boolean chatColors,
   int modelCustomisation,
   HumanoidArm mainHand,
   boolean textFilteringEnabled,
   boolean allowsListing,
   ParticleStatus particleStatus
) {
   public static final int MAX_LANGUAGE_LENGTH = 16;

   public ClientInformation(FriendlyByteBuf p_300049_) {
      this(
         p_300049_.readUtf(16),
         p_300049_.readByte(),
         p_300049_.readEnum(ChatVisiblity.class),
         p_300049_.readBoolean(),
         p_300049_.readUnsignedByte(),
         p_300049_.readEnum(HumanoidArm.class),
         p_300049_.readBoolean(),
         p_300049_.readBoolean(),
         p_300049_.readEnum(ParticleStatus.class)
      );
   }

   public void write(FriendlyByteBuf p_297289_) {
      p_297289_.writeUtf(this.language);
      p_297289_.writeByte(this.viewDistance);
      p_297289_.writeEnum(this.chatVisibility);
      p_297289_.writeBoolean(this.chatColors);
      p_297289_.writeByte(this.modelCustomisation);
      p_297289_.writeEnum(this.mainHand);
      p_297289_.writeBoolean(this.textFilteringEnabled);
      p_297289_.writeBoolean(this.allowsListing);
      p_297289_.writeEnum(this.particleStatus);
   }

   public static ClientInformation createDefault() {
      return new ClientInformation("en_us", 2, ChatVisiblity.FULL, true, 0, Player.DEFAULT_MAIN_HAND, false, false, ParticleStatus.ALL);
   }
}
