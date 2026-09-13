package net.minecraft.client.gui.screens.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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
   private Button selectButton;
   private Button deleteButton;
   private ServerData editingServer;
   private LanServerDetection.LanServerList lanServerList;
   private LanServerDetection.@Nullable LanServerDetector lanServerDetector;

   public JoinMultiplayerScreen(Screen p_99688_) {
      super(Component.translatable("multiplayer.title"));
      this.lastScreen = p_99688_;
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
      } catch (Exception exception) {
         LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
      }

      this.serverSelectionList = this.layout
         .addToContents(new ServerSelectionList(this, this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight(), 36));
      this.serverSelectionList.updateOnlineServers(this.servers);
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal().spacing(4));
      LinearLayout linearlayout2 = linearlayout.addChild(LinearLayout.horizontal().spacing(4));
      this.selectButton = linearlayout1.addChild(Button.builder(Component.translatable("selectServer.select"), p_420759_ -> {
         ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
         if (serverselectionlist$entry != null) {
            serverselectionlist$entry.join();
         }
      }).width(100).build());
      linearlayout1.addChild(Button.builder(Component.translatable("selectServer.direct"), p_296191_ -> {
         this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
         this.minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
      }).width(100).build());
      linearlayout1.addChild(Button.builder(Component.translatable("selectServer.add"), p_420757_ -> {
         this.editingServer = new ServerData("", "", ServerData.Type.OTHER);
         this.minecraft.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), this::addServerCallback, this.editingServer));
      }).width(100).build());
      this.editButton = linearlayout2.addChild(
         Button.builder(
               Component.translatable("selectServer.edit"),
               p_420758_ -> {
                  ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
                  if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
                     ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
                     this.editingServer = new ServerData(serverdata.name, serverdata.ip, ServerData.Type.OTHER);
                     this.editingServer.copyFrom(serverdata);
                     this.minecraft
                        .setScreen(
                           new ManageServerScreen(this, Component.translatable("manageServer.edit.title"), this::editServerCallback, this.editingServer)
                        );
                  }
               }
            )
            .width(74)
            .build()
      );
      this.deleteButton = linearlayout2.addChild(Button.builder(Component.translatable("selectServer.delete"), p_99710_ -> {
         ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
         if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
            String s = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData().name;
            if (s != null) {
               Component component = Component.translatable("selectServer.deleteQuestion");
               Component component1 = Component.translatable("selectServer.deleteWarning", s);
               Component component2 = Component.translatable("selectServer.deleteButton");
               Component component3 = CommonComponents.GUI_CANCEL;
               this.minecraft.setScreen(new ConfirmScreen(this::deleteCallback, component, component1, component2, component3));
            }
         }
      }).width(74).build());
      linearlayout2.addChild(Button.builder(Component.translatable("selectServer.refresh"), p_99706_ -> this.refreshServerList()).width(74).build());
      linearlayout2.addChild(Button.builder(CommonComponents.GUI_BACK, p_325384_ -> this.onClose()).width(74).build());
      this.layout.visitWidgets(p_420761_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_420761_);
      });
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
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void tick() {
      super.tick();
      List<LanServer> list = this.lanServerList.takeDirtyServers();
      if (list != null) {
         this.serverSelectionList.updateNetworkServers(list);
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
      this.minecraft.setScreen(new JoinMultiplayerScreen(this.lastScreen));
   }

   private void deleteCallback(boolean p_99712_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (p_99712_ && serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         this.servers.remove(((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData());
         this.servers.save();
         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void editServerCallback(boolean p_99717_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (p_99717_ && serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
         serverdata.name = this.editingServer.name;
         serverdata.ip = this.editingServer.ip;
         serverdata.copyFrom(this.editingServer);
         this.servers.save();
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void addServerCallback(boolean p_99722_) {
      if (p_99722_) {
         ServerData serverdata = this.servers.unhide(this.editingServer.ip);
         if (serverdata != null) {
            serverdata.copyNameIconFrom(this.editingServer);
            this.servers.save();
         } else {
            this.servers.add(this.editingServer, false);
            this.servers.save();
         }

         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void directJoinCallback(boolean p_99726_) {
      if (p_99726_) {
         ServerData serverdata = this.servers.get(this.editingServer.ip);
         if (serverdata == null) {
            this.servers.add(this.editingServer, true);
            this.servers.save();
            this.join(this.editingServer);
         } else {
            this.join(serverdata);
         }
      } else {
         this.minecraft.setScreen(this);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_426573_) {
      if (super.keyPressed(p_426573_)) {
         return true;
      } else if (p_426573_.key() == 294) {
         this.refreshServerList();
         return true;
      } else {
         return false;
      }
   }

   public void join(ServerData p_99703_) {
      ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(p_99703_.ip), p_99703_, false, null);
   }

   protected void onSelectedChange() {
      this.selectButton.active = false;
      this.editButton.active = false;
      this.deleteButton.active = false;
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (serverselectionlist$entry != null && !(serverselectionlist$entry instanceof ServerSelectionList.LANHeader)) {
         this.selectButton.active = true;
         if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
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
