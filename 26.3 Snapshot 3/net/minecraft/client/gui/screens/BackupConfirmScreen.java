package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class BackupConfirmScreen extends Screen {
   private static final Component SKIP_AND_JOIN = Component.translatable("selectWorld.backupJoinSkipButton");
   public static final Component BACKUP_AND_JOIN = Component.translatable("selectWorld.backupJoinConfirmButton");
   private final Runnable onCancel;
   protected final BackupConfirmScreen.Listener onProceed;
   private final Component description;
   private final boolean promptForCacheErase;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private final Component confirmation;
   protected int id;
   private Checkbox eraseCache;

   public BackupConfirmScreen(
      final Runnable onCancel,
      final BackupConfirmScreen.Listener onProceed,
      final Component title,
      final Component description,
      final boolean promptForCacheErase
   ) {
      this(onCancel, onProceed, title, description, BACKUP_AND_JOIN, promptForCacheErase);
   }

   public BackupConfirmScreen(
      final Runnable onCancel,
      final BackupConfirmScreen.Listener onProceed,
      final Component title,
      final Component description,
      final Component confirmation,
      final boolean promptForCacheErase
   ) {
      super(title);
      this.onCancel = onCancel;
      this.onProceed = onProceed;
      this.description = description;
      this.promptForCacheErase = promptForCacheErase;
      this.confirmation = confirmation;
   }

   @Override
   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.description, this.width - 50);
      int textSize = (this.message.getLineCount() + 1) * 9;
      this.eraseCache = Checkbox.builder(Component.translatable("selectWorld.backupEraseCache").withColor(-2039584), this.font)
         .pos(this.width / 2 - 155 + 80, 76 + textSize)
         .build();
      if (this.promptForCacheErase) {
         this.addRenderableWidget(this.eraseCache);
      }

      this.addRenderableWidget(
         Button.builder(this.confirmation, button -> this.onProceed.proceed(true, this.eraseCache.selected()))
            .bounds(this.width / 2 - 155, 100 + textSize, 150, 20)
            .build()
      );
      Button skipAndJoinButton = Button.builder(SKIP_AND_JOIN, button -> this.onProceed.proceed(false, this.eraseCache.selected()))
         .bounds(this.width / 2 - 155 + 160, 100 + textSize, 150, 20)
         .build();
      this.addRenderableWidget(skipAndJoinButton);
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel.run()).bounds(this.width / 2 - 155 + 80, 124 + textSize, 150, 20).build()
      );
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      ActiveTextCollector textRenderer = graphics.textRenderer();
      graphics.centeredText(this.font, this.title, this.width / 2, 50, -1);
      this.message.visitLines(TextAlignment.CENTER, this.width / 2, 70, 9, textRenderer);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (event.isEscape()) {
         this.onCancel.run();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   public interface Listener {
      void proceed(final boolean backup, final boolean eraseCache);
   }
}
