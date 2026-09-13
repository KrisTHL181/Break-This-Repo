package net.minecraft.client.gui.screens.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JoinMultiplayerScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int TOP_ROW_BUTTON_WIDTH = 100;
   private static final int LOWER_ROW_BUTTON_WIDTH = 74;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 60);
   private final ServerStatusPinger pinger = new ServerStatusPinger();
   private final Screen lastScreen;
   protected ServerSelectionList serverSelectionList;
   private ServerList servers;
   private Button editButton;
   private Button joinButton;
   private Button deleteButton;
   private ServerData editingServer;
   private LanServerDetection.LanServerList lanServerList;
   private LanServerDetection.@Nullable LanServerDetector lanServerDetector;

   public JoinMultiplayerScreen(final Screen lastScreen) {
      super(Component.translatable("multiplayer.title"));
      this.lastScreen = lastScreen;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(this.title, this.font);
      this.servers = new ServerList(this.minecraft);
      this.servers.load();
      this.lanServerList = new LanServerDetection.LanServerList();

      try {
         this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
         this.lanServerDetector.start();
      } catch (Exception e) {
         LOGGER.warn("Unable to start LAN server detection", e);
      }

      this.serverSelectionList = this.layout
         .addToContents(new ServerSelectionList(this, this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight(), 36));
      this.serverSelectionList.updateOnlineServers(this.servers);
      LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
      footer.defaultCellSetting().alignHorizontallyCenter();
      LinearLayout topFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(4));
      LinearLayout bottomFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(4));
      this.joinButton = topFooterButtons.addChild(Button.builder(Component.translatable("selectServer.select"), var1x -> {
         ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
         if (entry != null) {
            entry.join();
         }
      }).width(100).build());
      topFooterButtons.addChild(Button.builder(Component.translatable("selectServer.direct"), var1x -> {
         this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
         this.minecraft.gui.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
      }).width(100).build());
      topFooterButtons.addChild(
         Button.builder(
               Component.translatable("selectServer.add"),
               var1x -> {
                  this.editingServer = new ServerData("", "", ServerData.Type.OTHER);
                  this.minecraft
                     .gui
                     .setScreen(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), this::addServerCallback, this.editingServer));
               }
            )
            .width(100)
            .build()
      );
      this.editButton = bottomFooterButtons.addChild(
         Button.builder(
               Component.translatable("selectServer.edit"),
               var1x -> {
                  ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
                  if (entry instanceof ServerSelectionList.OnlineServerEntry onlineServerEntry) {
                     ServerData current = onlineServerEntry.getServerData();
                     this.editingServer = new ServerData(current.name, current.ip, ServerData.Type.OTHER);
                     this.editingServer.copyFrom(current);
                     this.minecraft
                        .gui
                        .setScreen(
                           new ManageServerScreen(this, Component.translatable("manageServer.edit.title"), this::editServerCallback, this.editingServer)
                        );
                  }
               }
            )
            .width(74)
            .build()
      );
      this.deleteButton = bottomFooterButtons.addChild(Button.builder(Component.translatable("selectServer.delete"), var1x -> {
         ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
         if (entry instanceof ServerSelectionList.OnlineServerEntry onlineServerEntry) {
            String serverName = onlineServerEntry.getServerData().name;
            if (serverName != null) {
               Component title = Component.translatable("selectServer.deleteQuestion");
               Component warning = Component.translatable("selectServer.deleteWarning", serverName);
               Component yes = Component.translatable("selectServer.deleteButton");
               Component no = CommonComponents.GUI_CANCEL;
               this.minecraft.gui.setScreen(new ConfirmScreen(this::deleteCallback, title, warning, yes, no));
            }
         }
      }).width(74).build());
      bottomFooterButtons.addChild(Button.builder(Component.translatable("selectServer.refresh"), var1x -> this.refreshServerList()).width(74).build());
      bottomFooterButtons.addChild(Button.builder(CommonComponents.GUI_BACK, var1x -> this.onClose()).width(74).build());
      JoinMultiplayerScreen var4 = this;
      this.layout.visitWidgets(x$0 -> var4.addRenderableWidget(x$0));
      this.repositionElements();
      this.onSelectedChange();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.serverSelectionList != null) {
         this.serverSelectionList.updateSize(this.width, this.layout);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   @Override
   public void tick() {
      super.tick();
      List<LanServer> lanServers = this.lanServerList.takeDirtyServers();
      if (lanServers != null) {
         this.serverSelectionList.updateNetworkServers(lanServers);
      }

      this.pinger.tick();
   }

   @Override
   public void removed() {
      if (this.lanServerDetector != null) {
         this.lanServerDetector.interrupt();
         this.lanServerDetector = null;
      }

      this.pinger.removeAll();
      this.serverSelectionList.removed();
   }

   private void refreshServerList() {
      this.minecraft.gui.setScreen(new JoinMultiplayerScreen(this.lastScreen));
   }

   private void deleteCallback(final boolean result) {
      ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
      if (result && entry instanceof ServerSelectionList.OnlineServerEntry onlineServerEntry) {
         this.servers.remove(onlineServerEntry.getServerData());
         this.servers.save();
         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.gui.setScreen(this);
   }

   private void editServerCallback(final boolean result) {
      ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
      if (result && entry instanceof ServerSelectionList.OnlineServerEntry onlineServerEntry) {
         ServerData current = onlineServerEntry.getServerData();
         current.name = this.editingServer.name;
         current.ip = this.editingServer.ip;
         current.copyFrom(this.editingServer);
         this.servers.save();
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.gui.setScreen(this);
   }

   private void addServerCallback(final boolean result) {
      if (result) {
         ServerData serverData = this.servers.unhide(this.editingServer.ip);
         if (serverData != null) {
            serverData.copyNameIconFrom(this.editingServer);
            this.servers.save();
         } else {
            this.servers.add(this.editingServer, false);
            this.servers.save();
         }

         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.gui.setScreen(this);
   }

   private void directJoinCallback(final boolean result) {
      if (result) {
         ServerData serverData = this.servers.get(this.editingServer.ip);
         if (serverData == null) {
            this.servers.add(this.editingServer, true);
            this.servers.save();
            this.join(this.editingServer);
         } else {
            this.join(serverData);
         }
      } else {
         this.minecraft.gui.setScreen(this);
      }
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (super.keyPressed(event)) {
         return true;
      } else if (event.key() == 294) {
         this.refreshServerList();
         return true;
      } else {
         return false;
      }
   }

   public void join(final ServerData data) {
      ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(data.ip), data, false, null);
   }

   protected void onSelectedChange() {
      this.joinButton.active = false;
      this.editButton.active = false;
      this.deleteButton.active = false;
      ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
      if (entry != null && !(entry instanceof ServerSelectionList.LANHeader)) {
         this.joinButton.active = true;
         if (entry instanceof ServerSelectionList.OnlineServerEntry) {
            this.editButton.active = true;
            this.deleteButton.active = true;
         }
      }
   }

   public ServerStatusPinger getPinger() {
      return this.pinger;
   }

   public ServerList getServers() {
      return this.servers;
   }
}
