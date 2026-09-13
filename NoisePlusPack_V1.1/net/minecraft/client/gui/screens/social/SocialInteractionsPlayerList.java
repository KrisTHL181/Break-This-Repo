package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   private @Nullable String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen p_100697_, Minecraft p_100698_, int p_100699_, int p_100700_, int p_100701_, int p_100702_) {
      super(p_100698_, p_100699_, p_100700_, p_100701_, p_100702_);
      this.socialInteractionsScreen = p_100697_;
   }

   @Override
   protected void renderListBackground(GuiGraphics p_329536_) {
   }

   @Override
   protected void renderListSeparators(GuiGraphics p_334427_) {
   }

   @Override
   protected void enableScissor(GuiGraphics p_281892_) {
      p_281892_.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
   }

   public void updatePlayerList(Collection<UUID> p_240798_, double p_240792_, boolean p_240829_) {
      Map<UUID, PlayerEntry> map = new HashMap<>();
      this.addOnlinePlayers(p_240798_, map);
      if (p_240829_) {
         this.addSeenPlayers(map);
      }

      this.updatePlayersFromChatLog(map, p_240829_);
      this.updateFiltersAndScroll(map.values(), p_240792_);
   }

   private void addOnlinePlayers(Collection<UUID> p_240813_, Map<UUID, PlayerEntry> p_240796_) {
      ClientPacketListener clientpacketlistener = this.minecraft.player.connection;

      for (UUID uuid : p_240813_) {
         PlayerInfo playerinfo = clientpacketlistener.getPlayerInfo(uuid);
         if (playerinfo != null) {
            PlayerEntry playerentry = this.makePlayerEntry(uuid, playerinfo);
            p_240796_.put(uuid, playerentry);
         }
      }
   }

   private void addSeenPlayers(Map<UUID, PlayerEntry> p_430321_) {
      Map<UUID, PlayerInfo> map = this.minecraft.player.connection.getSeenPlayers();

      for (Map.Entry<UUID, PlayerInfo> entry : map.entrySet()) {
         p_430321_.computeIfAbsent(entry.getKey(), p_420782_ -> {
            PlayerEntry playerentry = this.makePlayerEntry(p_420782_, entry.getValue());
            playerentry.setRemoved(true);
            return playerentry;
         });
      }
   }

   private PlayerEntry makePlayerEntry(UUID p_431134_, PlayerInfo p_424270_) {
      return new PlayerEntry(
         this.minecraft, this.socialInteractionsScreen, p_431134_, p_424270_.getProfile().name(), p_424270_::getSkin, p_424270_.hasVerifiableChat()
      );
   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> p_240780_, boolean p_240827_) {
      Map<UUID, GameProfile> map = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
      map.forEach(
         (p_420779_, p_420780_) -> {
            PlayerEntry playerentry;
            if (p_240827_) {
               playerentry = p_240780_.computeIfAbsent(
                  p_420779_,
                  p_420784_ -> {
                     PlayerEntry playerentry1 = new PlayerEntry(
                        this.minecraft,
                        this.socialInteractionsScreen,
                        p_420780_.id(),
                        p_420780_.name(),
                        this.minecraft.getSkinManager().createLookup(p_420780_, true),
                        true
                     );
                     playerentry1.setRemoved(true);
                     return playerentry1;
                  }
               );
            } else {
               playerentry = p_240780_.get(p_420779_);
               if (playerentry == null) {
                  return;
               }
            }

            playerentry.setHasRecentMessages(true);
         }
      );
   }

   private static Map<UUID, GameProfile> collectProfilesFromChatLog(ChatLog p_250748_) {
      Map<UUID, GameProfile> map = new Object2ObjectLinkedOpenHashMap();

      for (int i = p_250748_.end(); i >= p_250748_.start(); i--) {
         if (p_250748_.lookup(i) instanceof LoggedChatMessage.Player loggedchatmessage$player && loggedchatmessage$player.message().hasSignature()) {
            map.put(loggedchatmessage$player.profileId(), loggedchatmessage$player.profile());
         }
      }

      return map;
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.<PlayerEntry, Integer>comparing(p_240745_ -> {
         if (this.minecraft.isLocalPlayer(p_240745_.getPlayerId())) {
            return 0;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(p_240745_.getPlayerId())) {
            return 1;
         } else {
            return p_240745_.getPlayerId().version() == 2 ? 4 : p_240745_.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing(p_240744_ -> {
         if (!p_240744_.getPlayerName().isBlank()) {
            int i = p_240744_.getPlayerName().codePointAt(0);
            if (i == 95 || i >= 97 && i <= 122 || i >= 65 && i <= 90 || i >= 48 && i <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> p_240809_, double p_240830_) {
      this.players.clear();
      this.players.addAll(p_240809_);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(p_240830_);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf(p_100710_ -> !p_100710_.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
         this.replaceEntries(this.players);
      }
   }

   public void setFilter(String p_100718_) {
      this.filter = p_100718_;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo p_100715_, SocialInteractionsScreen.Page p_100716_) {
      UUID uuid = p_100715_.getProfile().id();

      for (PlayerEntry playerentry : this.players) {
         if (playerentry.getPlayerId().equals(uuid)) {
            playerentry.setRemoved(false);
            return;
         }
      }

      if ((p_100716_ == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uuid))
         && (Strings.isNullOrEmpty(this.filter) || p_100715_.getProfile().name().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean flag = p_100715_.hasVerifiableChat();
         PlayerEntry playerentry1 = new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, p_100715_.getProfile().id(), p_100715_.getProfile().name(), p_100715_::getSkin, flag
         );
         this.addEntry(playerentry1);
         this.players.add(playerentry1);
      }
   }

   public void removePlayer(UUID p_100723_) {
      for (PlayerEntry playerentry : this.players) {
         if (playerentry.getPlayerId().equals(p_100723_)) {
            playerentry.setRemoved(true);
            return;
         }
      }
   }

   public void refreshHasDraftReport() {
      this.players.forEach(p_404873_ -> p_404873_.refreshHasDraftReport(this.minecraft.getReportingContext()));
   }
}
