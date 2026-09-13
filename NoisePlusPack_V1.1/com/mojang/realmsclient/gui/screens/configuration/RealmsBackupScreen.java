package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.backup");
   static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
   static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
   private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
   private static final Component DOWNLOAD_LATEST = Component.translatable("mco.backup.button.download");
   private static final String UPLOADED_KEY = "uploaded";
   private static final int PADDING = 8;
   public static final DateTimeFormatter SHORT_DATE_FORMAT = Util.localizedDateFormatter(FormatStyle.SHORT);
   final RealmsConfigureWorldScreen lastScreen;
   List<Backup> backups = Collections.emptyList();
   RealmsBackupScreen.@Nullable BackupObjectSelectionList backupList;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final int slotId;
   @Nullable Button downloadButton;
   final RealmsServer serverData;
   boolean noBackups = false;

   public RealmsBackupScreen(RealmsConfigureWorldScreen p_407560_, RealmsServer p_408887_, int p_407165_) {
      super(TITLE);
      this.lastScreen = p_407560_;
      this.serverData = p_408887_;
      this.slotId = p_407165_;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.backupList = this.layout.addToContents(new RealmsBackupScreen.BackupObjectSelectionList());
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.downloadButton = linearlayout.addChild(Button.builder(DOWNLOAD_LATEST, p_409070_ -> this.downloadClicked()).build());
      this.downloadButton.active = false;
      linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_408429_ -> this.onClose()).build());
      this.layout.visitWidgets(p_406978_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_406978_);
      });
      this.repositionElements();
      this.fetchRealmsBackups();
   }

   @Override
   public void render(GuiGraphics p_409181_, int p_407612_, int p_406342_, float p_407477_) {
      super.render(p_409181_, p_407612_, p_406342_, p_407477_);
      if (this.noBackups && this.backupList != null) {
         p_409181_.drawString(
            this.font,
            NO_BACKUPS_LABEL,
            this.width / 2 - this.font.width(NO_BACKUPS_LABEL) / 2,
            this.backupList.getY() + this.backupList.getHeight() / 2 - 4,
            -1
         );
      }
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.backupList != null) {
         this.backupList.updateSize(this.width, this.layout);
      }
   }

   private void fetchRealmsBackups() {
      (new Thread("Realms-fetch-backups") {
            @Override
            public void run() {
               RealmsClient realmsclient = RealmsClient.getOrCreate();

               try {
                  List<Backup> list = realmsclient.backupsFor(RealmsBackupScreen.this.serverData.id).backups();
                  RealmsBackupScreen.this.minecraft
                     .execute(
                        () -> {
                           RealmsBackupScreen.this.backups = list;
                           RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                           if (!RealmsBackupScreen.this.noBackups && RealmsBackupScreen.this.downloadButton != null) {
                              RealmsBackupScreen.this.downloadButton.active = true;
                           }

                           if (RealmsBackupScreen.this.backupList != null) {
                              RealmsBackupScreen.this.backupList
                                 .replaceEntries(
                                    RealmsBackupScreen.this.backups.stream().map(p_409880_ -> RealmsBackupScreen.this.new Entry(p_409880_)).toList()
                                 );
                           }
                        }
                     );
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsBackupScreen.LOGGER.error("Couldn't request backups", realmsserviceexception);
               }
            }
         })
         .start();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void downloadClicked() {
      this.minecraft
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this,
               Component.translatable("mco.configure.world.restore.download.question.line1"),
               p_408338_ -> this.minecraft
                  .setScreen(
                     new RealmsLongRunningMcoTaskScreen(
                        this.lastScreen.getNewScreen(),
                        new DownloadTask(
                           this.serverData.id,
                           this.slotId,
                           Objects.requireNonNullElse(this.serverData.name, "")
                              + " ("
                              + this.serverData.slots.get(this.serverData.activeSlot).options.getSlotName(this.serverData.activeSlot)
                              + ")",
                           this
                        )
                     )
                  )
            )
         );
   }

   @OnlyIn(Dist.CLIENT)
   class BackupObjectSelectionList extends ContainerObjectSelectionList<RealmsBackupScreen.Entry> {
      private static final int ITEM_HEIGHT = 36;

      public BackupObjectSelectionList() {
         super(
            Minecraft.getInstance(),
            RealmsBackupScreen.this.width,
            RealmsBackupScreen.this.layout.getContentHeight(),
            RealmsBackupScreen.this.layout.getHeaderHeight(),
            36
         );
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ContainerObjectSelectionList.Entry<RealmsBackupScreen.Entry> {
      private static final int Y_PADDING = 2;
      private final Backup backup;
      private @Nullable Button restoreButton;
      private @Nullable Button changesButton;
      private final List<AbstractWidget> children = new ArrayList<>();

      public Entry(final Backup p_410748_) {
         this.backup = p_410748_;
         this.populateChangeList(p_410748_);
         if (!p_410748_.changeList.isEmpty()) {
            this.changesButton = Button.builder(
                  RealmsBackupScreen.HAS_CHANGES_TOOLTIP,
                  p_410731_ -> RealmsBackupScreen.this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.backup))
               )
               .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
               .createNarration(this::narrationForBackupEntry)
               .build();
            this.children.add(this.changesButton);
         }

         if (!RealmsBackupScreen.this.serverData.expired) {
            this.restoreButton = Button.builder(RealmsBackupScreen.RESTORE_TOOLTIP, p_406942_ -> this.restoreClicked())
               .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
               .createNarration(this::narrationForBackupEntry)
               .build();
            this.children.add(this.restoreButton);
         }
      }

      private MutableComponent narrationForBackupEntry(Supplier<MutableComponent> p_452547_) {
         return CommonComponents.joinForNarration(
            Component.translatable("mco.backup.narration", RealmsBackupScreen.SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate())), p_452547_.get()
         );
      }

      private void populateChangeList(Backup p_407265_) {
         int i = RealmsBackupScreen.this.backups.indexOf(p_407265_);
         if (i != RealmsBackupScreen.this.backups.size() - 1) {
            Backup backup = RealmsBackupScreen.this.backups.get(i + 1);

            for (String s : p_407265_.metadata.keySet()) {
               if (!s.contains("uploaded") && backup.metadata.containsKey(s)) {
                  if (!p_407265_.metadata.get(s).equals(backup.metadata.get(s))) {
                     this.addToChangeList(s);
                  }
               } else {
                  this.addToChangeList(s);
               }
            }
         }
      }

      private void addToChangeList(String p_409547_) {
         if (p_409547_.contains("uploaded")) {
            String s = RealmsBackupScreen.SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate());
            this.backup.changeList.put(p_409547_, s);
            this.backup.uploadedVersion = true;
         } else {
            this.backup.changeList.put(p_409547_, this.backup.metadata.get(p_409547_));
         }
      }

      private void restoreClicked() {
         Component component = RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModified);
         String s = RealmsBackupScreen.SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate());
         Component component1 = Component.translatable("mco.configure.world.restore.question.line1", s, component);
         RealmsBackupScreen.this.minecraft
            .setScreen(
               RealmsPopups.warningPopupScreen(
                  RealmsBackupScreen.this,
                  component1,
                  p_407367_ -> {
                     RealmsConfigureWorldScreen realmsconfigureworldscreen = RealmsBackupScreen.this.lastScreen.getNewScreen();
                     RealmsBackupScreen.this.minecraft
                        .setScreen(
                           new RealmsLongRunningMcoTaskScreen(
                              realmsconfigureworldscreen, new RestoreTask(this.backup, RealmsBackupScreen.this.serverData.id, realmsconfigureworldscreen)
                           )
                        );
                  }
               )
            );
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      @Override
      public void renderContent(GuiGraphics p_430435_, int p_423131_, int p_424290_, boolean p_425940_, float p_427651_) {
         int i = this.getContentYMiddle();
         int j = i - 9 - 2;
         int k = i + 2;
         int l = this.backup.uploadedVersion ? -8388737 : -1;
         p_430435_.drawString(
            RealmsBackupScreen.this.font,
            Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModified)),
            this.getContentX(),
            j,
            l
         );
         p_430435_.drawString(
            RealmsBackupScreen.this.font, RealmsBackupScreen.SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate()), this.getContentX(), k, -11776948
         );
         int i1 = 0;
         int j1 = this.getContentYMiddle() - 10;
         if (this.restoreButton != null) {
            i1 += this.restoreButton.getWidth() + 8;
            this.restoreButton.setX(this.getContentRight() - i1);
            this.restoreButton.setY(j1);
            this.restoreButton.render(p_430435_, p_423131_, p_424290_, p_427651_);
         }

         if (this.changesButton != null) {
            i1 += this.changesButton.getWidth() + 8;
            this.changesButton.setX(this.getContentRight() - i1);
            this.changesButton.setY(j1);
            this.changesButton.render(p_430435_, p_423131_, p_424290_, p_427651_);
         }
      }
   }
}
