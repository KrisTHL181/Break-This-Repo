package net.minecraft.client.gui.screens.worldselection;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class EditWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName").withStyle(ChatFormatting.GRAY);
   private static final Component RESET_ICON_BUTTON = Component.translatable("selectWorld.edit.resetIcon");
   private static final Component FOLDER_BUTTON = Component.translatable("selectWorld.edit.openFolder");
   private static final Component BACKUP_BUTTON = Component.translatable("selectWorld.edit.backup");
   private static final Component BACKUP_FOLDER_BUTTON = Component.translatable("selectWorld.edit.backupFolder");
   private static final Component OPTIMIZE_BUTTON = Component.translatable("selectWorld.edit.optimize");
   private static final Component OPTIMIZE_TITLE = Component.translatable("optimizeWorld.confirm.title");
   private static final Component OPTIMIIZE_DESCRIPTION = Component.translatable("optimizeWorld.confirm.description");
   private static final Component OPTIMIIZE_CONFIRMATION = Component.translatable("optimizeWorld.confirm.proceed");
   private static final Component SAVE_BUTTON = Component.translatable("selectWorld.edit.save");
   private static final int DEFAULT_WIDTH = 200;
   private static final int VERTICAL_SPACING = 4;
   private static final int HALF_WIDTH = 98;
   private final LinearLayout layout = LinearLayout.vertical().spacing(5);
   private final BooleanConsumer callback;
   private final LevelStorageSource.LevelStorageAccess levelAccess;
   private final EditBox nameEdit;

   public static EditWorldScreen create(Minecraft p_312937_, LevelStorageSource.LevelStorageAccess p_310908_, BooleanConsumer p_311675_) throws IOException {
      LevelSummary levelsummary = p_310908_.getSummary(p_310908_.getDataTag());
      return new EditWorldScreen(p_312937_, p_310908_, levelsummary.getLevelName(), p_311675_);
   }

   private EditWorldScreen(Minecraft p_309397_, LevelStorageSource.LevelStorageAccess p_101253_, String p_312996_, BooleanConsumer p_101252_) {
      super(Component.translatable("selectWorld.edit.title"));
      this.callback = p_101252_;
      this.levelAccess = p_101253_;
      Font font = p_309397_.font;
      this.layout.addChild(new SpacerElement(200, 20));
      this.layout.addChild(new StringWidget(NAME_LABEL, font));
      this.nameEdit = this.layout.addChild(new EditBox(font, 200, 20, NAME_LABEL));
      this.nameEdit.setValue(p_312996_);
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
      Button button = linearlayout.addChild(Button.builder(SAVE_BUTTON, p_325437_ -> this.onRename(this.nameEdit.getValue())).width(98).build());
      linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_308233_ -> this.onClose()).width(98).build());
      this.nameEdit.setResponder(p_325436_ -> button.active = !StringUtil.isBlank(p_325436_));
      this.layout.addChild(Button.builder(RESET_ICON_BUTTON, p_308218_ -> {
         p_101253_.getIconFile().ifPresent(p_182594_ -> FileUtils.deleteQuietly(p_182594_.toFile()));
         p_308218_.active = false;
      }).width(200).build()).active = p_101253_.getIconFile().filter(p_182587_ -> Files.isRegularFile(p_182587_)).isPresent();
      this.layout
         .addChild(Button.builder(FOLDER_BUTTON, p_448101_ -> Util.getPlatform().openPath(p_101253_.getLevelPath(LevelResource.ROOT))).width(200).build());
      this.layout.addChild(Button.builder(BACKUP_BUTTON, p_308216_ -> {
         boolean flag = makeBackupAndShowToast(p_101253_);
         this.callback.accept(!flag);
      }).width(200).build());
      this.layout.addChild(Button.builder(BACKUP_FOLDER_BUTTON, p_448099_ -> {
         LevelStorageSource levelstoragesource = p_309397_.getLevelSource();
         Path path = levelstoragesource.getBackupPath();

         try {
            FileUtil.createDirectoriesSafe(path);
         } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
         }

         Util.getPlatform().openPath(path);
      }).width(200).build());
      this.layout
         .addChild(
            Button.builder(
                  OPTIMIZE_BUTTON, p_357737_ -> p_309397_.setScreen(new BackupConfirmScreen(() -> p_309397_.setScreen(this), (p_308228_, p_308229_) -> {
                     if (p_308228_) {
                        makeBackupAndShowToast(p_101253_);
                     }

                     p_309397_.setScreen(OptimizeWorldScreen.create(p_309397_, this.callback, p_309397_.getFixerUpper(), p_101253_, p_308229_));
                  }, OPTIMIZE_TITLE, OPTIMIIZE_DESCRIPTION, OPTIMIIZE_CONFIRMATION, true))
               )
               .width(200)
               .build()
         );
      this.layout.addChild(new SpacerElement(200, 20));
      this.layout.addChild(linearlayout);
      this.layout.visitWidgets(p_325434_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325434_);
      });
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.nameEdit);
   }

   @Override
   protected void init() {
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public boolean keyPressed(KeyEvent p_425189_) {
      if (this.nameEdit.isFocused() && p_425189_.isConfirmation()) {
         this.onRename(this.nameEdit.getValue());
         this.onClose();
         return true;
      } else {
         return super.keyPressed(p_425189_);
      }
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   private void onRename(String p_312476_) {
      try {
         this.levelAccess.renameLevel(p_312476_);
      } catch (NbtException | ReportedNbtException | IOException ioexception) {
         LOGGER.error("Failed to access world '{}'", this.levelAccess.getLevelId(), ioexception);
         SystemToast.onWorldAccessFailure(this.minecraft, this.levelAccess.getLevelId());
      }

      this.callback.accept(true);
   }

   public static boolean makeBackupAndShowToast(LevelStorageSource.LevelStorageAccess p_101259_) {
      long i = 0L;
      IOException ioexception = null;

      try {
         i = p_101259_.makeWorldBackup();
      } catch (IOException ioexception1) {
         ioexception = ioexception1;
      }

      if (ioexception != null) {
         Component component2 = Component.translatable("selectWorld.edit.backupFailed");
         Component component3 = Component.literal(ioexception.getMessage());
         Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToast.SystemToastId.WORLD_BACKUP, component2, component3));
         return false;
      } else {
         Component component = Component.translatable("selectWorld.edit.backupCreated", p_101259_.getLevelId());
         Component component1 = Component.translatable("selectWorld.edit.backupSize", Mth.ceil(i / 1048576.0));
         Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToast.SystemToastId.WORLD_BACKUP, component, component1));
         return true;
      }
   }

   @Override
   public void render(GuiGraphics p_281742_, int p_101265_, int p_101266_, float p_101267_) {
      super.render(p_281742_, p_101265_, p_101266_, p_101267_);
      p_281742_.drawCenteredString(this.font, this.title, this.width / 2, 15, -1);
   }
}
