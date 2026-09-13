package net.minecraft.client.gui.screens.worldselection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
class SwitchGrid {
   private static final int DEFAULT_SWITCH_BUTTON_WIDTH = 44;
   private final List<SwitchGrid.LabeledSwitch> switches;
   private final Layout layout;

   SwitchGrid(List<SwitchGrid.LabeledSwitch> p_268257_, Layout p_377885_) {
      this.switches = p_268257_;
      this.layout = p_377885_;
   }

   public Layout layout() {
      return this.layout;
   }

   public void refreshStates() {
      this.switches.forEach(SwitchGrid.LabeledSwitch::refreshState);
   }

   public static SwitchGrid.Builder builder(int p_268344_) {
      return new SwitchGrid.Builder(p_268344_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      final int width;
      private final List<SwitchGrid.SwitchBuilder> switchBuilders = new ArrayList<>();
      int paddingLeft;
      int rowSpacing = 4;
      int rowCount;
      Optional<SwitchGrid.InfoUnderneathSettings> infoUnderneath = Optional.empty();

      public Builder(int p_267987_) {
         this.width = p_267987_;
      }

      void increaseRow() {
         this.rowCount++;
      }

      public SwitchGrid.SwitchBuilder addSwitch(Component p_268004_, BooleanSupplier p_268017_, Consumer<Boolean> p_268320_) {
         SwitchGrid.SwitchBuilder switchgrid$switchbuilder = new SwitchGrid.SwitchBuilder(p_268004_, p_268017_, p_268320_, 44);
         this.switchBuilders.add(switchgrid$switchbuilder);
         return switchgrid$switchbuilder;
      }

      public SwitchGrid.Builder withPaddingLeft(int p_267998_) {
         this.paddingLeft = p_267998_;
         return this;
      }

      public SwitchGrid.Builder withRowSpacing(int p_270750_) {
         this.rowSpacing = p_270750_;
         return this;
      }

      public SwitchGrid build() {
         GridLayout gridlayout = new GridLayout().rowSpacing(this.rowSpacing);
         gridlayout.addChild(SpacerElement.width(this.width - 44), 0, 0);
         gridlayout.addChild(SpacerElement.width(44), 0, 1);
         List<SwitchGrid.LabeledSwitch> list = new ArrayList<>();
         this.rowCount = 0;

         for (SwitchGrid.SwitchBuilder switchgrid$switchbuilder : this.switchBuilders) {
            list.add(switchgrid$switchbuilder.build(this, gridlayout, 0));
         }

         gridlayout.arrangeElements();
         SwitchGrid switchgrid = new SwitchGrid(list, gridlayout);
         switchgrid.refreshStates();
         return switchgrid;
      }

      public SwitchGrid.Builder withInfoUnderneath(int p_270730_, boolean p_270594_) {
         this.infoUnderneath = Optional.of(new SwitchGrid.InfoUnderneathSettings(p_270730_, p_270594_));
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record InfoUnderneathSettings(int maxInfoRows, boolean alwaysMaxHeight) {
   }

   @OnlyIn(Dist.CLIENT)
   record LabeledSwitch(CycleButton<Boolean> button, BooleanSupplier stateSupplier, @Nullable BooleanSupplier isActiveCondition) {
      public void refreshState() {
         this.button.setValue(this.stateSupplier.getAsBoolean());
         if (this.isActiveCondition != null) {
            this.button.active = this.isActiveCondition.getAsBoolean();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SwitchBuilder {
      private final Component label;
      private final BooleanSupplier stateSupplier;
      private final Consumer<Boolean> onClicked;
      private @Nullable Component info;
      private @Nullable BooleanSupplier isActiveCondition;
      private final int buttonWidth;

      SwitchBuilder(Component p_268282_, BooleanSupplier p_268294_, Consumer<Boolean> p_268132_, int p_268250_) {
         this.label = p_268282_;
         this.stateSupplier = p_268294_;
         this.onClicked = p_268132_;
         this.buttonWidth = p_268250_;
      }

      public SwitchGrid.SwitchBuilder withIsActiveCondition(BooleanSupplier p_267966_) {
         this.isActiveCondition = p_267966_;
         return this;
      }

      public SwitchGrid.SwitchBuilder withInfo(Component p_268240_) {
         this.info = p_268240_;
         return this;
      }

      SwitchGrid.LabeledSwitch build(SwitchGrid.Builder p_270513_, GridLayout p_271004_, int p_270506_) {
         p_270513_.increaseRow();
         StringWidget stringwidget = new StringWidget(this.label, Minecraft.getInstance().font);
         p_271004_.addChild(stringwidget, p_270513_.rowCount, p_270506_, p_271004_.newCellSettings().align(0.0F, 0.5F).paddingLeft(p_270513_.paddingLeft));
         Optional<SwitchGrid.InfoUnderneathSettings> optional = p_270513_.infoUnderneath;
         CycleButton.Builder<Boolean> builder = CycleButton.onOffBuilder(this.stateSupplier.getAsBoolean());
         builder.displayOnlyValue();
         boolean flag = this.info != null && optional.isEmpty();
         if (flag) {
            Tooltip tooltip = Tooltip.create(this.info);
            builder.withTooltip(p_269644_ -> tooltip);
         }

         if (this.info != null && !flag) {
            builder.withCustomNarration(p_269645_ -> CommonComponents.joinForNarration(this.label, p_269645_.createDefaultNarrationMessage(), this.info));
         } else {
            builder.withCustomNarration(p_268230_ -> CommonComponents.joinForNarration(this.label, p_268230_.createDefaultNarrationMessage()));
         }

         CycleButton<Boolean> cyclebutton = builder.create(
            0, 0, this.buttonWidth, 20, Component.empty(), (p_267942_, p_268251_) -> this.onClicked.accept(p_268251_)
         );
         if (this.isActiveCondition != null) {
            cyclebutton.active = this.isActiveCondition.getAsBoolean();
         }

         p_271004_.addChild(cyclebutton, p_270513_.rowCount, p_270506_ + 1, p_271004_.newCellSettings().alignHorizontallyRight());
         if (this.info != null) {
            optional.ifPresent(
               p_269649_ -> {
                  Component component = this.info.copy().withStyle(ChatFormatting.GRAY);
                  Font font = Minecraft.getInstance().font;
                  MultiLineTextWidget multilinetextwidget = new MultiLineTextWidget(component, font);
                  multilinetextwidget.setMaxWidth(p_270513_.width - p_270513_.paddingLeft - this.buttonWidth);
                  multilinetextwidget.setMaxRows(p_269649_.maxInfoRows());
                  p_270513_.increaseRow();
                  int i = p_269649_.alwaysMaxHeight ? 9 * p_269649_.maxInfoRows - multilinetextwidget.getHeight() : 0;
                  p_271004_.addChild(
                     multilinetextwidget, p_270513_.rowCount, p_270506_, p_271004_.newCellSettings().paddingTop(-p_270513_.rowSpacing).paddingBottom(i)
                  );
               }
            );
         }

         return new SwitchGrid.LabeledSwitch(cyclebutton, this.stateSupplier, this.isActiveCondition);
      }
   }
}
