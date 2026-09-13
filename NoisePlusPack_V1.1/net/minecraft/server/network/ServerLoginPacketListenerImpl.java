package net.minecraft.server.network;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerLoginPacketListenerImpl implements ServerLoginPacketListener, TickablePacketListener {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TICKS_BEFORE_LOGIN = 600;
   private final byte[] challenge;
   final MinecraftServer server;
   final Connection connection;
   final ServerActivityMonitor serverActivityMonitor;
   private volatile ServerLoginPacketListenerImpl.State state = ServerLoginPacketListenerImpl.State.HELLO;
   private int tick;
   @Nullable String requestedUsername;
   private @Nullable GameProfile authenticatedProfile;
   private final String serverId = "";
   private final boolean transferred;

   public ServerLoginPacketListenerImpl(MinecraftServer p_10027_, Connection p_10028_, boolean p_332648_) {
      this.server = p_10027_;
      this.connection = p_10028_;
      this.serverActivityMonitor = this.server.getServerActivityMonitor();
      this.challenge = Ints.toByteArray(RandomSource.create().nextInt());
      this.transferred = p_332648_;
   }

   @Override
   public void tick() {
      if (this.state == ServerLoginPacketListenerImpl.State.VERIFYING) {
         this.verifyLoginAndFinishConnectionSetup(Objects.requireNonNull(this.authenticatedProfile));
      }

      if (this.state == ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT
         && !this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile))) {
         this.finishLoginAndWaitForClient(this.authenticatedProfile);
      }

      if (this.tick++ == 600) {
         this.disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public void disconnect(Component p_10054_) {
      try {
         LOGGER.info("Disconnecting {}: {}", this.getUserName(), p_10054_.getString());
         this.connection.send(new ClientboundLoginDisconnectPacket(p_10054_));
         this.connection.disconnect(p_10054_);
      } catch (Exception exception) {
         LOGGER.error("Error whilst disconnecting player", exception);
      }
   }

   private boolean isPlayerAlreadyInWorld(GameProfile p_298499_) {
      return this.server.getPlayerList().getPlayer(p_298499_.id()) != null;
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_343815_) {
      LOGGER.info("{} lost connection: {}", this.getUserName(), p_343815_.reason().getString());
   }

   public String getUserName() {
      String s = this.connection.getLoggableAddress(this.server.logIPs());
      return this.requestedUsername != null ? this.requestedUsername + " (" + s + ")" : s;
   }

   @Override
   public void handleHello(ServerboundHelloPacket p_10047_) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet", new Object[0]);
      Validate.validState(StringUtil.isValidPlayerName(p_10047_.name()), "Invalid characters in username", new Object[0]);
      this.requestedUsername = p_10047_.name();
      GameProfile gameprofile = this.server.getSingleplayerProfile();
      if (gameprofile != null && this.requestedUsername.equalsIgnoreCase(gameprofile.name())) {
         this.startClientVerification(gameprofile);
      } else if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
         this.state = ServerLoginPacketListenerImpl.State.KEY;
         this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge, true));
      } else {
         this.startClientVerification(UUIDUtil.createOfflineProfile(this.requestedUsername));
      }
   }

   void startClientVerification(GameProfile p_301095_) {
      this.authenticatedProfile = p_301095_;
      this.state = ServerLoginPacketListenerImpl.State.VERIFYING;
   }

   private void verifyLoginAndFinishConnectionSetup(GameProfile p_299507_) {
      PlayerList playerlist = this.server.getPlayerList();
      Component component = playerlist.canPlayerLogin(this.connection.getRemoteAddress(), new NameAndId(p_299507_));
      if (component != null) {
         this.disconnect(component);
      } else {
         if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
            this.connection
               .send(
                  new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()),
                  PacketSendListener.thenRun(() -> this.connection.setupCompression(this.server.getCompressionThreshold(), true))
               );
         }

         boolean flag = playerlist.disconnectAllPlayersWithProfile(p_299507_.id());
         if (flag) {
            this.state = ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT;
         } else {
            this.finishLoginAndWaitForClient(p_299507_);
         }
      }
   }

   private void finishLoginAndWaitForClient(GameProfile p_300150_) {
      this.state = ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING;
      this.connection.send(new ClientboundLoginFinishedPacket(p_300150_));
   }

   @Override
   public void handleKey(ServerboundKeyPacket p_10049_) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet", new Object[0]);

      final String s;
      try {
         PrivateKey privatekey = this.server.getKeyPair().getPrivate();
         if (!p_10049_.isChallengeValid(this.challenge, privatekey)) {
            throw new IllegalStateException("Protocol error");
         }

         SecretKey secretkey = p_10049_.getSecretKey(privatekey);
         Cipher cipher = Crypt.getCipher(2, secretkey);
         Cipher cipher1 = Crypt.getCipher(1, secretkey);
         s = new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretkey)).toString(16);
         this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
         this.connection.setEncryptionKey(cipher, cipher1);
      } catch (CryptException cryptexception) {
         throw new IllegalStateException("Protocol error", cryptexception);
      }

      Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
         @Override
         public void run() {
            String s1 = Objects.requireNonNull(ServerLoginPacketListenerImpl.this.requestedUsername, "Player name not initialized");

            try {
               ProfileResult profileresult = ServerLoginPacketListenerImpl.this.server.services().sessionService().hasJoinedServer(s1, s, this.getAddress());
               if (profileresult != null) {
                  GameProfile gameprofile = profileresult.profile();
                  ServerLoginPacketListenerImpl.LOGGER.info("UUID of player {} is {}", gameprofile.name(), gameprofile.id());
                  ServerLoginPacketListenerImpl.this.serverActivityMonitor.reportLoginActivity();
                  ServerLoginPacketListenerImpl.this.startClientVerification(gameprofile);
               } else if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                  ServerLoginPacketListenerImpl.LOGGER.warn("Failed to verify username but will let them in anyway!");
                  ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(s1));
               } else {
                  ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                  ServerLoginPacketListenerImpl.LOGGER.error("Username '{}' tried to join with an invalid session", s1);
               }
            } catch (AuthenticationUnavailableException authenticationunavailableexception) {
               if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                  ServerLoginPacketListenerImpl.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                  ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(s1));
               } else {
                  ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                  ServerLoginPacketListenerImpl.LOGGER.error("Couldn't verify username because servers are unavailable");
               }
            }
         }

         private @Nullable InetAddress getAddress() {
            SocketAddress socketaddress = ServerLoginPacketListenerImpl.this.connection.getRemoteAddress();
            return ServerLoginPacketListenerImpl.this.server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress
               ? ((InetSocketAddress)socketaddress).getAddress()
               : null;
         }
      };
      thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   @Override
   public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket p_297965_) {
      this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
   }

   @Override
   public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket p_298815_) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING, "Unexpected login acknowledgement packet", new Object[0]);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
      CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(Objects.requireNonNull(this.authenticatedProfile), this.transferred);
      ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = new ServerConfigurationPacketListenerImpl(
         this.server, this.connection, commonlistenercookie
      );
      this.connection.setupInboundProtocol(ConfigurationProtocols.SERVERBOUND, serverconfigurationpacketlistenerimpl);
      serverconfigurationpacketlistenerimpl.startConfiguration();
      this.state = ServerLoginPacketListenerImpl.State.ACCEPTED;
   }

   @Override
   public void fillListenerSpecificCrashDetails(CrashReport p_345455_, CrashReportCategory p_310682_) {
      p_310682_.setDetail("Login phase", () -> this.state.toString());
   }

   @Override
   public void handleCookieResponse(ServerboundCookieResponsePacket p_333672_) {
      this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
   }

   enum State {
      HELLO,
      KEY,
      AUTHENTICATING,
      NEGOTIATING,
      VERIFYING,
      WAITING_FOR_DUPE_DISCONNECT,
      PROTOCOL_SWITCHING,
      ACCEPTED;
   }
}
