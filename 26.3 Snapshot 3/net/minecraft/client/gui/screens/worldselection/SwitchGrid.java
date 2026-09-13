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
import org.jspecify.annotations.Nullable;

class SwitchGrid {
   private static final int DEFAULT_SWITCH_BUTTON_WIDTH = 44;
   private final List<SwitchGrid.LabeledSwitch> switches;
   private final Layout layout;

   private SwitchGrid(final List<SwitchGrid.LabeledSwitch> switches, final Layout layout) {
      this.switches = switches;
      this.layout = layout;
   }

   public Layout layout() {
      return this.layout;
   }

   public void refreshStates() {
      this.switches.forEach(SwitchGrid.LabeledSwitch::refreshState);
   }

   public static SwitchGrid.Builder builder(final int width) {
      return new SwitchGrid.Builder(width);
   }

   public static class Builder {
      private final int width;
      private final List<SwitchGrid.SwitchBuilder> switchBuilders = new ArrayList<>();
      private int paddingLeft;
      private int rowSpacing = 4;
      private int rowCount;
      private Optional<SwitchGrid.InfoUnderneathSettings> infoUnderneath = Optional.empty();

      public Builder(final int width) {
         this.width = width;
      }

      private void increaseRow() {
         this.rowCount++;
      }

      public SwitchGrid.SwitchBuilder addSwitch(final Component label, final BooleanSupplier stateSupplier, final Consumer<Boolean> onClicked) {
         SwitchGrid.SwitchBuilder switchBuilder = new SwitchGrid.SwitchBuilder(label, stateSupplier, onClicked, 44);
         this.switchBuilders.add(switchBuilder);
         return switchBuilder;
      }

      public SwitchGrid.Builder withPaddingLeft(final int paddingLeft) {
         this.paddingLeft = paddingLeft;
         return this;
      }

      public SwitchGrid.Builder withRowSpacing(final int rowSpacing) {
         this.rowSpacing = rowSpacing;
         return this;
      }

      public SwitchGrid build() {
         GridLayout switchGrid = new GridLayout().rowSpacing(this.rowSpacing);
         switchGrid.addChild(SpacerElement.width(this.width - 44), 0, 0);
         switchGrid.addChild(SpacerElement.width(44), 0, 1);
         List<SwitchGrid.LabeledSwitch> switches = new ArrayList<>();
         this.rowCount = 0;

         for (SwitchGrid.SwitchBuilder switchBuilder : this.switchBuilders) {
            switches.add(switchBuilder.build(this, switchGrid, 0));
         }

         switchGrid.arrangeElements();
         SwitchGrid result = new SwitchGrid(switches, switchGrid);
         result.refreshStates();
         return result;
      }

      public SwitchGrid.Builder withInfoUnderneath(final int maxRows, final boolean alwaysMaxHeight) {
         this.infoUnderneath = Optional.of(new SwitchGrid.InfoUnderneathSettings(maxRows, alwaysMaxHeight));
         return this;
      }
   }

   private record InfoUnderneathSettings(int maxInfoRows, boolean alwaysMaxHeight) {
   }

   private record LabeledSwitch(CycleButton<Boolean> button, BooleanSupplier stateSupplier, @Nullable BooleanSupplier isActiveCondition) {
      public void refreshState() {
         this.button.setValue(this.stateSupplier.getAsBoolean());
         if (this.isActiveCondition != null) {
            this.button.active = this.isActiveCondition.getAsBoolean();
         }
      }
   }

   public static class SwitchBuilder {
      private final Component label;
      private final BooleanSupplier stateSupplier;
      private final Consumer<Boolean> onClicked;
      private @Nullable Component info;
      private @Nullable BooleanSupplier isActiveCondition;
      private final int buttonWidth;

      private SwitchBuilder(final Component label, final BooleanSupplier stateSupplier, final Consumer<Boolean> onClicked, final int buttonWidth) {
         this.label = label;
         this.stateSupplier = stateSupplier;
         this.onClicked = onClicked;
         this.buttonWidth = buttonWidth;
      }

      public SwitchGrid.SwitchBuilder withIsActiveCondition(final BooleanSupplier isActiveCondition) {
         this.isActiveCondition = isActiveCondition;
         return this;
      }

      public SwitchGrid.SwitchBuilder withInfo(final Component info) {
         this.info = info;
         return this;
      }

      private SwitchGrid.LabeledSwitch build(final SwitchGrid.Builder switchGridBuilder, final GridLayout gridLayout, final int startColumn) {
         switchGridBuilder.increaseRow();
         StringWidget labelWidget = new StringWidget(this.label, Minecraft.getInstance().font);
         gridLayout.addChild(
            labelWidget, switchGridBuilder.rowCount, startColumn, gridLayout.newCellSettings().align(0.0F, 0.5F).paddingLeft(switchGridBuilder.paddingLeft)
         );
         Optional<SwitchGrid.InfoUnderneathSettings> infoUnderneath = switchGridBuilder.infoUnderneath;
         CycleButton.Builder<Boolean> buttonBuilder = CycleButton.onOffBuilder(this.stateSupplier.getAsBoolean());
         buttonBuilder.displayOnlyValue();
         boolean hasTooltip = this.info != null && infoUnderneath.isEmpty();
         if (hasTooltip) {
            Tooltip tooltip = Tooltip.create(this.info);
            buttonBuilder.withTooltip(value -> tooltip);
         }

         if (this.info != null && !hasTooltip) {
            buttonBuilder.withCustomNarration(buttonx -> CommonComponents.joinForNarration(this.label, buttonx.createDefaultNarrationMessage(), this.info));
         } else {
            buttonBuilder.withCustomNarration(buttonx -> CommonComponents.joinForNarration(this.label, buttonx.createDefaultNarrationMessage()));
         }

         CycleButton<Boolean> button = buttonBuilder.create(0, 0, this.buttonWidth, 20, Component.empty(), (b, value) -> this.onClicked.accept(value));
         if (this.isActiveCondition != null) {
            button.active = this.isActiveCondition.getAsBoolean();
         }

         gridLayout.addChild(button, switchGridBuilder.rowCount, startColumn + 1, gridLayout.newCellSettings().alignHorizontallyRight());
         if (this.info != null) {
            infoUnderneath.ifPresent(
               infoUnderneathSettings -> {
                  Component styledInfo = this.info.copy().withStyle(ChatFormatting.GRAY);
                  Font font = Minecraft.getInstance().font;
                  MultiLineTextWidget infoWidget = new MultiLineTextWidget(styledInfo, font);
                  infoWidget.setMaxWidth(switchGridBuilder.width - switchGridBuilder.paddingLeft - this.buttonWidth);
                  infoWidget.setMaxRows(infoUnderneathSettings.maxInfoRows());
                  switchGridBuilder.increaseRow();
                  int extraBottomPadding = infoUnderneathSettings.alwaysMaxHeight ? 9 * infoUnderneathSettings.maxInfoRows - infoWidget.getHeight() : 0;
                  gridLayout.addChild(
                     infoWidget,
                     switchGridBuilder.rowCount,
                     startColumn,
                     gridLayout.newCellSettings().paddingTop(-switchGridBuilder.rowSpacing).paddingBottom(extraBottomPadding)
                  );
               }
            );
         }

         return new SwitchGrid.LabeledSwitch(button, this.stateSupplier, this.isActiveCondition);
      }
   }
}
