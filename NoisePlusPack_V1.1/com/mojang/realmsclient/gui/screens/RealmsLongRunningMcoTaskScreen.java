package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.util.task.LongRunningTask;
import java.time.Duration;
import java.util.List;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.realms.RepeatedNarrator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsLongRunningMcoTaskScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RepeatedNarrator REPEATED_NARRATOR = new RepeatedNarrator(Duration.ofSeconds(5L));
   private final List<LongRunningTask> queuedTasks;
   private final Screen lastScreen;
   protected final LinearLayout layout = LinearLayout.vertical();
   private volatile Component title;
   private @Nullable LoadingDotsWidget loadingDotsWidget;

   public RealmsLongRunningMcoTaskScreen(Screen p_88777_, LongRunningTask... p_309789_) {
      super(GameNarrator.NO_TITLE);
      this.lastScreen = p_88777_;
      this.queuedTasks = List.of(p_309789_);
      if (this.queuedTasks.isEmpty()) {
         throw new IllegalArgumentException("No tasks added");
      }

      this.title = this.queuedTasks.get(0).getTitle();
      Runnable runnable = () -> {
         for (LongRunningTask longrunningtask : p_309789_) {
            this.setTitle(longrunningtask.getTitle());
            if (longrunningtask.aborted()) {
               break;
            }

            longrunningtask.run();
            if (longrunningtask.aborted()) {
               return;
            }
         }
      };
      Thread thread = new Thread(runnable, "Realms-long-running-task");
      thread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   @Override
   public boolean canInterruptWithAnotherScreen() {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.loadingDotsWidget != null) {
         REPEATED_NARRATOR.narrate(this.minecraft.getNarrator(), this.loadingDotsWidget.getMessage());
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_424541_) {
      if (p_424541_.key() == 256) {
         this.cancel();
         return true;
      } else {
         return super.keyPressed(p_424541_);
      }
   }

   @Override
   public void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild(realmsLogo());
      this.loadingDotsWidget = new LoadingDotsWidget(this.font, this.title);
      this.layout.addChild(this.loadingDotsWidget, p_404761_ -> p_404761_.paddingTop(10).paddingBottom(30));
      this.layout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_296059_ -> this.cancel()).build());
      this.layout.visitWidgets(p_325132_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325132_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   protected void cancel() {
      for (LongRunningTask longrunningtask : this.queuedTasks) {
         longrunningtask.abortTask();
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   public void setTitle(Component p_88797_) {
      if (this.loadingDotsWidget != null) {
         this.loadingDotsWidget.setMessage(p_88797_);
      }

      this.title = p_88797_;
   }
}
