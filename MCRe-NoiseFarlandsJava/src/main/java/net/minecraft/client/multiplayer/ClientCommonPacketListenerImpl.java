package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.dialog.DialogScreens;
import net.minecraft.client.gui.screens.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
    private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Minecraft minecraft;
    protected final Connection connection;
    protected final @Nullable ServerData serverData;
    protected @Nullable String serverBrand;
    protected final WorldSessionTelemetryManager telemetryManager;
    protected final @Nullable Screen postDisconnectScreen;
    protected boolean isTransferring;
    private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();
    protected final Map<Identifier, byte[]> serverCookies;
    protected Map<String, String> customReportDetails;
    private ServerLinks serverLinks;
    protected final Map<UUID, PlayerInfo> seenPlayers;
    protected boolean seenInsecureChatWarning;

    protected ClientCommonPacketListenerImpl(final Minecraft minecraft, final Connection connection, final CommonListenerCookie cookie) {
        this.minecraft = minecraft;
        this.connection = connection;
        this.serverData = cookie.serverData();
        this.serverBrand = cookie.serverBrand();
        this.telemetryManager = cookie.telemetryManager();
        this.postDisconnectScreen = cookie.postDisconnectScreen();
        this.serverCookies = cookie.serverCookies();
        this.customReportDetails = cookie.customReportDetails();
        this.serverLinks = cookie.serverLinks();
        this.seenPlayers = new HashMap<>(cookie.seenPlayers());
        this.seenInsecureChatWarning = cookie.seenInsecureChatWarning();
    }

    public ServerLinks serverLinks() {
        return this.serverLinks;
    }

    @Override
    public void onPacketError(final Packet packet, final Exception cause) {
        LOGGER.error("Failed to handle packet {}, disconnecting", packet, cause);
        Optional<Path> report = this.storeDisconnectionReport(packet, cause);
        Optional<URI> bugReportLink = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        this.connection.disconnect(new DisconnectionDetails(Component.translatable("disconnect.packetError"), report, bugReportLink));
    }

    @Override
    public DisconnectionDetails createDisconnectionInfo(final Component reason, final Throwable cause) {
        Optional<Path> report = this.storeDisconnectionReport(null, cause);
        Optional<URI> bugReportUrl = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        return new DisconnectionDetails(reason, report, bugReportUrl);
    }

    private Optional<Path> storeDisconnectionReport(final @Nullable Packet packet, final Throwable cause) {
        CrashReport report = CrashReport.forThrowable(cause, "Packet handling error");
        PacketUtils.fillCrashReport(report, this, packet);
        Path debugDir = this.minecraft.gameDirectory.toPath().resolve("debug");
        Path reportFile = debugDir.resolve("disconnect-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Optional<ServerLinks.Entry> bugReportLink = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT);
        List<String> extraComments = bugReportLink.<List<String>>map(link -> List.of("Server bug reporting link: " + link.link())).orElse(List.of());
        return report.saveToFile(reportFile, ReportType.NETWORK_PROTOCOL_ERROR, extraComments) ? Optional.of(reportFile) : Optional.empty();
    }

    @Override
    public boolean shouldHandleMessage(final Packet<?> packet) {
        return ClientCommonPacketListener.super.shouldHandleMessage(packet)
            ? true
            : this.isTransferring && (packet instanceof ClientboundStoreCookiePacket || packet instanceof ClientboundTransferPacket);
    }

    @Override
    public void handleKeepAlive(final ClientboundKeepAlivePacket packet) {
        this.sendWhen(new ServerboundKeepAlivePacket(packet.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
    }

    @Override
    public void handlePing(final ClientboundPingPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.send(new ServerboundPongPacket(packet.getId()));
    }

    @Override
    public void handleCustomPayload(final ClientboundCustomPayloadPacket packet) {
        CustomPacketPayload payload = packet.payload();
        if (!(payload instanceof DiscardedPayload)) {
            PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
            if (payload instanceof BrandPayload brand) {
                this.serverBrand = brand.brand();
                this.telemetryManager.onServerBrandReceived(brand.brand());
            } else {
                this.handleCustomPayload(payload);
            }
        }
    }

    protected abstract void handleCustomPayload(CustomPacketPayload payload);

    @Override
    public void handleResourcePackPush(final ClientboundResourcePackPushPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        UUID packId = packet.id();
        URL url = parseResourcePackUrl(packet.url());
        if (url == null) {
            this.connection.send(new ServerboundResourcePackPacket(packId, ServerboundResourcePackPacket.Action.INVALID_URL));
        } else {
            String hash = packet.hash();
            boolean required = packet.required();
            ServerData.ServerPackStatus serverPackStatus = this.serverData != null
                ? this.serverData.getResourcePackStatus()
                : ServerData.ServerPackStatus.PROMPT;
            if (serverPackStatus != ServerData.ServerPackStatus.PROMPT && (!required || serverPackStatus != ServerData.ServerPackStatus.DISABLED)) {
                this.minecraft.getDownloadedPackSource().pushPack(packId, url, hash);
            } else {
                this.minecraft.gui.setScreen(this.addOrUpdatePackPrompt(packId, url, hash, required, packet.prompt().orElse(null)));
            }
        }
    }

    @Override
    public void handleResourcePackPop(final ClientboundResourcePackPopPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.id().ifPresentOrElse(id -> this.minecraft.getDownloadedPackSource().popPack(id), () -> this.minecraft.getDownloadedPackSource().popAll());
    }

    private static Component preparePackPrompt(final Component header, final @Nullable Component prompt) {
        return prompt == null ? header : Component.translatable("multiplayer.texturePrompt.serverPrompt", header, prompt);
    }

    private static @Nullable URL parseResourcePackUrl(final String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            return !"http".equals(protocol) && !"https".equals(protocol) ? null : url;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public void handleRequestCookie(final ClientboundCookieRequestPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.connection.send(new ServerboundCookieResponsePacket(packet.key(), this.serverCookies.get(packet.key())));
    }

    @Override
    public void handleStoreCookie(final ClientboundStoreCookiePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.serverCookies.put(packet.key(), packet.payload());
    }

    @Override
    public void handleCustomReportDetails(final ClientboundCustomReportDetailsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.customReportDetails = packet.details();
    }

    @Override
    public void handleServerLinks(final ClientboundServerLinksPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        List<ServerLinks.UntrustedEntry> untrustedEntries = packet.links();
        Builder<ServerLinks.Entry> trustedEntries = ImmutableList.builderWithExpectedSize(untrustedEntries.size());

        for (ServerLinks.UntrustedEntry entry : untrustedEntries) {
            try {
                URI parsedLink = Util.parseAndValidateUntrustedUri(entry.link());
                trustedEntries.add(new ServerLinks.Entry(entry.type(), parsedLink));
            } catch (Exception e) {
                LOGGER.warn("Received invalid link for type {}:{}", entry.type(), entry.link(), e);
            }
        }

        this.serverLinks = new ServerLinks(trustedEntries.build());
    }

    @Override
    public void handleShowDialog(final ClientboundShowDialogPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.showDialog(packet.dialog(), this.minecraft.gui.screen());
    }

    protected abstract DialogConnectionAccess createDialogAccess();

    public void showDialog(final Holder<Dialog> dialog, final @Nullable Screen activeScreen) {
        this.showDialog(dialog, this.createDialogAccess(), activeScreen);
    }

    protected void showDialog(final Holder<Dialog> dialog, final DialogConnectionAccess connectionAccess, final @Nullable Screen activeScreen) {
        if (activeScreen instanceof DialogScreen.WarningScreen existingWarningScreen) {
            Screen hiddenScreen = existingWarningScreen.returnScreen();
            Screen previousScreen = hiddenScreen instanceof DialogScreen<?> hiddenDialog ? hiddenDialog.previousScreen() : hiddenScreen;
            DialogScreen<?> newDialogScreen = DialogScreens.createFromData(dialog.value(), previousScreen, connectionAccess);
            if (newDialogScreen != null) {
                existingWarningScreen.updateReturnScreen(newDialogScreen);
            } else {
                LOGGER.warn("Failed to show dialog for data {}", dialog);
            }
        } else {
            Screen previousScreen;
            if (activeScreen instanceof DialogScreen<?> existingDialog) {
                previousScreen = existingDialog.previousScreen();
            } else if (activeScreen instanceof WaitingForResponseScreen waitScreen) {
                previousScreen = waitScreen.previousScreen();
            } else {
                previousScreen = activeScreen;
            }

            Screen screen = DialogScreens.createFromData(dialog.value(), previousScreen, connectionAccess);
            if (screen != null) {
                this.minecraft.gui.setScreen(screen);
            } else {
                LOGGER.warn("Failed to show dialog for data {}", dialog);
            }
        }
    }

    @Override
    public void handleClearDialog(final ClientboundClearDialogPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.clearDialog();
    }

    public void clearDialog() {
        if (this.minecraft.gui.screen() instanceof DialogScreen.WarningScreen existingWarningScreen) {
            if (existingWarningScreen.returnScreen() instanceof DialogScreen<?> dialogScreen) {
                existingWarningScreen.updateReturnScreen(dialogScreen.previousScreen());
            }
        } else if (this.minecraft.gui.screen() instanceof DialogScreen<?> dialog) {
            this.minecraft.gui.setScreen(dialog.previousScreen());
        }
    }

    @Override
    public void handleTransfer(final ClientboundTransferPacket packet) {
        this.isTransferring = true;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.serverData == null) {
            throw new IllegalStateException("Cannot transfer to server from singleplayer");
        }

        this.connection.disconnect(Component.translatable("disconnect.transfer"));
        this.connection.setReadOnly();
        this.connection.handleDisconnection();
        ServerAddress address = new ServerAddress(packet.host(), packet.port());
        ConnectScreen.startConnecting(
            Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new),
            this.minecraft,
            address,
            this.serverData,
            false,
            new TransferState(this.serverCookies, this.seenPlayers, this.seenInsecureChatWarning)
        );
    }

    @Override
    public void handleDisconnect(final ClientboundDisconnectPacket packet) {
        this.connection.disconnect(packet.reason());
    }

    protected void sendDeferredPackets() {
        Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

        while (iterator.hasNext()) {
            ClientCommonPacketListenerImpl.DeferredPacket deferredPacket = iterator.next();
            if (deferredPacket.sendCondition().getAsBoolean()) {
                this.send(deferredPacket.packet);
                iterator.remove();
            } else if (deferredPacket.expirationTime() <= Util.getMillis()) {
                iterator.remove();
            }
        }
    }

    public void send(final Packet<?> packet) {
        this.connection.send(packet);
    }

    @Override
    public void onDisconnect(final DisconnectionDetails details) {
        this.telemetryManager.onDisconnect();
        this.minecraft.disconnect(this.createDisconnectScreen(details), this.isTransferring);
        LOGGER.warn("Client disconnected with reason: {}", details.reason().getString());
    }

    @Override
    public void fillListenerSpecificCrashDetails(final CrashReport report, final CrashReportCategory connectionDetails) {
        connectionDetails.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
        connectionDetails.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<none>");
        connectionDetails.setDetail("Server brand", () -> this.serverBrand);
        if (!this.customReportDetails.isEmpty()) {
            CrashReportCategory serverDetailsCategory = report.addCategory("Custom Server Details");
            this.customReportDetails.forEach(serverDetailsCategory::setDetail);
        }
    }

    protected Screen createDisconnectScreen(final DisconnectionDetails details) {
        Screen callbackScreen = Objects.requireNonNullElseGet(
            this.postDisconnectScreen, () -> this.serverData != null ? new JoinMultiplayerScreen(new TitleScreen()) : new TitleScreen()
        );
        return this.serverData != null && this.serverData.isRealm()
            ? new DisconnectedScreen(callbackScreen, GENERIC_DISCONNECT_MESSAGE, details, CommonComponents.GUI_BACK)
            : new DisconnectedScreen(callbackScreen, GENERIC_DISCONNECT_MESSAGE, details);
    }

    public @Nullable String serverBrand() {
        return this.serverBrand;
    }

    private void sendWhen(final Packet<? extends ServerboundPacketListener> packet, final BooleanSupplier condition, final Duration expireAfterDuration) {
        if (condition.getAsBoolean()) {
            this.send(packet);
        } else {
            this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(packet, condition, Util.getMillis() + expireAfterDuration.toMillis()));
        }
    }

    private Screen addOrUpdatePackPrompt(final UUID packId, final URL url, final String hash, final boolean required, final @Nullable Component prompt) {
        Screen currentScreen = this.minecraft.gui.screen();
        return currentScreen instanceof ClientCommonPacketListenerImpl.PackConfirmScreen promptScreen
            ? promptScreen.update(this.minecraft, packId, url, hash, required, prompt)
            : new ClientCommonPacketListenerImpl.PackConfirmScreen(
                this.minecraft,
                currentScreen,
                List.of(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(packId, url, hash)),
                required,
                prompt
            );
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract class CommonDialogAccess implements DialogConnectionAccess {
        @Override
        public void disconnect(final Component message) {
            ClientCommonPacketListenerImpl.this.connection.disconnect(message);
            ClientCommonPacketListenerImpl.this.connection.handleDisconnection();
        }

        @Override
        public void openDialog(final Holder<Dialog> dialog, final @Nullable Screen activeScreen) {
            ClientCommonPacketListenerImpl.this.showDialog(dialog, this, activeScreen);
        }

        @Override
        public void sendCustomAction(final Identifier id, final Optional<Tag> payload) {
            ClientCommonPacketListenerImpl.this.send(new ServerboundCustomClickActionPacket(id, payload));
        }

        @Override
        public ServerLinks serverLinks() {
            return ClientCommonPacketListenerImpl.this.serverLinks();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
    }

    @OnlyIn(Dist.CLIENT)
    private class PackConfirmScreen extends ConfirmScreen {
        private final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests;
        private final @Nullable Screen parentScreen;

        private PackConfirmScreen(
            final Minecraft minecraft,
            final @Nullable Screen parentScreen,
            final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests,
            final boolean required,
            final @Nullable Component prompt
        ) {
            super(
                result -> {
                    minecraft.gui.setScreen(parentScreen);
                    DownloadedPackSource packSource = minecraft.getDownloadedPackSource();
                    if (result) {
                        if (ClientCommonPacketListenerImpl.this.serverData != null) {
                            ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                        }

                        packSource.allowServerPacks();
                    } else {
                        packSource.rejectServerPacks();
                        if (required) {
                            ClientCommonPacketListenerImpl.this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                        } else if (ClientCommonPacketListenerImpl.this.serverData != null) {
                            ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                        }
                    }

                    for (ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest request : requests) {
                        packSource.pushPack(request.id, request.url, request.hash);
                    }

                    if (ClientCommonPacketListenerImpl.this.serverData != null) {
                        ServerList.saveSingleServer(ClientCommonPacketListenerImpl.this.serverData);
                    }
                },
                required ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"),
                ClientCommonPacketListenerImpl.preparePackPrompt(
                    required
                        ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        : Component.translatable("multiplayer.texturePrompt.line2"),
                    prompt
                ),
                required ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES,
                required ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO
            );
            this.requests = requests;
            this.parentScreen = parentScreen;
        }

        public ClientCommonPacketListenerImpl.PackConfirmScreen update(
            final Minecraft minecraft, final UUID id, final URL url, final String hash, final boolean required, final @Nullable Component prompt
        ) {
            List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> extendedRequests = ImmutableList.<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest>builderWithExpectedSize(
                    this.requests.size() + 1
                )
                .addAll(this.requests)
                .add(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(id, url, hash))
                .build();
            return ClientCommonPacketListenerImpl.this.new PackConfirmScreen(minecraft, this.parentScreen, extendedRequests, required, prompt);
        }

        @OnlyIn(Dist.CLIENT)
        private record PendingRequest(UUID id, URL url, String hash) {
        }
    }
}