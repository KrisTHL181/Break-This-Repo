package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public class ClientboundSetPlayerTeamPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetPlayerTeamPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetPlayerTeamPacket::write, ClientboundSetPlayerTeamPacket::new
   );
   private static final int METHOD_ADD = 0;
   private static final int METHOD_REMOVE = 1;
   private static final int METHOD_CHANGE = 2;
   private static final int METHOD_JOIN = 3;
   private static final int METHOD_LEAVE = 4;
   private static final int MAX_VISIBILITY_LENGTH = 40;
   private static final int MAX_COLLISION_LENGTH = 40;
   private final int method;
   private final String name;
   private final Collection<String> players;
   private final Optional<ClientboundSetPlayerTeamPacket.Parameters> parameters;

   private ClientboundSetPlayerTeamPacket(
      String p_179318_, int p_179319_, Optional<ClientboundSetPlayerTeamPacket.Parameters> p_179320_, Collection<String> p_179321_
   ) {
      this.name = p_179318_;
      this.method = p_179319_;
      this.parameters = p_179320_;
      this.players = ImmutableList.copyOf(p_179321_);
   }

   public static ClientboundSetPlayerTeamPacket createAddOrModifyPacket(PlayerTeam p_179333_, boolean p_179334_) {
      return new ClientboundSetPlayerTeamPacket(
         p_179333_.getName(),
         p_179334_ ? 0 : 2,
         Optional.of(new ClientboundSetPlayerTeamPacket.Parameters(p_179333_)),
         (Collection<String>)(p_179334_ ? p_179333_.getPlayers() : ImmutableList.of())
      );
   }

   public static ClientboundSetPlayerTeamPacket createRemovePacket(PlayerTeam p_179327_) {
      return new ClientboundSetPlayerTeamPacket(p_179327_.getName(), 1, Optional.empty(), ImmutableList.of());
   }

   public static ClientboundSetPlayerTeamPacket createPlayerPacket(PlayerTeam p_179329_, String p_179330_, ClientboundSetPlayerTeamPacket.Action p_179331_) {
      return new ClientboundSetPlayerTeamPacket(
         p_179329_.getName(), p_179331_ == ClientboundSetPlayerTeamPacket.Action.ADD ? 3 : 4, Optional.empty(), ImmutableList.of(p_179330_)
      );
   }

   private ClientboundSetPlayerTeamPacket(RegistryFriendlyByteBuf p_332992_) {
      this.name = p_332992_.readUtf();
      this.method = p_332992_.readByte();
      if (shouldHaveParameters(this.method)) {
         this.parameters = Optional.of(new ClientboundSetPlayerTeamPacket.Parameters(p_332992_));
      } else {
         this.parameters = Optional.empty();
      }

      if (shouldHavePlayerList(this.method)) {
         this.players = p_332992_.readList(FriendlyByteBuf::readUtf);
      } else {
         this.players = ImmutableList.of();
      }
   }

   private void write(RegistryFriendlyByteBuf p_332768_) {
      p_332768_.writeUtf(this.name);
      p_332768_.writeByte(this.method);
      if (shouldHaveParameters(this.method)) {
         this.parameters.orElseThrow(() -> new IllegalStateException("Parameters not present, but method is" + this.method)).write(p_332768_);
      }

      if (shouldHavePlayerList(this.method)) {
         p_332768_.writeCollection(this.players, FriendlyByteBuf::writeUtf);
      }
   }

   private static boolean shouldHavePlayerList(int p_179325_) {
      return p_179325_ == 0 || p_179325_ == 3 || p_179325_ == 4;
   }

   private static boolean shouldHaveParameters(int p_179337_) {
      return p_179337_ == 0 || p_179337_ == 2;
   }

   public ClientboundSetPlayerTeamPacket.@Nullable Action getPlayerAction() {
      return switch (this.method) {
         case 0, 3 -> ClientboundSetPlayerTeamPacket.Action.ADD;
         default -> null;
         case 4 -> ClientboundSetPlayerTeamPacket.Action.REMOVE;
      };
   }

   public ClientboundSetPlayerTeamPacket.@Nullable Action getTeamAction() {
      return switch (this.method) {
         case 0 -> ClientboundSetPlayerTeamPacket.Action.ADD;
         case 1 -> ClientboundSetPlayerTeamPacket.Action.REMOVE;
         default -> null;
      };
   }

   @Override
   public PacketType<ClientboundSetPlayerTeamPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_PLAYER_TEAM;
   }

   public void handle(ClientGamePacketListener p_133310_) {
      p_133310_.handleSetPlayerTeamPacket(this);
   }

   public String getName() {
      return this.name;
   }

   public Collection<String> getPlayers() {
      return this.players;
   }

   public Optional<ClientboundSetPlayerTeamPacket.Parameters> getParameters() {
      return this.parameters;
   }

   public enum Action {
      ADD,
      REMOVE;
   }

   public static class Parameters {
      private final Component displayName;
      private final Component playerPrefix;
      private final Component playerSuffix;
      private final Team.Visibility nametagVisibility;
      private final Team.CollisionRule collisionRule;
      private final ChatFormatting color;
      private final int options;

      public Parameters(PlayerTeam p_179360_) {
         this.displayName = p_179360_.getDisplayName();
         this.options = p_179360_.packOptions();
         this.nametagVisibility = p_179360_.getNameTagVisibility();
         this.collisionRule = p_179360_.getCollisionRule();
         this.color = p_179360_.getColor();
         this.playerPrefix = p_179360_.getPlayerPrefix();
         this.playerSuffix = p_179360_.getPlayerSuffix();
      }

      public Parameters(RegistryFriendlyByteBuf p_329115_) {
         this.displayName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_329115_);
         this.options = p_329115_.readByte();
         this.nametagVisibility = Team.Visibility.STREAM_CODEC.decode(p_329115_);
         this.collisionRule = Team.CollisionRule.STREAM_CODEC.decode(p_329115_);
         this.color = p_329115_.readEnum(ChatFormatting.class);
         this.playerPrefix = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_329115_);
         this.playerSuffix = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_329115_);
      }

      public Component getDisplayName() {
         return this.displayName;
      }

      public int getOptions() {
         return this.options;
      }

      public ChatFormatting getColor() {
         return this.color;
      }

      public Team.Visibility getNametagVisibility() {
         return this.nametagVisibility;
      }

      public Team.CollisionRule getCollisionRule() {
         return this.collisionRule;
      }

      public Component getPlayerPrefix() {
         return this.playerPrefix;
      }

      public Component getPlayerSuffix() {
         return this.playerSuffix;
      }

      public void write(RegistryFriendlyByteBuf p_333283_) {
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_333283_, this.displayName);
         p_333283_.writeByte(this.options);
         Team.Visibility.STREAM_CODEC.encode(p_333283_, this.nametagVisibility);
         Team.CollisionRule.STREAM_CODEC.encode(p_333283_, this.collisionRule);
         p_333283_.writeEnum(this.color);
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_333283_, this.playerPrefix);
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_333283_, this.playerSuffix);
      }
   }
}
