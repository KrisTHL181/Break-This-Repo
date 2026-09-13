package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.jspecify.annotations.Nullable;

public class ExperimentsScreen extends Screen {
   private static final Component TITLE = Component.translatable("selectWorld.experiments");
   private static final Component INFO = Component.translatable("selectWorld.experiments.info").withStyle(ChatFormatting.RED);
   private static final int MAIN_CONTENT_WIDTH = 310;
   private static final int SCROLL_AREA_MIN_HEIGHT = 130;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Screen parent;
   private final PackRepository packRepository;
   private final Consumer<PackRepository> output;
   private final Object2BooleanMap<Pack> packs = new Object2BooleanLinkedOpenHashMap();
   private @Nullable ScrollableLayout scrollArea;

   public ExperimentsScreen(final Screen parent, final PackRepository packRepository, final Consumer<PackRepository> output) {
      super(TITLE);
      this.parent = parent;
      this.packRepository = packRepository;
      this.output = output;

      for (Pack pack : packRepository.getAvailablePacks()) {
         if (pack.getPackSource() == PackSource.FEATURE) {
            this.packs.put(pack, packRepository.getSelectedPacks().contains(pack));
         }
      }
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
      content.addChild(new MultiLineTextWidget(INFO, this.font).setMaxWidth(310), s -> s.paddingBottom(15));
      SwitchGrid.Builder switchGridBuilder = SwitchGrid.builder(299).withInfoUnderneath(2, true).withRowSpacing(4);
      this.packs
         .forEach(
            (pack, selected) -> switchGridBuilder.addSwitch(
                  getHumanReadableTitle(pack), () -> this.packs.getBoolean(pack), newSelected -> this.packs.put(pack, newSelected)
               )
               .withInfo(pack.getDescription())
         );
      Layout switchGridLayout = switchGridBuilder.build().layout();
      this.scrollArea = new ScrollableLayout(this.minecraft, switchGridLayout, 130);
      this.scrollArea.setMinWidth(310);
      content.addChild(this.scrollArea);
      LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).build());
      footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   private static Component getHumanReadableTitle(final Pack pack) {
      String translationKey = "dataPack." + pack.getId() + ".name";
      return Language.getInstance().has(translationKey) ? Component.translatable(translationKey) : pack.getTitle();
   }

   @Override
   protected void repositionElements() {
      this.scrollArea.setMaxHeight(130);
      this.layout.arrangeElements();
      int availableExtraHeight = this.height - this.layout.getFooterHeight() - this.scrollArea.getRectangle().bottom();
      this.scrollArea.setMaxHeight(this.scrollArea.getHeight() + availableExtraHeight);
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), INFO);
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.parent);
   }

   private void onDone() {
      List<Pack> selectedPacks = new ArrayList<>(this.packRepository.getSelectedPacks());
      List<Pack> selectedFeatures = new ArrayList<>();
      this.packs.forEach((pack, selected) -> {
         selectedPacks.remove(pack);
         if (selected) {
            selectedFeatures.add(pack);
         }
      });
      selectedPacks.addAll(Lists.reverse(selectedFeatures));
      this.packRepository.setSelected(selectedPacks.stream().map(Pack::getId).toList());
      this.output.accept(this.packRepository);
   }
}
