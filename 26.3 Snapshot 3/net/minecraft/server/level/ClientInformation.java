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

   public ClientInformation(final FriendlyByteBuf input) {
      this(
         input.readUtf(16),
         input.readByte(),
         input.readEnum(ChatVisiblity.class),
         input.readBoolean(),
         input.readUnsignedByte(),
         input.readEnum(HumanoidArm.class),
         input.readBoolean(),
         input.readBoolean(),
         input.readEnum(ParticleStatus.class)
      );
   }

   public void write(final FriendlyByteBuf output) {
      output.writeUtf(this.language);
      output.writeByte(this.viewDistance);
      output.writeEnum(this.chatVisibility);
      output.writeBoolean(this.chatColors);
      output.writeByte(this.modelCustomisation);
      output.writeEnum(this.mainHand);
      output.writeBoolean(this.textFilteringEnabled);
      output.writeBoolean(this.allowsListing);
      output.writeEnum(this.particleStatus);
   }

   public static ClientInformation createDefault() {
      return new ClientInformation("en_us", 2, ChatVisiblity.FULL, true, 0, Player.DEFAULT_MAIN_HAND, false, false, ParticleStatus.ALL);
   }
}
