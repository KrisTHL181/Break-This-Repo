package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
   private static final int DEFAULT_MAX = 100;
   private final Identifier customId;
   private final Set<UUID> players = Sets.newHashSet();
   private int value;
   private int max = 100;
   private final Runnable dirtyCallback;

   public CustomBossEvent(final UUID id, final Identifier customId, final Component name, final Runnable dirtyCallback) {
      super(id, name, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
      this.dirtyCallback = dirtyCallback;
      this.customId = customId;
      this.setProgress(0.0F);
   }

   public Identifier customId() {
      return this.customId;
   }

   @Override
   public void addPlayer(final ServerPlayer player) {
      super.addPlayer(player);
      if (this.players.add(player.getUUID())) {
         this.setDirty();
      }
   }

   @Override
   public void removePlayer(final ServerPlayer player) {
      super.removePlayer(player);
      if (this.players.remove(player.getUUID())) {
         this.setDirty();
      }
   }

   @Override
   public void removeAllPlayers() {
      super.removeAllPlayers();
      if (!this.players.isEmpty()) {
         this.players.clear();
         this.setDirty();
      }
   }

   public int value() {
      return this.value;
   }

   public int max() {
      return this.max;
   }

   public void setValue(final int value) {
      this.value = value;
      this.setProgress(Mth.clamp((float)value / this.max, 0.0F, 1.0F));
      this.setDirty();
   }

   public void setMax(final int max) {
      this.max = max;
      this.setProgress(Mth.clamp((float)this.value / max, 0.0F, 1.0F));
      this.setDirty();
   }

   public final Component getDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.getName())
         .withStyle(
            s -> s.withColor(this.getColor().getFormatting())
               .withHoverEvent(new HoverEvent.ShowText(Component.literal(this.customId().toString())))
               .withInsertion(this.customId().toString())
         );
   }

   public boolean setPlayers(final Collection<ServerPlayer> players) {
      Set<UUID> toRemove = Sets.newHashSet();
      Set<ServerPlayer> toAdd = Sets.newHashSet();

      for (UUID uuid : this.players) {
         boolean found = false;

         for (ServerPlayer player : players) {
            if (player.getUUID().equals(uuid)) {
               found = true;
               break;
            }
         }

         if (!found) {
            toRemove.add(uuid);
         }
      }

      for (ServerPlayer player : players) {
         boolean found = false;

         for (UUID uuid : this.players) {
            if (player.getUUID().equals(uuid)) {
               found = true;
               break;
            }
         }

         if (!found) {
            toAdd.add(player);
         }
      }

      for (UUID uuid : toRemove) {
         for (ServerPlayer player : this.getPlayers()) {
            if (player.getUUID().equals(uuid)) {
               this.removePlayer(player);
               break;
            }
         }

         this.players.remove(uuid);
      }

      for (ServerPlayer player : toAdd) {
         this.addPlayer(player);
      }

      boolean playersChanged = !toRemove.isEmpty() || !toAdd.isEmpty();
      if (playersChanged) {
         this.setDirty();
      }

      return playersChanged;
   }

   public static CustomBossEvent load(final UUID id, final Identifier customId, final CustomBossEvent.Packed packed, final Runnable setDirty) {
      CustomBossEvent event = new CustomBossEvent(id, customId, packed.name, setDirty);
      event.setVisible(packed.visible);
      event.setValue(packed.value);
      event.setMax(packed.max);
      event.setColor(packed.color);
      event.setOverlay(packed.overlay);
      event.setDarkenScreen(packed.darkenScreen);
      event.setPlayBossMusic(packed.playBossMusic);
      event.setCreateWorldFog(packed.createWorldFog);
      event.players.addAll(packed.players);
      return event;
   }

   public CustomBossEvent.Packed pack() {
      return new CustomBossEvent.Packed(
         this.getName(),
         this.isVisible(),
         this.value(),
         this.max(),
         this.getColor(),
         this.getOverlay(),
         this.shouldDarkenScreen(),
         this.shouldPlayBossMusic(),
         this.shouldCreateWorldFog(),
         Set.copyOf(this.players)
      );
   }

   public void onPlayerConnect(final ServerPlayer player) {
      if (this.players.contains(player.getUUID())) {
         this.addPlayer(player);
      }
   }

   public void onPlayerDisconnect(final ServerPlayer player) {
      super.removePlayer(player);
   }

   @Override
   public void setDirty() {
      this.dirtyCallback.run();
   }

   public record Packed(
      Component name,
      boolean visible,
      int value,
      int max,
      BossEvent.BossBarColor color,
      BossEvent.BossBarOverlay overlay,
      boolean darkenScreen,
      boolean playBossMusic,
      boolean createWorldFog,
      Set<UUID> players
   ) {
      public static final Codec<CustomBossEvent.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ComponentSerialization.CODEC.fieldOf("Name").forGetter(CustomBossEvent.Packed::name),
               Codec.BOOL.optionalFieldOf("Visible", false).forGetter(CustomBossEvent.Packed::visible),
               Codec.INT.optionalFieldOf("Value", 0).forGetter(CustomBossEvent.Packed::value),
               Codec.INT.optionalFieldOf("Max", 100).forGetter(CustomBossEvent.Packed::max),
               BossEvent.BossBarColor.CODEC.optionalFieldOf("Color", BossEvent.BossBarColor.WHITE).forGetter(CustomBossEvent.Packed::color),
               BossEvent.BossBarOverlay.CODEC.optionalFieldOf("Overlay", BossEvent.BossBarOverlay.PROGRESS).forGetter(CustomBossEvent.Packed::overlay),
               Codec.BOOL.optionalFieldOf("DarkenScreen", false).forGetter(CustomBossEvent.Packed::darkenScreen),
               Codec.BOOL.optionalFieldOf("PlayBossMusic", false).forGetter(CustomBossEvent.Packed::playBossMusic),
               Codec.BOOL.optionalFieldOf("CreateWorldFog", false).forGetter(CustomBossEvent.Packed::createWorldFog),
               UUIDUtil.CODEC_SET.optionalFieldOf("Players", Set.of()).forGetter(CustomBossEvent.Packed::players)
            )
            .apply(i, CustomBossEvent.Packed::new)
      );
   }
}
