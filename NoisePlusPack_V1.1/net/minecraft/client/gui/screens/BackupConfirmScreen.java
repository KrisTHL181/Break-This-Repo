package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BackupConfirmScreen extends Screen {
   private static final Component SKIP_AND_JOIN = Component.translatable("selectWorld.backupJoinSkipButton");
   public static final Component BACKUP_AND_JOIN = Component.translatable("selectWorld.backupJoinConfirmButton");
   private final Runnable onCancel;
   protected final BackupConfirmScreen.Listener onProceed;
   private final Component description;
   private final boolean promptForCacheErase;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   final Component confirmation;
   protected int id;
   private Checkbox eraseCache;

   public BackupConfirmScreen(Runnable p_309995_, BackupConfirmScreen.Listener p_95544_, Component p_95545_, Component p_95546_, boolean p_95547_) {
      this(p_309995_, p_95544_, p_95545_, p_95546_, BACKUP_AND_JOIN, p_95547_);
   }

   public BackupConfirmScreen(
      Runnable p_364937_, BackupConfirmScreen.Listener p_370062_, Component p_361532_, Component p_369162_, Component p_369010_, boolean p_363018_
   ) {
      super(p_361532_);
      this.onCancel = p_364937_;
      this.onProceed = p_370062_;
      this.description = p_369162_;
      this.promptForCacheErase = p_363018_;
      this.confirmation = p_369010_;
   }

   @Override
   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.description, this.width - 50);
      int i = (this.message.getLineCount() + 1) * 9;
      this.eraseCache = Checkbox.builder(Component.translatable("selectWorld.backupEraseCache").withColor(-2039584), this.font)
         .pos(this.width / 2 - 155 + 80, 76 + i)
         .build();
      if (this.promptForCacheErase) {
         this.addRenderableWidget(this.eraseCache);
      }

      this.addRenderableWidget(
         Button.builder(this.confirmation, p_308190_ -> this.onProceed.proceed(true, this.eraseCache.selected()))
            .bounds(this.width / 2 - 155, 100 + i, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(SKIP_AND_JOIN, p_308188_ -> this.onProceed.proceed(false, this.eraseCache.selected()))
            .bounds(this.width / 2 - 155 + 160, 100 + i, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_308189_ -> this.onCancel.run()).bounds(this.width / 2 - 155 + 80, 124 + i, 150, 20).build()
      );
   }

   @Override
   public void render(GuiGraphics p_282759_, int p_282356_, int p_282725_, float p_281518_) {
      super.render(p_282759_, p_282356_, p_282725_, p_281518_);
      ActiveTextCollector activetextcollector = p_282759_.textRenderer();
      p_282759_.drawCenteredString(this.font, this.title, this.width / 2, 50, -1);
      this.message.visitLines(TextAlignment.CENTER, this.width / 2, 70, 9, activetextcollector);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean keyPressed(KeyEvent p_422565_) {
      if (p_422565_.key() == 256) {
         this.onCancel.run();
         return true;
      } else {
         return super.keyPressed(p_422565_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Listener {
      void proceed(boolean var1, boolean var2);
   }
}
