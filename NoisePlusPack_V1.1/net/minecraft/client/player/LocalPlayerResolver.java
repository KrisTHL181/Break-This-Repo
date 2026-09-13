package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.players.ProfileResolver;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocalPlayerResolver implements ProfileResolver {
   private final Minecraft minecraft;
   private final ProfileResolver parentResolver;

   public LocalPlayerResolver(Minecraft p_424184_, ProfileResolver p_425106_) {
      this.minecraft = p_424184_;
      this.parentResolver = p_425106_;
   }

   @Override
   public Optional<GameProfile> fetchByName(String p_427515_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         PlayerInfo playerinfo = clientpacketlistener.getPlayerInfoIgnoreCase(p_427515_);
         if (playerinfo != null) {
            return Optional.of(playerinfo.getProfile());
         }
      }

      return this.parentResolver.fetchByName(p_427515_);
   }

   @Override
   public Optional<GameProfile> fetchById(UUID p_422672_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         PlayerInfo playerinfo = clientpacketlistener.getPlayerInfo(p_422672_);
         if (playerinfo != null) {
            return Optional.of(playerinfo.getProfile());
         }
      }

      return this.parentResolver.fetchById(p_422672_);
   }
}
