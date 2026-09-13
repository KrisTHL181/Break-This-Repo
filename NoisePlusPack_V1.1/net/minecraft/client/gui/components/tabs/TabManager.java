package net.minecraft.client.gui.components.tabs;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TabManager {
   private final Consumer<AbstractWidget> addWidget;
   private final Consumer<AbstractWidget> removeWidget;
   private final Consumer<Tab> onSelected;
   private final Consumer<Tab> onDeselected;
   private @Nullable Tab currentTab;
   private @Nullable ScreenRectangle tabArea;

   public TabManager(Consumer<AbstractWidget> p_268279_, Consumer<AbstractWidget> p_268196_) {
      this(p_268279_, p_268196_, p_406722_ -> {}, p_408604_ -> {});
   }

   public TabManager(Consumer<AbstractWidget> p_406290_, Consumer<AbstractWidget> p_409721_, Consumer<Tab> p_408279_, Consumer<Tab> p_406687_) {
      this.addWidget = p_406290_;
      this.removeWidget = p_409721_;
      this.onSelected = p_408279_;
      this.onDeselected = p_406687_;
   }

   public void setTabArea(ScreenRectangle p_268042_) {
      this.tabArea = p_268042_;
      Tab tab = this.getCurrentTab();
      if (tab != null) {
         tab.doLayout(p_268042_);
      }
   }

   public void setCurrentTab(Tab p_276109_, boolean p_276120_) {
      if (!Objects.equals(this.currentTab, p_276109_)) {
         if (this.currentTab != null) {
            this.currentTab.visitChildren(this.removeWidget);
         }

         Tab tab = this.currentTab;
         this.currentTab = p_276109_;
         p_276109_.visitChildren(this.addWidget);
         if (this.tabArea != null) {
            p_276109_.doLayout(this.tabArea);
         }

         if (p_276120_) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

         this.onDeselected.accept(tab);
         this.onSelected.accept(this.currentTab);
      }
   }

   public @Nullable Tab getCurrentTab() {
      return this.currentTab;
   }
}
