package net.minecraft.client.gui.screens.options;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class OptionsSubScreen extends Screen {
   protected final Screen lastScreen;
   protected final Options options;
   protected @Nullable OptionsList list;
   public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   public OptionsSubScreen(Screen p_344485_, Options p_344051_, Component p_343770_) {
      super(p_343770_);
      this.lastScreen = p_344485_;
      this.options = p_344051_;
   }

   @Override
   protected void init() {
      this.addTitle();
      this.addContents();
      this.addFooter();
      this.layout.visitWidgets(p_344531_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_344531_);
      });
      this.repositionElements();
   }

   protected void addTitle() {
      this.layout.addTitleHeader(this.title, this.font);
   }

   protected void addContents() {
      this.list = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this));
      this.addOptions();
      if (this.list.findOption(this.options.narrator()) instanceof CycleButton cyclebutton) {
         this.narratorButton = cyclebutton;
         this.narratorButton.active = this.minecraft.getNarrator().isActive();
      }
   }

   protected abstract void addOptions();

   protected void addFooter() {
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_343150_ -> this.onClose()).width(200).build());
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.list != null) {
         this.list.updateSize(this.width, this.layout);
      }
   }

   @Override
   public void removed() {
      this.minecraft.options.save();
   }

   @Override
   public void onClose() {
      if (this.list != null) {
         this.list.applyUnsavedChanges();
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   public void resetOption(OptionInstance<?> p_450871_) {
      if (this.list != null) {
         this.list.resetOption(p_450871_);
      }
   }
}
