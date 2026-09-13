package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.multiplayer.CodeOfConductScreen;
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
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Component DISCONNECTED_MESSAGE = Component.translatable("multiplayer.disconnect.code_of_conduct");
   private final LevelLoadTracker levelLoadTracker;
   private final GameProfile localGameProfile;
   private FeatureFlagSet enabledFeatures;
   private final RegistryAccess.Frozen receivedRegistries;
   private final RegistryDataCollector registryDataCollector = new RegistryDataCollector();
   private @Nullable KnownPacksManager knownPacks;
   protected ChatComponent.@Nullable State chatState;
   private boolean seenCodeOfConduct;

   public ClientConfigurationPacketListenerImpl(Minecraft p_301278_, Connection p_299257_, CommonListenerCookie p_300907_) {
      super(p_301278_, p_299257_, p_300907_);
      this.levelLoadTracker = p_300907_.levelLoadTracker();
      this.localGameProfile = p_300907_.localGameProfile();
      this.receivedRegistries = p_300907_.receivedRegistries();
      this.enabledFeatures = p_300907_.enabledFeatures();
      this.chatState = p_300907_.chatState();
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   protected void handleCustomPayload(CustomPacketPayload p_301281_) {
      this.handleUnknownCustomPayload(p_301281_);
   }

   private void handleUnknownCustomPayload(CustomPacketPayload p_300719_) {
      LOGGER.warn("Unknown custom packet payload: {}", p_300719_.type().id());
   }

   @Override
   public void handleRegistryData(ClientboundRegistryDataPacket p_299218_) {
      PacketUtils.ensureRunningOnSameThread(p_299218_, this, this.minecraft.packetProcessor());
      this.registryDataCollector.appendContents(p_299218_.registry(), p_299218_.entries());
   }

   @Override
   public void handleUpdateTags(ClientboundUpdateTagsPacket p_335168_) {
      PacketUtils.ensureRunningOnSameThread(p_335168_, this, this.minecraft.packetProcessor());
      this.registryDataCollector.appendTags(p_335168_.getTags());
   }

   @Override
   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket p_301158_) {
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(p_301158_.features());
   }

   @Override
   public void handleSelectKnownPacks(ClientboundSelectKnownPacks p_333075_) {
      PacketUtils.ensureRunningOnSameThread(p_333075_, this, this.minecraft.packetProcessor());
      if (this.knownPacks == null) {
         this.knownPacks = new KnownPacksManager();
      }

      List<KnownPack> list = this.knownPacks.trySelectingPacks(p_333075_.knownPacks());
      this.send(new ServerboundSelectKnownPacks(list));
   }

   @Override
   public void handleResetChat(ClientboundResetChatPacket p_328730_) {
      this.chatState = null;
   }

   private <T> T runWithResources(Function<ResourceProvider, T> p_330303_) {
      if (this.knownPacks == null) {
         return p_330303_.apply(ResourceProvider.EMPTY);
      }

      try (CloseableResourceManager closeableresourcemanager = this.knownPacks.createResourceManager()) {
         return p_330303_.apply(closeableresourcemanager);
      }
   }

   @Override
   public void handleCodeOfConduct(ClientboundCodeOfConductPacket p_423799_) {
      PacketUtils.ensureRunningOnSameThread(p_423799_, this, this.minecraft.packetProcessor());
      if (this.seenCodeOfConduct) {
         throw new IllegalStateException("Server sent duplicate Code of Conduct");
      }

      this.seenCodeOfConduct = true;
      String s = p_423799_.codeOfConduct();
      if (this.serverData != null && this.serverData.hasAcceptedCodeOfConduct(s)) {
         this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
      } else {
         Screen screen = this.minecraft.screen;
         this.minecraft.setScreen(new CodeOfConductScreen(this.serverData, screen, s, p_420830_ -> {
            if (p_420830_) {
               this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
               this.minecraft.setScreen(screen);
            } else {
               this.createDialogAccess().disconnect(DISCONNECTED_MESSAGE);
            }
         }));
      }
   }

   @Override
   public void handleConfigurationFinished(ClientboundFinishConfigurationPacket p_299280_) {
      PacketUtils.ensureRunningOnSameThread(p_299280_, this, this.minecraft.packetProcessor());
      RegistryAccess.Frozen registryaccess$frozen = this.runWithResources(
         p_357775_ -> this.registryDataCollector.collectGameRegistries(p_357775_, this.receivedRegistries, this.connection.isMemoryConnection())
      );
      this.connection
         .setupInboundProtocol(
            GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registryaccess$frozen)),
            new ClientPacketListener(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  this.levelLoadTracker,
                  this.localGameProfile,
                  this.telemetryManager,
                  registryaccess$frozen,
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
         .setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registryaccess$frozen), new GameProtocols.Context() {
            @Override
            public boolean hasInfiniteMaterials() {
               return true;
            }
         }));
   }

   @Override
   public void tick() {
      this.sendDeferredPackets();
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_343449_) {
      super.onDisconnect(p_343449_);
      this.minecraft.clearDownloadedResourcePacks();
   }

   @Override
   protected DialogConnectionAccess createDialogAccess() {
      return new ClientCommonPacketListenerImpl.CommonDialogAccess() {
         @Override
         public void runCommand(String p_408326_, @Nullable Screen p_406657_) {
            ClientConfigurationPacketListenerImpl.LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", p_408326_);
         }
      };
   }
}
