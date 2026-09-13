package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.multiplayer.CodeOfConductScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientConfigurationPacketListenerImpl extends ClientCommonPacketListenerImpl implements ClientConfigurationPacketListener, TickablePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component DISCONNECTED_MESSAGE = Component.translatable("multiplayer.disconnect.code_of_conduct");
    private final LevelLoadTracker levelLoadTracker;
    private final GameProfile localGameProfile;
    private FeatureFlagSet enabledFeatures;
    private final RegistryAccess.Frozen receivedRegistries;
    private final RegistryDataCollector registryDataCollector = new RegistryDataCollector();
    private @Nullable KnownPacksManager knownPacks;
    protected ChatComponent.@Nullable State chatState;
    private boolean seenCodeOfConduct;

    public ClientConfigurationPacketListenerImpl(final Minecraft minecraft, final Connection connection, final CommonListenerCookie cookie) {
        super(minecraft, connection, cookie);
        this.levelLoadTracker = cookie.levelLoadTracker();
        this.localGameProfile = cookie.localGameProfile();
        this.receivedRegistries = cookie.receivedRegistries();
        this.enabledFeatures = cookie.enabledFeatures();
        this.chatState = cookie.chatState();
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    @Override
    protected void handleCustomPayload(final CustomPacketPayload payload) {
        this.handleUnknownCustomPayload(payload);
    }

    private void handleUnknownCustomPayload(final CustomPacketPayload payload) {
        LOGGER.warn("Unknown custom packet payload: {}", payload.type().id());
    }

    @Override
    public void handleRegistryData(final ClientboundRegistryDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.registryDataCollector.appendContents(packet.registry(), packet.entries());
    }

    @Override
    public void handleUpdateTags(final ClientboundUpdateTagsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.registryDataCollector.appendTags(packet.getTags());
    }

    @Override
    public void handleEnabledFeatures(final ClientboundUpdateEnabledFeaturesPacket packet) {
        this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(packet.features());
    }

    @Override
    public void handleSelectKnownPacks(final ClientboundSelectKnownPacks packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.knownPacks == null) {
            this.knownPacks = new KnownPacksManager();
        }

        List<KnownPack> selected = this.knownPacks.trySelectingPacks(packet.knownPacks());
        this.send(new ServerboundSelectKnownPacks(selected));
    }

    @Override
    public void handleResetChat(final ClientboundResetChatPacket packet) {
        this.chatState = null;
    }

    private <T> T runWithResources(final Function<ResourceProvider, T> operation) {
        if (this.knownPacks == null) {
            return operation.apply(ResourceProvider.EMPTY);
        }

        try (CloseableResourceManager manager = this.knownPacks.createResourceManager()) {
            return operation.apply(manager);
        }
    }

    @Override
    public void handleCodeOfConduct(final ClientboundCodeOfConductPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.seenCodeOfConduct) {
            throw new IllegalStateException("Server sent duplicate Code of Conduct");
        }

        this.seenCodeOfConduct = true;
        String codeOfConduct = packet.codeOfConduct();
        if (this.serverData != null && this.serverData.hasAcceptedCodeOfConduct(codeOfConduct)) {
            this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
        } else {
            Screen lastScreen = this.minecraft.gui.screen();
            this.minecraft.gui.setScreen(new CodeOfConductScreen(this.serverData, lastScreen, codeOfConduct, accepted -> {
                if (accepted) {
                    this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
                    this.minecraft.gui.setScreen(lastScreen);
                } else {
                    this.createDialogAccess().disconnect(DISCONNECTED_MESSAGE);
                }
            }));
        }
    }

    @Override
    public void handleConfigurationFinished(final ClientboundFinishConfigurationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        RegistryAccess.Frozen registries = this.runWithResources(
            knownPacksProvider -> this.registryDataCollector
                .collectGameRegistries(knownPacksProvider, this.receivedRegistries, this.connection.isMemoryConnection())
        );
        IntegratedServer localServer = this.minecraft.getSingleplayerServer();
        if (localServer != null) {
            registries = filterRegistries(localServer.registryAccess(), registries.listRegistryKeys());
        }

        this.connection
            .setupInboundProtocol(
                GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries)),
                new ClientPacketListener(
                    this.minecraft,
                    this.connection,
                    new CommonListenerCookie(
                        this.levelLoadTracker,
                        this.localGameProfile,
                        this.telemetryManager,
                        registries,
                        this.enabledFeatures,
                        this.serverBrand,
                        this.serverData,
                        this.postDisconnectScreen,
                        this.serverCookies,
                        this.chatState,
                        this.customReportDetails,
                        this.serverLinks(),
                        this.seenPlayers,
                        this.seenInsecureChatWarning
                    )
                )
            );
        this.connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
        this.connection
            .setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries), new GameProtocols.Context() {
                @Override
                public boolean hasInfiniteMaterials() {
                    return true;
                }
            }));
    }

    private static RegistryAccess.Frozen filterRegistries(final RegistryAccess.Frozen original, final Stream<ResourceKey<? extends Registry<?>>> keysToInclude) {
        List<? extends Registry<?>> filteredRegistries = keysToInclude.map(original::lookupOrThrow).toList();
        return new RegistryAccess.ImmutableRegistryAccess(filteredRegistries).freeze();
    }

    @Override
    public void tick() {
        this.sendDeferredPackets();
    }

    @Override
    public void onDisconnect(final DisconnectionDetails reason) {
        super.onDisconnect(reason);
        this.minecraft.clearDownloadedResourcePacks();
    }

    @Override
    protected DialogConnectionAccess createDialogAccess() {
        return new ClientCommonPacketListenerImpl.CommonDialogAccess() {
            @Override
            public void runCommand(final String command, final @Nullable Screen activeScreen) {
                ClientConfigurationPacketListenerImpl.LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", command);
            }
        };
    }
}