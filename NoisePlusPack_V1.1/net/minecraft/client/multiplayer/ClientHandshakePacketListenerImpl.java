package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.ForcedUsernameChangeException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Crypt;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientHandshakePacketListenerImpl implements ClientLoginPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final @Nullable ServerData serverData;
   private final @Nullable Screen parent;
   private final Consumer<Component> updateStatus;
   private final Connection connection;
   private final boolean newWorld;
   private final @Nullable Duration worldLoadDuration;
   private @Nullable String minigameName;
   private final LevelLoadTracker levelLoadTracker;
   private final Map<Identifier, byte[]> cookies;
   private final boolean wasTransferredTo;
   private final Map<UUID, PlayerInfo> seenPlayers;
   private final boolean seenInsecureChatWarning;
   private final AtomicReference<ClientHandshakePacketListenerImpl.State> state = new AtomicReference<>(ClientHandshakePacketListenerImpl.State.CONNECTING);

   public ClientHandshakePacketListenerImpl(
      Connection p_261697_,
      Minecraft p_261835_,
      @Nullable ServerData p_261938_,
      @Nullable Screen p_261783_,
      boolean p_261562_,
      @Nullable Duration p_261673_,
      Consumer<Component> p_261945_,
      LevelLoadTracker p_425358_,
      @Nullable TransferState p_332707_
   ) {
      this.connection = p_261697_;
      this.minecraft = p_261835_;
      this.serverData = p_261938_;
      this.parent = p_261783_;
      this.updateStatus = p_261945_;
      this.newWorld = p_261562_;
      this.worldLoadDuration = p_261673_;
      this.levelLoadTracker = p_425358_;
      this.cookies = p_332707_ != null ? new HashMap<>(p_332707_.cookies()) : new HashMap<>();
      this.seenPlayers = p_332707_ != null ? p_332707_.seenPlayers() : Map.of();
      this.seenInsecureChatWarning = p_332707_ != null ? p_332707_.seenInsecureChatWarning() : false;
      this.wasTransferredTo = p_332707_ != null;
   }

   private void switchState(ClientHandshakePacketListenerImpl.State p_301608_) {
      ClientHandshakePacketListenerImpl.State clienthandshakepacketlistenerimpl$state = this.state.updateAndGet(p_325472_ -> {
         if (!p_301608_.fromStates.contains(p_325472_)) {
            throw new IllegalStateException("Tried to switch to " + p_301608_ + " from " + p_325472_ + ", but expected one of " + p_301608_.fromStates);
         } else {
            return p_301608_;
         }
      });
      this.updateStatus.accept(clienthandshakepacketlistenerimpl$state.message);
   }

   @Override
   public void handleHello(ClientboundHelloPacket p_104549_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.AUTHORIZING);

      Cipher cipher;
      Cipher cipher1;
      String s;
      ServerboundKeyPacket serverboundkeypacket;
      try {
         SecretKey secretkey = Crypt.generateSecretKey();
         PublicKey publickey = p_104549_.getPublicKey();
         s = new BigInteger(Crypt.digestData(p_104549_.getServerId(), publickey, secretkey)).toString(16);
         cipher = Crypt.getCipher(2, secretkey);
         cipher1 = Crypt.getCipher(1, secretkey);
         byte[] abyte = p_104549_.getChallenge();
         serverboundkeypacket = new ServerboundKeyPacket(secretkey, publickey, abyte);
      } catch (Exception exception) {
         throw new IllegalStateException("Protocol error", exception);
      }

      if (p_104549_.shouldAuthenticate()) {
         Util.ioPool().execute(() -> {
            Component component = this.authenticateServer(s);
            if (component != null) {
               if (this.serverData == null || !this.serverData.isLan()) {
                  this.connection.disconnect(component);
                  return;
               }

               LOGGER.warn(component.getString());
            }

            this.setEncryption(serverboundkeypacket, cipher, cipher1);
         });
      } else {
         this.setEncryption(serverboundkeypacket, cipher, cipher1);
      }
   }

   private void setEncryption(ServerboundKeyPacket p_333847_, Cipher p_327699_, Cipher p_330168_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.ENCRYPTING);
      this.connection.send(p_333847_, PacketSendListener.thenRun(() -> this.connection.setEncryptionKey(p_327699_, p_330168_)));
   }

   private @Nullable Component authenticateServer(String p_104532_) {
      try {
         this.minecraft.services().sessionService().joinServer(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getAccessToken(), p_104532_);
         return null;
      } catch (AuthenticationUnavailableException authenticationunavailableexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
      } catch (InvalidCredentialsException invalidcredentialsexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
      } catch (InsufficientPrivilegesException insufficientprivilegesexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
      } catch (ForcedUsernameChangeException | UserBannedException userbannedexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
      } catch (AuthenticationException authenticationexception) {
         return Component.translatable("disconnect.loginFailedInfo", authenticationexception.getMessage());
      }
   }

   @Override
   public void handleLoginFinished(ClientboundLoginFinishedPacket p_368567_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.JOINING);
      GameProfile gameprofile = p_368567_.gameProfile();
      this.connection
         .setupInboundProtocol(
            ConfigurationProtocols.CLIENTBOUND,
            new ClientConfigurationPacketListenerImpl(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  this.levelLoadTracker,
                  gameprofile,
                  this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration, this.minigameName),
                  ClientRegistryLayer.createRegistryAccess().compositeAccess(),
                  FeatureFlags.DEFAULT_FLAGS,
                  null,
                  this.serverData,
                  this.parent,
                  this.cookies,
                  null,
                  Map.of(),
                  ServerLinks.EMPTY,
                  this.seenPlayers,
                  false
               )
            )
         );
      this.connection.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
      this.connection.send(new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrandRetriever.getClientModName())));
      this.connection.send(new ServerboundClientInformationPacket(this.minecraft.options.buildPlayerInformation()));
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_342266_) {
      Component component = this.wasTransferredTo ? CommonComponents.TRANSFER_CONNECT_FAILED : CommonComponents.CONNECT_FAILED;
      if (this.serverData != null && this.serverData.isRealm()) {
         this.minecraft.setScreen(new DisconnectedScreen(this.parent, component, p_342266_.reason(), CommonComponents.GUI_BACK));
      } else {
         this.minecraft.setScreen(new DisconnectedScreen(this.parent, component, p_342266_));
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   public void handleDisconnect(ClientboundLoginDisconnectPacket p_104553_) {
      this.connection.disconnect(p_104553_.reason());
   }

   @Override
   public void handleCompression(ClientboundLoginCompressionPacket p_104551_) {
      if (!this.connection.isMemoryConnection()) {
         this.connection.setupCompression(p_104551_.getCompressionThreshold(), false);
      }
   }

   @Override
   public void handleCustomQuery(ClientboundCustomQueryPacket p_104545_) {
      this.updateStatus.accept(Component.translatable("connect.negotiating"));
      this.connection.send(new ServerboundCustomQueryAnswerPacket(p_104545_.transactionId(), null));
   }

   public void setMinigameName(@Nullable String p_286653_) {
      this.minigameName = p_286653_;
   }

   @Override
   public void handleRequestCookie(ClientboundCookieRequestPacket p_328065_) {
      this.connection.send(new ServerboundCookieResponsePacket(p_328065_.key(), this.cookies.get(p_328065_.key())));
   }

   @Override
   public void fillListenerSpecificCrashDetails(CrashReport p_342297_, CrashReportCategory p_311844_) {
      p_311844_.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<unknown>");
      p_311844_.setDetail("Login phase", () -> this.state.get().toString());
      p_311844_.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
   }

   @OnlyIn(Dist.CLIENT)
   enum State {
      CONNECTING(Component.translatable("connect.connecting"), Set.of()),
      AUTHORIZING(Component.translatable("connect.authorizing"), Set.of(CONNECTING)),
      ENCRYPTING(Component.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
      JOINING(Component.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

      final Component message;
      final Set<ClientHandshakePacketListenerImpl.State> fromStates;

      State(final Component p_301605_, final Set<ClientHandshakePacketListenerImpl.State> p_301615_) {
         this.message = p_301605_;
         this.fromStates = p_301615_;
      }
   }
}
