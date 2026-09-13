package net.minecraft.server.network;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
import net.minecraft.network.chat.CommonComponents;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private final byte[] challenge;
    private final MinecraftServer server;
    private final Connection connection;
    private final ServerActivityMonitor serverActivityMonitor;
    private volatile ServerLoginPacketListenerImpl.State state = ServerLoginPacketListenerImpl.State.HELLO;
    private int tick;
    private @Nullable String requestedUsername;
    private @Nullable GameProfile authenticatedProfile;
    private final String serverId = "";
    private final boolean transferred;

    public ServerLoginPacketListenerImpl(final MinecraftServer minecraftserver, final Connection connection, final boolean transferred) {
        this.server = minecraftserver;
        this.connection = connection;
        this.serverActivityMonitor = this.server.getServerActivityMonitor();
        this.challenge = Ints.toByteArray(RandomSource.create().nextInt());
        this.transferred = transferred;
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

    public void disconnect(final Component component) {
        try {
            LOGGER.info("Disconnecting {}: {}", this.getUserName(), component.getString());
            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
        } catch (Exception e) {
            LOGGER.error("Error whilst disconnecting player", e);
        }
    }

    private boolean isPlayerAlreadyInWorld(final GameProfile gameProfile) {
        return this.server.getPlayerList().getPlayer(gameProfile.id()) != null;
    }

    @Override
    public void onDisconnect(final DisconnectionDetails details) {
        LOGGER.info("{} lost connection: {}", this.getUserName(), details.reason().getString());
    }

    public String getUserName() {
        String loggableAddress = this.connection.getLoggableAddress(this.server.logIPs());
        return this.requestedUsername != null ? this.requestedUsername + " (" + loggableAddress + ")" : loggableAddress;
    }

    @Override
    public void handleHello(final ServerboundHelloPacket packet) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        Validate.validState(StringUtil.isValidPlayerName(packet.name()), "Invalid characters in username");
        this.requestedUsername = packet.name();
        GameProfile singleplayerProfile = this.server.getSingleplayerProfile();
        if (singleplayerProfile != null && this.requestedUsername.equalsIgnoreCase(singleplayerProfile.name())) {
            this.startClientVerification(singleplayerProfile);
        } else {
            if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
                this.state = ServerLoginPacketListenerImpl.State.KEY;
                this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge, true));
            } else {
                this.startClientVerification(UUIDUtil.createOfflineProfile(this.requestedUsername));
            }
        }
    }

    private void startClientVerification(final GameProfile profile) {
        this.authenticatedProfile = profile;
        this.state = ServerLoginPacketListenerImpl.State.VERIFYING;
    }

    private void verifyLoginAndFinishConnectionSetup(final GameProfile profile) {
        PlayerList playerList = this.server.getPlayerList();
        Component error = playerList.canPlayerLogin(this.connection.getRemoteAddress(), new NameAndId(profile));
        if (error != null) {
            this.disconnect(error);
        } else if (this.connection.getIntendedProfileId() != null && !profile.id().equals(this.connection.getIntendedProfileId())) {
            this.disconnect(CommonComponents.CONNECT_FAILED);
        } else {
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
                this.connection
                    .send(
                        new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()),
                        PacketSendListener.thenRun(() -> this.connection.setupCompression(this.server.getCompressionThreshold(), true))
                    );
            }

            boolean waitForDisconnection = playerList.disconnectAllPlayersWithProfile(profile.id());
            if (waitForDisconnection) {
                this.state = ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT;
            } else {
                this.finishLoginAndWaitForClient(profile);
            }
        }
    }

    private void finishLoginAndWaitForClient(final GameProfile gameProfile) {
        this.state = ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING;
        this.connection.send(new ClientboundLoginFinishedPacket(gameProfile, this.server.getConnection().getSessionId()));
    }

    @Override
    public void handleKey(final ServerboundKeyPacket packet) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet");

        final String digest;
        try {
            PrivateKey serverPrivateKey = this.server.getKeyPair().getPrivate();
            if (!packet.isChallengeValid(this.challenge, serverPrivateKey)) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packet.getSecretKey(serverPrivateKey);
            digest = new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey)).toString(16);
            this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
            Cipher decryptCipher = Crypt.getCipher(2, secretKey);
            Cipher encryptCipher = Crypt.getCipher(1, secretKey);
            this.connection.setEncryptionKey(decryptCipher, encryptCipher);
        } catch (CryptException e) {
            throw new IllegalStateException("Protocol error", e);
        }

        Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            @Override
            public void run() {
                String name = Objects.requireNonNull(ServerLoginPacketListenerImpl.this.requestedUsername, "Player name not initialized");

                try {
                    ProfileResult result = ServerLoginPacketListenerImpl.this.server
                        .services()
                        .sessionService()
                        .hasJoinedServer(name, digest, this.getAddress());
                    if (result != null) {
                        GameProfile profile = result.profile();
                        ServerLoginPacketListenerImpl.LOGGER.info("UUID of player {} is {}", profile.name(), profile.id());
                        ServerLoginPacketListenerImpl.this.serverActivityMonitor.reportLoginActivity();
                        ServerLoginPacketListenerImpl.this.startClientVerification(profile);
                    } else if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                        ServerLoginPacketListenerImpl.LOGGER.warn("Failed to verify username but will let them in anyway!");
                        ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(name));
                    } else {
                        ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                        ServerLoginPacketListenerImpl.LOGGER.error("Username '{}' tried to join with an invalid session", name);
                    }
                } catch (AuthenticationUnavailableException ignored) {
                    if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                        ServerLoginPacketListenerImpl.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(name));
                    } else {
                        ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                        ServerLoginPacketListenerImpl.LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                }
            }

            private @Nullable InetAddress getAddress() {
                return ServerLoginPacketListenerImpl.this.server.getPreventProxyConnections()
                        && ServerLoginPacketListenerImpl.this.connection.getRemoteAddress() instanceof InetSocketAddress inetSocketAddress
                    ? inetSocketAddress.getAddress()
                    : null;
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Override
    public void handleCustomQueryPacket(final ServerboundCustomQueryAnswerPacket packet) {
        this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
    }

    @Override
    public void handleLoginAcknowledgement(final ServerboundLoginAcknowledgedPacket packet) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING, "Unexpected login acknowledgement packet");
        this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(Objects.requireNonNull(this.authenticatedProfile), this.transferred);
        ServerConfigurationPacketListenerImpl configPacketListener = new ServerConfigurationPacketListenerImpl(this.server, this.connection, cookie);
        this.connection.setupInboundProtocol(ConfigurationProtocols.SERVERBOUND, configPacketListener);
        configPacketListener.startConfiguration();
        this.state = ServerLoginPacketListenerImpl.State.ACCEPTED;
    }

    @Override
    public void fillListenerSpecificCrashDetails(final CrashReport report, final CrashReportCategory connectionDetails) {
        connectionDetails.setDetail("Login phase", () -> this.state.toString());
    }

    @Override
    public void handleCookieResponse(final ServerboundCookieResponsePacket packet) {
        this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
    }

    private enum State {
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