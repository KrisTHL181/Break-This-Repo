package net.minecraft.server.level;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;

public class ServerBossEvent extends BossEvent {
   private final Set<ServerPlayer> players = Sets.newHashSet();
   private final Set<ServerPlayer> unmodifiablePlayers = Collections.unmodifiableSet(this.players);
   private boolean visible = true;

   public ServerBossEvent(final UUID id, final Component name, final BossEvent.BossBarColor color, final BossEvent.BossBarOverlay overlay) {
      super(id, name, color, overlay);
   }

   @Override
   public void setProgress(final float progress) {
      if (progress != this.progress) {
         super.setProgress(progress);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdateProgressPacket);
      }
   }

   @Override
   public void setColor(final BossEvent.BossBarColor color) {
      if (color != this.color) {
         super.setColor(color);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdateStylePacket);
      }
   }

   @Override
   public void setOverlay(final BossEvent.BossBarOverlay overlay) {
      if (overlay != this.overlay) {
         super.setOverlay(overlay);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdateStylePacket);
      }
   }

   @Override
   public BossEvent setDarkenScreen(final boolean darkenScreen) {
      if (darkenScreen != this.darkenScreen) {
         super.setDarkenScreen(darkenScreen);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdatePropertiesPacket);
      }

      return this;
   }

   @Override
   public BossEvent setPlayBossMusic(final boolean playBossMusic) {
      if (playBossMusic != this.playBossMusic) {
         super.setPlayBossMusic(playBossMusic);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdatePropertiesPacket);
      }

      return this;
   }

   @Override
   public BossEvent setCreateWorldFog(final boolean createWorldFog) {
      if (createWorldFog != this.createWorldFog) {
         super.setCreateWorldFog(createWorldFog);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdatePropertiesPacket);
      }

      return this;
   }

   @Override
   public void setName(final Component name) {
      if (!Objects.equal(name, this.name)) {
         super.setName(name);
         this.setDirty();
         this.broadcast(ClientboundBossEventPacket::createUpdateNamePacket);
      }
   }

   private void broadcast(final Function<BossEvent, ClientboundBossEventPacket> factory) {
      if (this.visible) {
         ClientboundBossEventPacket packet = factory.apply(this);

         for (ServerPlayer player : this.players) {
            player.connection.send(packet);
         }
      }
   }

   public void addPlayer(final ServerPlayer player) {
      if (this.players.add(player) && this.visible) {
         player.connection.send(ClientboundBossEventPacket.createAddPacket(this));
      }
   }

   public void removePlayer(final ServerPlayer player) {
      if (this.players.remove(player) && this.visible) {
         player.connection.send(ClientboundBossEventPacket.createRemovePacket(this.getId()));
      }
   }

   public void removeAllPlayers() {
      if (!this.players.isEmpty()) {
         for (ServerPlayer player : Lists.newArrayList(this.players)) {
            this.removePlayer(player);
         }
      }
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(final boolean visible) {
      if (visible != this.visible) {
         this.visible = visible;
         this.setDirty();

         for (ServerPlayer player : this.players) {
            player.connection.send(visible ? ClientboundBossEventPacket.createAddPacket(this) : ClientboundBossEventPacket.createRemovePacket(this.getId()));
         }
      }
   }

   public Collection<ServerPlayer> getPlayers() {
      return this.unmodifiablePlayers;
   }

   protected void setDirty() {
   }
}
