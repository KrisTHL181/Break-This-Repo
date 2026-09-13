package net.minecraft.client.gui.screens.worldselection;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FileUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SelectWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final WorldOptions TEST_OPTIONS = new WorldOptions("test1".hashCode(), true, false);
   protected final Screen lastScreen;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 49, 60);
   private @Nullable Button deleteButton;
   private @Nullable Button selectButton;
   private @Nullable Button renameButton;
   private @Nullable Button copyButton;
   protected @Nullable EditBox searchBox;
   private @Nullable WorldSelectionList list;

   public SelectWorldScreen(Screen p_101338_) {
      super(Component.translatable("selectWorld.title"));
      this.lastScreen = p_101338_;
   }

   @Override
   protected void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(new StringWidget(this.title, this.font));
      LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal().spacing(4));
      if (SharedConstants.DEBUG_WORLD_RECREATE) {
         linearlayout1.addChild(this.createDebugWorldRecreateButton());
      }

      this.searchBox = linearlayout1.addChild(
         new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("selectWorld.search"))
      );
      this.searchBox.setResponder(p_420787_ -> {
         if (this.list != null) {
            this.list.updateFilter(p_420787_);
         }
      });
      this.searchBox.setHint(Component.translatable("gui.selectWorld.search").setStyle(EditBox.SEARCH_HINT_STYLE));
      Consumer<WorldSelectionList.WorldListEntry> consumer = WorldSelectionList.WorldListEntry::joinWorld;
      this.list = this.layout
         .addToContents(
            new WorldSelectionList.Builder(this.minecraft, this)
               .width(this.width)
               .height(this.layout.getContentHeight())
               .filter(this.searchBox.getValue())
               .oldList(this.list)
               .onEntrySelect(this::updateButtonStatus)
               .onEntryInteract(consumer)
               .build()
         );
      this.createFooterButtons(consumer, this.list);
      this.layout.visitWidgets(p_420791_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_420791_);
      });
      this.repositionElements();
      this.updateButtonStatus(null);
   }

   private void createFooterButtons(Consumer<WorldSelectionList.WorldListEntry> p_430246_, WorldSelectionList p_429546_) {
      GridLayout gridlayout = this.layout.addToFooter(new GridLayout().columnSpacing(8).rowSpacing(4));
      gridlayout.defaultCellSetting().alignHorizontallyCenter();
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(4);
      this.selectButton = gridlayout$rowhelper.addChild(
         Button.builder(LevelSummary.PLAY_WORLD, p_420800_ -> p_429546_.getSelectedOpt().ifPresent(p_430246_)).build(), 2
      );
      gridlayout$rowhelper.addChild(
         Button.builder(Component.translatable("selectWorld.create"), p_420789_ -> CreateWorldScreen.openFresh(this.minecraft, p_429546_::returnToScreen))
            .build(),
         2
      );
      this.renameButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.translatable("selectWorld.edit"), p_420793_ -> p_429546_.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::editWorld)
            )
            .width(71)
            .build()
      );
      this.deleteButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.translatable("selectWorld.delete"), p_420795_ -> p_429546_.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::deleteWorld)
            )
            .width(71)
            .build()
      );
      this.copyButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.translatable("selectWorld.recreate"),
               p_420797_ -> p_429546_.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::recreateWorld)
            )
            .width(71)
            .build()
      );
      gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_BACK, p_280917_ -> this.minecraft.setScreen(this.lastScreen)).width(71).build());
   }

   private Button createDebugWorldRecreateButton() {
      return Button.builder(
            Component.literal("DEBUG recreate"),
            p_357744_ -> {
               try {
                  String s = "DEBUG world";
                  if (this.list != null && !this.list.children().isEmpty()) {
                     WorldSelectionList.Entry worldselectionlist$entry = this.list.children().getFirst();
                     if (worldselectionlist$entry instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry
                        && worldselectionlist$worldlistentry.getLevelName().equals("DEBUG world")) {
                        worldselectionlist$worldlistentry.doDeleteWorld();
                     }
                  }

                  LevelSettings levelsettings = new LevelSettings(
                     "DEBUG world",
                     GameType.SPECTATOR,
                     false,
                     Difficulty.NORMAL,
                     true,
                     new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),
                     WorldDataConfiguration.DEFAULT
                  );
                  String s1 = FileUtil.findAvailableName(this.minecraft.getLevelSource().getBaseDir(), "DEBUG world", "");
                  this.minecraft.createWorldOpenFlows().createFreshLevel(s1, levelsettings, TEST_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
               } catch (IOException ioexception) {
                  LOGGER.error("Failed to recreate the debug world", ioexception);
               }
            }
         )
         .width(72)
         .build();
   }

   @Override
   protected void repositionElements() {
      if (this.list != null) {
         this.list.updateSize(this.width, this.layout);
      }

      this.layout.arrangeElements();
   }

   @Override
   protected void setInitialFocus() {
      if (this.searchBox != null) {
         this.setInitialFocus(this.searchBox);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public void updateButtonStatus(@Nullable LevelSummary p_309997_) {
      if (this.selectButton != null && this.renameButton != null && this.copyButton != null && this.deleteButton != null) {
         if (p_309997_ == null) {
            this.selectButton.setMessage(LevelSummary.PLAY_WORLD);
            this.selectButton.active = false;
            this.renameButton.active = false;
            this.copyButton.active = false;
            this.deleteButton.active = false;
         } else {
            this.selectButton.setMessage(p_309997_.primaryActionMessage());
            this.selectButton.active = p_309997_.primaryActionActive();
            this.renameButton.active = p_309997_.canEdit();
            this.copyButton.active = p_309997_.canRecreate();
            this.deleteButton.active = p_309997_.canDelete();
         }
      }
   }

   @Override
   public void removed() {
      if (this.list != null) {
         this.list.children().forEach(WorldSelectionList.Entry::close);
      }
   }
}
