package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
   static final Identifier INCOMPATIBLE_SPRITE = Identifier.withDefaultNamespace("server_list/incompatible");
   static final Identifier UNREACHABLE_SPRITE = Identifier.withDefaultNamespace("server_list/unreachable");
   static final Identifier PING_1_SPRITE = Identifier.withDefaultNamespace("server_list/ping_1");
   static final Identifier PING_2_SPRITE = Identifier.withDefaultNamespace("server_list/ping_2");
   static final Identifier PING_3_SPRITE = Identifier.withDefaultNamespace("server_list/ping_3");
   static final Identifier PING_4_SPRITE = Identifier.withDefaultNamespace("server_list/ping_4");
   static final Identifier PING_5_SPRITE = Identifier.withDefaultNamespace("server_list/ping_5");
   static final Identifier PINGING_1_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_1");
   static final Identifier PINGING_2_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_2");
   static final Identifier PINGING_3_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_3");
   static final Identifier PINGING_4_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_4");
   static final Identifier PINGING_5_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_5");
   static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/join_highlighted");
   static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("server_list/join");
   static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_up_highlighted");
   static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("server_list/move_up");
   static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_down_highlighted");
   static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("server_list/move_down");
   static final Logger LOGGER = LogUtils.getLogger();
   static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(
      5,
      new ThreadFactoryBuilder()
         .setNameFormat("Server Pinger #%d")
         .setDaemon(true)
         .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
         .build()
   );
   static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
   static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
   static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
   static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
   static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
   static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
   static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
   private final JoinMultiplayerScreen screen;
   private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
   private final ServerSelectionList.Entry lanHeader = new ServerSelectionList.LANHeader();
   private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();

   public ServerSelectionList(JoinMultiplayerScreen p_99771_, Minecraft p_99772_, int p_99773_, int p_99774_, int p_99775_, int p_99776_) {
      super(p_99772_, p_99773_, p_99774_, p_99775_, p_99776_);
      this.screen = p_99771_;
   }

   private void refreshEntries() {
      ServerSelectionList.Entry serverselectionlist$entry = this.getSelected();
      List<ServerSelectionList.Entry> list = new ArrayList<>(this.onlineServers);
      list.add(this.lanHeader);
      list.addAll(this.networkServers);
      this.replaceEntries(list);
      if (serverselectionlist$entry != null) {
         for (ServerSelectionList.Entry serverselectionlist$entry1 : list) {
            if (serverselectionlist$entry1.matches(serverselectionlist$entry)) {
               this.setSelected(serverselectionlist$entry1);
               break;
            }
         }
      }
   }

   public void setSelected(ServerSelectionList.@Nullable Entry p_99790_) {
      super.setSelected(p_99790_);
      this.screen.onSelectedChange();
   }

   public void updateOnlineServers(ServerList p_99798_) {
      this.onlineServers.clear();

      for (int i = 0; i < p_99798_.size(); i++) {
         this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, p_99798_.get(i)));
      }

      this.refreshEntries();
   }

   public void updateNetworkServers(List<LanServer> p_99800_) {
      int i = p_99800_.size() - this.networkServers.size();
      this.networkServers.clear();

      for (LanServer lanserver : p_99800_) {
         this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, lanserver));
      }

      this.refreshEntries();

      for (int i1 = this.networkServers.size() - i; i1 < this.networkServers.size(); i1++) {
         ServerSelectionList.NetworkServerEntry serverselectionlist$networkserverentry = this.networkServers.get(i1);
         int j = i1 - this.networkServers.size() + this.children().size();
         int k = this.getRowTop(j);
         int l = this.getRowBottom(j);
         if (l >= this.getY() && k <= this.getBottom()) {
            this.minecraft
               .getNarrator()
               .saySystemQueued(Component.translatable("multiplayer.lan.server_found", serverselectionlist$networkserverentry.getServerNarration()));
         }
      }
   }

   @Override
   public int getRowWidth() {
      return 305;
   }

   public void removed() {
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }

      abstract boolean matches(ServerSelectionList.Entry var1);

      public abstract void join();
   }

   @OnlyIn(Dist.CLIENT)
   public static class LANHeader extends ServerSelectionList.Entry {
      private final Minecraft minecraft = Minecraft.getInstance();
      private final LoadingDotsWidget loadingDotsWidget = new LoadingDotsWidget(this.minecraft.font, ServerSelectionList.SCANNING_LABEL);

      @Override
      public void renderContent(GuiGraphics p_426245_, int p_422472_, int p_425094_, boolean p_423481_, float p_429004_) {
         this.loadingDotsWidget.setPosition(this.getContentXMiddle() - this.minecraft.font.width(ServerSelectionList.SCANNING_LABEL) / 2, this.getContentY());
         this.loadingDotsWidget.render(p_426245_, p_422472_, p_425094_, p_429004_);
      }

      @Override
      public Component getNarration() {
         return ServerSelectionList.SCANNING_LABEL;
      }

      @Override
      boolean matches(ServerSelectionList.Entry p_429702_) {
         return p_429702_ instanceof ServerSelectionList.LANHeader;
      }

      @Override
      public void join() {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NetworkServerEntry extends ServerSelectionList.Entry {
      private static final int ICON_WIDTH = 32;
      private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
      private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
      private final JoinMultiplayerScreen screen;
      protected final Minecraft minecraft;
      protected final LanServer serverData;

      protected NetworkServerEntry(JoinMultiplayerScreen p_99836_, LanServer p_99837_) {
         this.screen = p_99836_;
         this.serverData = p_99837_;
         this.minecraft = Minecraft.getInstance();
      }

      @Override
      public void renderContent(GuiGraphics p_282600_, int p_282649_, int p_283641_, boolean p_283673_, float p_282694_) {
         p_282600_.drawString(this.minecraft.font, LAN_SERVER_HEADER, this.getContentX() + 32 + 3, this.getContentY() + 1, -1);
         p_282600_.drawString(this.minecraft.font, this.serverData.getMotd(), this.getContentX() + 32 + 3, this.getContentY() + 12, -8355712);
         if (this.minecraft.options.hideServerAddress) {
            p_282600_.drawString(this.minecraft.font, HIDDEN_ADDRESS_TEXT, this.getContentX() + 32 + 3, this.getContentY() + 12 + 11, -8355712);
         } else {
            p_282600_.drawString(this.minecraft.font, this.serverData.getAddress(), this.getContentX() + 32 + 3, this.getContentY() + 12 + 11, -8355712);
         }
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_424079_, boolean p_424379_) {
         if (p_424379_) {
            this.join();
         }

         return super.mouseClicked(p_424079_, p_424379_);
      }

      @Override
      public boolean keyPressed(KeyEvent p_425753_) {
         if (p_425753_.isSelection()) {
            this.join();
            return true;
         } else {
            return super.keyPressed(p_425753_);
         }
      }

      @Override
      public void join() {
         this.screen.join(new ServerData(this.serverData.getMotd(), this.serverData.getAddress(), ServerData.Type.LAN));
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.getServerNarration());
      }

      public Component getServerNarration() {
         return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
      }

      @Override
      boolean matches(ServerSelectionList.Entry p_427565_) {
         return p_427565_ instanceof ServerSelectionList.NetworkServerEntry serverselectionlist$networkserverentry
            && serverselectionlist$networkserverentry.serverData == this.serverData;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class OnlineServerEntry extends ServerSelectionList.Entry implements SelectableEntry {
      private static final int ICON_SIZE = 32;
      private static final int SPACING = 5;
      private static final int STATUS_ICON_WIDTH = 10;
      private static final int STATUS_ICON_HEIGHT = 8;
      private final JoinMultiplayerScreen screen;
      private final Minecraft minecraft;
      private final ServerData serverData;
      private final FaviconTexture icon;
      private byte @Nullable [] lastIconBytes;
      private @Nullable List<Component> onlinePlayersTooltip;
      private @Nullable Identifier statusIcon;
      private @Nullable Component statusIconTooltip;

      protected OnlineServerEntry(final JoinMultiplayerScreen p_99864_, final ServerData p_99865_) {
         this.screen = p_99864_;
         this.serverData = p_99865_;
         this.minecraft = Minecraft.getInstance();
         this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), p_99865_.ip);
         this.refreshStatus();
      }

      @Override
      public void renderContent(GuiGraphics p_425273_, int p_424268_, int p_430737_, boolean p_425246_, float p_422401_) {
         if (this.serverData.state() == ServerData.State.INITIAL) {
            this.serverData.setState(ServerData.State.PINGING);
            this.serverData.motd = CommonComponents.EMPTY;
            this.serverData.status = CommonComponents.EMPTY;
            ServerSelectionList.THREAD_POOL
               .submit(
                  () -> {
                     try {
                        this.screen
                           .getPinger()
                           .pingServer(
                              this.serverData,
                              () -> this.minecraft.execute(this::updateServerList),
                              () -> {
                                 this.serverData
                                    .setState(
                                       this.serverData.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                                          ? ServerData.State.SUCCESSFUL
                                          : ServerData.State.INCOMPATIBLE
                                    );
                                 this.minecraft.execute(this::refreshStatus);
                              },
                              EventLoopGroupHolder.remote(this.minecraft.options.useNativeTransport())
                           );
                     } catch (UnknownHostException unknownhostexception) {
                        this.serverData.setState(ServerData.State.UNREACHABLE);
                        this.serverData.motd = ServerSelectionList.CANT_RESOLVE_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                     } catch (Exception exception) {
                        this.serverData.setState(ServerData.State.UNREACHABLE);
                        this.serverData.motd = ServerSelectionList.CANT_CONNECT_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                     }
                  }
               );
         }

         p_425273_.drawString(this.minecraft.font, this.serverData.name, this.getContentX() + 32 + 3, this.getContentY() + 1, -1);
         List<FormattedCharSequence> list = this.minecraft.font.split(this.serverData.motd, this.getContentWidth() - 32 - 2);

         for (int i = 0; i < Math.min(list.size(), 2); i++) {
            p_425273_.drawString(this.minecraft.font, list.get(i), this.getContentX() + 32 + 3, this.getContentY() + 12 + 9 * i, -8355712);
         }

         this.drawIcon(p_425273_, this.getContentX(), this.getContentY(), this.icon.textureLocation());
         int k1 = ServerSelectionList.this.children().indexOf(this);
         if (this.serverData.state() == ServerData.State.PINGING) {
            int j = (int)(Util.getMillis() / 100L + k1 * 2 & 7L);
            if (j > 4) {
               j = 8 - j;
            }
            this.statusIcon = switch (j) {
               case 1 -> ServerSelectionList.PINGING_2_SPRITE;
               case 2 -> ServerSelectionList.PINGING_3_SPRITE;
               case 3 -> ServerSelectionList.PINGING_4_SPRITE;
               case 4 -> ServerSelectionList.PINGING_5_SPRITE;
               default -> ServerSelectionList.PINGING_1_SPRITE;
            };
         }

         int l1 = this.getContentRight() - 10 - 5;
         if (this.statusIcon != null) {
            p_425273_.blitSprite(RenderPipelines.GUI_TEXTURED, this.statusIcon, l1, this.getContentY(), 10, 8);
         }

         byte[] abyte = this.serverData.getIconBytes();
         if (!Arrays.equals(abyte, this.lastIconBytes)) {
            if (this.uploadServerIcon(abyte)) {
               this.lastIconBytes = abyte;
            } else {
               this.serverData.setIconBytes(null);
               this.updateServerList();
            }
         }

         Component component = this.serverData.state() == ServerData.State.INCOMPATIBLE
            ? this.serverData.version.copy().withStyle(ChatFormatting.RED)
            : this.serverData.status;
         int k = this.minecraft.font.width(component);
         int l = l1 - k - 5;
         p_425273_.drawString(this.minecraft.font, component, l, this.getContentY() + 1, -8355712);
         if (this.statusIconTooltip != null
            && p_424268_ >= l1
            && p_424268_ <= l1 + 10
            && p_430737_ >= this.getContentY()
            && p_430737_ <= this.getContentY() + 8) {
            p_425273_.setTooltipForNextFrame(this.statusIconTooltip, p_424268_, p_430737_);
         } else if (this.onlinePlayersTooltip != null
            && p_424268_ >= l
            && p_424268_ <= l + k
            && p_430737_ >= this.getContentY()
            && p_430737_ <= this.getContentY() - 1 + 9) {
            p_425273_.setTooltipForNextFrame(Lists.transform(this.onlinePlayersTooltip, Component::getVisualOrderText), p_424268_, p_430737_);
         }

         if (this.minecraft.options.touchscreen().get() || p_425246_) {
            p_425273_.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int i1 = p_424268_ - this.getContentX();
            int j1 = p_430737_ - this.getContentY();
            if (this.mouseOverRightHalf(i1, j1, 32)) {
               p_425273_.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.JOIN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
               ServerSelectionList.this.handleCursor(p_425273_);
            } else {
               p_425273_.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.JOIN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
            }

            if (k1 > 0) {
               if (this.mouseOverTopLeftQuarter(i1, j1, 32)) {
                  p_425273_.blitSprite(
                     RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                  );
                  ServerSelectionList.this.handleCursor(p_425273_);
               } else {
                  p_425273_.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_UP_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
               }
            }

            if (k1 < this.screen.getServers().size() - 1) {
               if (this.mouseOverBottomLeftQuarter(i1, j1, 32)) {
                  p_425273_.blitSprite(
                     RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                  );
                  ServerSelectionList.this.handleCursor(p_425273_);
               } else {
                  p_425273_.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_DOWN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
               }
            }
         }
      }

      private void refreshStatus() {
         this.onlinePlayersTooltip = null;
         switch (this.serverData.state()) {
            case INITIAL:
            case PINGING:
               this.statusIcon = ServerSelectionList.PING_1_SPRITE;
               this.statusIconTooltip = ServerSelectionList.PINGING_STATUS;
               break;
            case INCOMPATIBLE:
               this.statusIcon = ServerSelectionList.INCOMPATIBLE_SPRITE;
               this.statusIconTooltip = ServerSelectionList.INCOMPATIBLE_STATUS;
               this.onlinePlayersTooltip = this.serverData.playerList;
               break;
            case UNREACHABLE:
               this.statusIcon = ServerSelectionList.UNREACHABLE_SPRITE;
               this.statusIconTooltip = ServerSelectionList.NO_CONNECTION_STATUS;
               break;
            case SUCCESSFUL:
               if (this.serverData.ping < 150L) {
                  this.statusIcon = ServerSelectionList.PING_5_SPRITE;
               } else if (this.serverData.ping < 300L) {
                  this.statusIcon = ServerSelectionList.PING_4_SPRITE;
               } else if (this.serverData.ping < 600L) {
                  this.statusIcon = ServerSelectionList.PING_3_SPRITE;
               } else if (this.serverData.ping < 1000L) {
                  this.statusIcon = ServerSelectionList.PING_2_SPRITE;
               } else {
                  this.statusIcon = ServerSelectionList.PING_1_SPRITE;
               }

               this.statusIconTooltip = Component.translatable("multiplayer.status.ping", this.serverData.ping);
               this.onlinePlayersTooltip = this.serverData.playerList;
         }
      }

      public void updateServerList() {
         this.screen.getServers().save();
      }

      protected void drawIcon(GuiGraphics p_281338_, int p_283001_, int p_282834_, Identifier p_460851_) {
         p_281338_.blit(RenderPipelines.GUI_TEXTURED, p_460851_, p_283001_, p_282834_, 0.0F, 0.0F, 32, 32, 32, 32);
      }

      private boolean uploadServerIcon(byte @Nullable [] p_273176_) {
         if (p_273176_ == null) {
            this.icon.clear();
         } else {
            try {
               this.icon.upload(NativeImage.read(p_273176_));
            } catch (Throwable throwable) {
               ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", new Object[]{this.serverData.name, this.serverData.ip, throwable});
               return false;
            }
         }

         return true;
      }

      @Override
      public boolean keyPressed(KeyEvent p_426878_) {
         if (p_426878_.isSelection()) {
            this.join();
            return true;
         }

         if (p_426878_.hasShiftDown()) {
            ServerSelectionList serverselectionlist = this.screen.serverSelectionList;
            int i = serverselectionlist.children().indexOf(this);
            if (i == -1) {
               return true;
            }

            if (p_426878_.isDown() && i < this.screen.getServers().size() - 1 || p_426878_.isUp() && i > 0) {
               this.swap(i, p_426878_.isDown() ? i + 1 : i - 1);
               return true;
            }
         }

         return super.keyPressed(p_426878_);
      }

      @Override
      public void join() {
         this.screen.join(this.serverData);
      }

      private void swap(int p_99872_, int p_99873_) {
         this.screen.getServers().swap(p_99872_, p_99873_);
         this.screen.serverSelectionList.swap(p_99872_, p_99873_);
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_427078_, boolean p_424088_) {
         int i = (int)p_427078_.x() - this.getContentX();
         int j = (int)p_427078_.y() - this.getContentY();
         if (this.mouseOverRightHalf(i, j, 32)) {
            this.join();
            return true;
         }

         int k = this.screen.serverSelectionList.children().indexOf(this);
         if (k > 0 && this.mouseOverTopLeftQuarter(i, j, 32)) {
            this.swap(k, k - 1);
            return true;
         }

         if (k < this.screen.getServers().size() - 1 && this.mouseOverBottomLeftQuarter(i, j, 32)) {
            this.swap(k, k + 1);
            return true;
         }

         if (p_424088_) {
            this.join();
         }

         return super.mouseClicked(p_427078_, p_424088_);
      }

      public ServerData getServerData() {
         return this.serverData;
      }

      @Override
      public Component getNarration() {
         MutableComponent mutablecomponent = Component.empty();
         mutablecomponent.append(Component.translatable("narrator.select", this.serverData.name));
         mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
         switch (this.serverData.state()) {
            case PINGING:
               mutablecomponent.append(ServerSelectionList.PINGING_STATUS);
               break;
            case INCOMPATIBLE:
               mutablecomponent.append(ServerSelectionList.INCOMPATIBLE_STATUS);
               mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
               mutablecomponent.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
               mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
               mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
               break;
            case UNREACHABLE:
               mutablecomponent.append(ServerSelectionList.NO_CONNECTION_STATUS);
               break;
            default:
               mutablecomponent.append(ServerSelectionList.ONLINE_STATUS);
               mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
               mutablecomponent.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
               mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
               mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
               if (this.serverData.players != null) {
                  mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                  mutablecomponent.append(
                     Component.translatable("multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max())
                  );
                  mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                  mutablecomponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
               }
         }

         return mutablecomponent;
      }

      @Override
      public void close() {
         this.icon.close();
      }

      @Override
      boolean matches(ServerSelectionList.Entry p_431278_) {
         return p_431278_ instanceof ServerSelectionList.OnlineServerEntry serverselectionlist$onlineserverentry
            && serverselectionlist$onlineserverentry.serverData == this.serverData;
      }
   }
}
