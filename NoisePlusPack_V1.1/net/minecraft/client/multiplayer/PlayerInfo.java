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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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

   public PlayerInfo(GameProfile p_253609_, boolean p_254409_) {
      this.profile = p_253609_;
      this.messageValidator = fallbackMessageValidator(p_254409_);
   }

   private static Supplier<PlayerSkin> createSkinLookup(GameProfile p_298306_) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean flag = !minecraft.isLocalPlayer(p_298306_.id());
      return minecraft.getSkinManager().createLookup(p_298306_, flag);
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

   protected void setChatSession(RemoteChatSession p_249599_) {
      this.chatSession = p_249599_;
      this.messageValidator = p_249599_.createMessageValidator(ProfilePublicKey.EXPIRY_GRACE_PERIOD);
   }

   protected void clearChatSession(boolean p_254536_) {
      this.chatSession = null;
      this.messageValidator = fallbackMessageValidator(p_254536_);
   }

   private static SignedMessageValidator fallbackMessageValidator(boolean p_254311_) {
      return p_254311_ ? SignedMessageValidator.REJECT_ALL : SignedMessageValidator.ACCEPT_UNSIGNED;
   }

   public GameType getGameMode() {
      return this.gameMode;
   }

   protected void setGameMode(GameType p_105318_) {
      this.gameMode = p_105318_;
   }

   public int getLatency() {
      return this.latency;
   }

   protected void setLatency(int p_105314_) {
      this.latency = p_105314_;
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

   public void setTabListDisplayName(@Nullable Component p_105324_) {
      this.tabListDisplayName = p_105324_;
   }

   public @Nullable Component getTabListDisplayName() {
      return this.tabListDisplayName;
   }

   public void setShowHat(boolean p_376365_) {
      this.showHat = p_376365_;
   }

   public boolean showHat() {
      return this.showHat;
   }

   public void setTabListOrder(int p_364557_) {
      this.tabListOrder = p_364557_;
   }

   public int getTabListOrder() {
      return this.tabListOrder;
   }
}
