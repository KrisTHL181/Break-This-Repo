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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import org.jspecify.annotations.Nullable;

public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   private @Nullable String filter;

   public SocialInteractionsPlayerList(
      final SocialInteractionsScreen socialInteractionsScreen, final Minecraft minecraft, final int width, final int height, final int y, final int itemHeight
   ) {
      super(minecraft, width, height, y, itemHeight);
      this.socialInteractionsScreen = socialInteractionsScreen;
   }

   @Override
   protected void extractListBackground(final GuiGraphicsExtractor graphics) {
   }

   @Override
   protected void extractListSeparators(final GuiGraphicsExtractor graphics) {
   }

   @Override
   protected void enableScissor(final GuiGraphicsExtractor graphics) {
      graphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
   }

   public void updatePlayerList(final Collection<UUID> playersToAdd, final double scrollAmount, final boolean addOfflineEntries) {
      Map<UUID, PlayerEntry> newEntries = new HashMap<>();
      this.addOnlinePlayers(playersToAdd, newEntries);
      if (addOfflineEntries) {
         this.addSeenPlayers(newEntries);
      }

      this.updatePlayersFromChatLog(newEntries, addOfflineEntries);
      this.updateFiltersAndScroll(newEntries.values(), scrollAmount);
   }

   private void addOnlinePlayers(final Collection<UUID> playersToAdd, final Map<UUID, PlayerEntry> output) {
      ClientPacketListener connection = this.minecraft.player.connection;

      for (UUID id : playersToAdd) {
         PlayerInfo playerInfo = connection.getPlayerInfo(id);
         if (playerInfo != null) {
            PlayerEntry player = this.makePlayerEntry(id, playerInfo);
            output.put(id, player);
         }
      }
   }

   private void addSeenPlayers(final Map<UUID, PlayerEntry> newEntries) {
      Map<UUID, PlayerInfo> seenPlayers = this.minecraft.player.connection.getSeenPlayers();

      for (Map.Entry<UUID, PlayerInfo> entry : seenPlayers.entrySet()) {
         newEntries.computeIfAbsent(entry.getKey(), uuid -> {
            PlayerEntry player = this.makePlayerEntry(uuid, entry.getValue());
            player.setRemoved(true);
            return player;
         });
      }
   }

   private PlayerEntry makePlayerEntry(final UUID id, final PlayerInfo playerInfo) {
      return new PlayerEntry(
         this.minecraft, this.socialInteractionsScreen, id, playerInfo.getProfile().name(), playerInfo::getSkin, playerInfo.hasVerifiableChat()
      );
   }

   private void updatePlayersFromChatLog(final Map<UUID, PlayerEntry> entries, final boolean addOfflineEntries) {
      Map<UUID, GameProfile> gameProfiles = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
      gameProfiles.forEach(
         (id, gameProfile) -> {
            PlayerEntry entry;
            if (addOfflineEntries) {
               entry = entries.computeIfAbsent(
                  id,
                  uuid -> {
                     PlayerEntry player = new PlayerEntry(
                        this.minecraft,
                        this.socialInteractionsScreen,
                        gameProfile.id(),
                        gameProfile.name(),
                        this.minecraft.getSkinManager().createLookup(gameProfile, true),
                        true
                     );
                     player.setRemoved(true);
                     return player;
                  }
               );
            } else {
               entry = entries.get(id);
               if (entry == null) {
                  return;
               }
            }

            entry.setHasRecentMessages(true);
         }
      );
   }

   private static Map<UUID, GameProfile> collectProfilesFromChatLog(final ChatLog chatLog) {
      Map<UUID, GameProfile> gameProfiles = new Object2ObjectLinkedOpenHashMap();

      for (int id = chatLog.end(); id >= chatLog.start(); id--) {
         if (chatLog.lookup(id) instanceof LoggedChatMessage.Player message && message.message().hasSignature()) {
            gameProfiles.put(message.profileId(), message.profile());
         }
      }

      return gameProfiles;
   }

   private void sortPlayerEntries() {
      this.players
         .sort(
            Comparator.<PlayerEntry, Integer>comparing(e -> {
                  if (this.minecraft.isLocalPlayer(e.getPlayerId())) {
                     return 0;
                  } else if (this.minecraft.getReportingContext().hasDraftReportFor(e.getPlayerId())) {
                     return 1;
                  } else if (e.getPlayerId().version() == 2) {
                     return 4;
                  } else {
                     return e.hasRecentMessages() ? 2 : 3;
                  }
               })
               .thenComparing(
                  e -> {
                     if (!e.getPlayerName().isBlank()) {
                        int firstCodepoint = e.getPlayerName().codePointAt(0);
                        if (firstCodepoint == 95
                           || firstCodepoint >= 97 && firstCodepoint <= 122
                           || firstCodepoint >= 65 && firstCodepoint <= 90
                           || firstCodepoint >= 48 && firstCodepoint <= 57) {
                           return 0;
                        }
                     }

                     return 1;
                  }
               )
               .thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase)
         );
   }

   private void updateFiltersAndScroll(final Collection<PlayerEntry> newEntries, final double scrollAmount) {
      this.players.clear();
      this.players.addAll(newEntries);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(scrollAmount);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf(p -> !p.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
         this.replaceEntries(this.players);
      }
   }

   public void setFilter(final String filter) {
      this.filter = filter;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(final PlayerInfo player, final SocialInteractionsScreen.Page page) {
      UUID playerId = player.getProfile().id();

      for (PlayerEntry playerEntry : this.players) {
         if (playerEntry.getPlayerId().equals(playerId)) {
            playerEntry.setRemoved(false);
            return;
         }
      }

      if ((page == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(playerId))
         && (Strings.isNullOrEmpty(this.filter) || player.getProfile().name().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean chatReportable = player.hasVerifiableChat();
         PlayerEntry playerEntry = new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, player.getProfile().id(), player.getProfile().name(), player::getSkin, chatReportable
         );
         this.addEntry(playerEntry);
         this.players.add(playerEntry);
      }
   }

   public void removePlayer(final UUID id) {
      for (PlayerEntry playerEntry : this.players) {
         if (playerEntry.getPlayerId().equals(id)) {
            playerEntry.setRemoved(true);
            return;
         }
      }
   }

   public void refreshHasDraftReport() {
      this.players.forEach(playerEntry -> playerEntry.refreshHasDraftReport(this.minecraft.getReportingContext()));
   }
}
