package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignedMessageValidator;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public class PlayerInfo {
   private final GameProfile profile;
   private @Nullable Supplier<PlayerSkin> skinLookup;
   private GameType gameMode = GameType.DEFAULT_MODE;
   private int latency;
   private @Nullable Component tabListDisplayName;
   private boolean showHat = true;
   private @Nullable RemoteChatSession chatSession;
   private SignedMessageValidator messageValidator;
   private int tabListOrder;

   public PlayerInfo(final GameProfile profile, final boolean enforcesSecureChat) {
      this.profile = profile;
      this.messageValidator = fallbackMessageValidator(enforcesSecureChat);
   }

   private static Supplier<PlayerSkin> createSkinLookup(final GameProfile profile) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean requireSecure = !minecraft.isLocalPlayer(profile.id());
      return minecraft.getSkinManager().createLookup(profile, requireSecure);
   }

   public GameProfile getProfile() {
      return this.profile;
   }

   public @Nullable RemoteChatSession getChatSession() {
      return this.chatSession;
   }

   public SignedMessageValidator getMessageValidator() {
      return this.messageValidator;
   }

   public boolean hasVerifiableChat() {
      return this.chatSession != null;
   }

   protected void setChatSession(final RemoteChatSession chatSession) {
      this.chatSession = chatSession;
      this.messageValidator = chatSession.createMessageValidator(ProfilePublicKey.EXPIRY_GRACE_PERIOD);
   }

   protected void clearChatSession(final boolean enforcesSecureChat) {
      this.chatSession = null;
      this.messageValidator = fallbackMessageValidator(enforcesSecureChat);
   }

   private static SignedMessageValidator fallbackMessageValidator(final boolean enforcesSecureChat) {
      return enforcesSecureChat ? SignedMessageValidator.REJECT_ALL : SignedMessageValidator.ACCEPT_UNSIGNED;
   }

   public GameType getGameMode() {
      return this.gameMode;
   }

   protected void setGameMode(final GameType gameMode) {
      this.gameMode = gameMode;
   }

   public int getLatency() {
      return this.latency;
   }

   protected void setLatency(final int latency) {
      this.latency = latency;
   }

   public PlayerSkin getSkin() {
      if (this.skinLookup == null) {
         this.skinLookup = createSkinLookup(this.profile);
      }

      return this.skinLookup.get();
   }

   public @Nullable PlayerTeam getTeam() {
      return Minecraft.getInstance().level.getScoreboard().getPlayersTeam(this.getProfile().name());
   }

   public void setTabListDisplayName(final @Nullable Component tabListDisplayName) {
      this.tabListDisplayName = tabListDisplayName;
   }

   public @Nullable Component getTabListDisplayName() {
      return this.tabListDisplayName;
   }

   public void setShowHat(final boolean showHat) {
      this.showHat = showHat;
   }

   public boolean showHat() {
      return this.showHat;
   }

   public void setTabListOrder(final int tabListOrder) {
      this.tabListOrder = tabListOrder;
   }

   public int getTabListOrder() {
      return this.tabListOrder;
   }
}
