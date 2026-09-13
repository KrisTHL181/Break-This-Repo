package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_ICON_SIZE = 1024;
   public String name;
   public String ip;
   public Component status;
   public Component motd;
   public ServerStatus.@Nullable Players players;
   public long ping;
   public int protocol = SharedConstants.getCurrentVersion().protocolVersion();
   public Component version = Component.literal(SharedConstants.getCurrentVersion().name());
   public List<Component> playerList = Collections.emptyList();
   private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
   private byte @Nullable [] iconBytes;
   private ServerData.Type type;
   private int acceptedCodeOfConduct;
   private ServerData.State state = ServerData.State.INITIAL;

   public ServerData(final String name, final String ip, final ServerData.Type type) {
      this.name = name;
      this.ip = ip;
      this.type = type;
   }

   public CompoundTag write() {
      CompoundTag tag = new CompoundTag();
      tag.putString("name", this.name);
      tag.putString("ip", this.ip);
      tag.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
      tag.store(ServerData.ServerPackStatus.FIELD_CODEC, this.packStatus);
      if (this.acceptedCodeOfConduct != 0) {
         tag.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
      }

      return tag;
   }

   public ServerData.ServerPackStatus getResourcePackStatus() {
      return this.packStatus;
   }

   public void setResourcePackStatus(final ServerData.ServerPackStatus packStatus) {
      this.packStatus = packStatus;
   }

   public static ServerData read(final CompoundTag tag) {
      ServerData server = new ServerData(tag.getStringOr("name", ""), tag.getStringOr("ip", ""), ServerData.Type.OTHER);
      server.setIconBytes(tag.<byte[]>read("icon", ExtraCodecs.BASE64_STRING).orElse(null));
      server.setResourcePackStatus(tag.<ServerData.ServerPackStatus>read(ServerData.ServerPackStatus.FIELD_CODEC).orElse(ServerData.ServerPackStatus.PROMPT));
      server.acceptedCodeOfConduct = tag.getIntOr("acceptedCodeOfConduct", 0);
      return server;
   }

   public byte @Nullable [] getIconBytes() {
      return this.iconBytes;
   }

   public void setIconBytes(final byte @Nullable [] iconBytes) {
      this.iconBytes = iconBytes;
   }

   public boolean isLan() {
      return this.type == ServerData.Type.LAN;
   }

   public boolean isRealm() {
      return this.type == ServerData.Type.REALM;
   }

   public ServerData.Type type() {
      return this.type;
   }

   public boolean hasAcceptedCodeOfConduct(final String codeOfConduct) {
      return this.acceptedCodeOfConduct == codeOfConduct.hashCode();
   }

   public void acceptCodeOfConduct(final String codeOfConduct) {
      this.acceptedCodeOfConduct = codeOfConduct.hashCode();
   }

   public void clearCodeOfConduct() {
      this.acceptedCodeOfConduct = 0;
   }

   public void copyNameIconFrom(final ServerData other) {
      this.ip = other.ip;
      this.name = other.name;
      this.iconBytes = other.iconBytes;
   }

   public void copyFrom(final ServerData other) {
      this.copyNameIconFrom(other);
      this.setResourcePackStatus(other.getResourcePackStatus());
      this.type = other.type;
   }

   public ServerData.State state() {
      return this.state;
   }

   public void setState(final ServerData.State state) {
      this.state = state;
   }

   public static byte @Nullable [] validateIcon(final byte @Nullable [] bytes) {
      if (bytes != null) {
         try {
            PngInfo iconInfo = PngInfo.fromBytes(bytes);
            if (iconInfo.width() <= 1024 && iconInfo.height() <= 1024) {
               return bytes;
            }
         } catch (IOException e) {
            LOGGER.warn("Failed to decode server icon", e);
         }
      }

      return null;
   }

   public enum ServerPackStatus {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      public static final MapCodec<ServerData.ServerPackStatus> FIELD_CODEC = Codec.BOOL
         .optionalFieldOf("acceptTextures")
         .xmap(acceptTextures -> acceptTextures.<ServerData.ServerPackStatus>map(b -> b ? ENABLED : DISABLED).orElse(PROMPT), status -> {
            return switch (status) {
               case ENABLED -> Optional.of(true);
               case DISABLED -> Optional.of(false);
               case PROMPT -> Optional.empty();
            };
         });
      private final Component name;

      ServerPackStatus(final String name) {
         this.name = Component.translatable("manageServer.resourcePack." + name);
      }

      public Component getName() {
         return this.name;
      }
   }

   public enum State {
      INITIAL,
      PINGING,
      UNREACHABLE,
      INCOMPATIBLE,
      SUCCESSFUL;
   }

   public enum Type {
      LAN,
      REALM,
      OTHER;
   }
}
