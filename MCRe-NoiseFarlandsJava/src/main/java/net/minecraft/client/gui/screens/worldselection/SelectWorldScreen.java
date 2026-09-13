package net.minecraft.client.gui.screens.worldselection;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
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
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);
    private @Nullable Button deleteButton;
    private @Nullable Button playWorldButton;
    private @Nullable Button editButton;
    private @Nullable Button recreateButton;
    protected @Nullable EditBox searchBox;
    private @Nullable WorldSelectionList list;

    public SelectWorldScreen(final Screen lastScreen) {
        super(Component.translatable("selectWorld.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        LinearLayout subHeader = header.addChild(LinearLayout.horizontal().spacing(4));
        if (SharedConstants.DEBUG_WORLD_RECREATE) {
            subHeader.addChild(this.createDebugWorldRecreateButton());
        }

        this.searchBox = subHeader.addChild(
            new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("selectWorld.search"))
        );
        this.searchBox.setResponder(value -> {
            if (this.list != null) {
                this.list.updateFilter(value);
            }
        });
        this.searchBox.setHint(Component.translatable("gui.selectWorld.search").setStyle(EditBox.SEARCH_HINT_STYLE));
        Consumer<WorldSelectionList.WorldListEntry> joinWorld = WorldSelectionList.WorldListEntry::joinWorld;
        this.list = this.layout
            .addToContents(
                new WorldSelectionList.Builder(this.minecraft, this)
                    .width(this.width)
                    .height(this.layout.getContentHeight())
                    .filter(this.searchBox.getValue())
                    .oldList(this.list)
                    .onEntrySelect(this::updateButtonStatus)
                    .onEntryInteract(joinWorld)
                    .build()
            );
        this.createFooterButtons(joinWorld, this.list);
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
        this.updateButtonStatus(null);
    }

    private void createFooterButtons(final Consumer<WorldSelectionList.WorldListEntry> joinWorld, final WorldSelectionList list) {
        GridLayout footer = this.layout.addToFooter(new GridLayout().columnSpacing(8).rowSpacing(4));
        footer.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper rowHelper = footer.createRowHelper(4);
        this.playWorldButton = rowHelper.addChild(Button.builder(LevelSummary.PLAY_WORLD, button -> list.getSelectedOpt().ifPresent(joinWorld)).build(), 2);
        rowHelper.addChild(
            Button.builder(Component.translatable("selectWorld.create"), button -> CreateWorldScreen.openFresh(this.minecraft, list::returnToScreen)).build(),
            2
        );
        this.editButton = rowHelper.addChild(
            Button.builder(Component.translatable("selectWorld.edit"), button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::editWorld))
                .width(71)
                .build()
        );
        this.deleteButton = rowHelper.addChild(
            Button.builder(
                    Component.translatable("selectWorld.delete"), button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::deleteWorld)
                )
                .width(71)
                .build()
        );
        this.recreateButton = rowHelper.addChild(
            Button.builder(
                    Component.translatable("selectWorld.recreate"), button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::recreateWorld)
                )
                .width(71)
                .build()
        );
        rowHelper.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.minecraft.gui.setScreen(this.lastScreen)).width(71).build());
    }

    private Button createDebugWorldRecreateButton() {
        return Button.builder(
                Component.literal("DEBUG recreate"),
                button -> {
                    try {
                        String levelName = "DEBUG world";
                        if (this.list != null && !this.list.children().isEmpty()) {
                            WorldSelectionList.Entry entry = this.list.children().getFirst();
                            if (entry instanceof WorldSelectionList.WorldListEntry worldEntry && worldEntry.getLevelName().equals("DEBUG world")) {
                                worldEntry.doDeleteWorld();
                            }
                        }

                        LevelSettings levelSettings = new LevelSettings(
                            "DEBUG world", GameType.SPECTATOR, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT
                        );
                        String resultFolder = FileUtil.findAvailableName(this.minecraft.getLevelSource().getBaseDir(), "DEBUG world", "");
                        this.minecraft
                            .createWorldOpenFlows()
                            .createFreshLevel(resultFolder, levelSettings, TEST_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
                    } catch (IOException e) {
                        LOGGER.error("Failed to recreate the debug world", e);
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
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    public void updateButtonStatus(final @Nullable LevelSummary summary) {
        if (this.playWorldButton != null && this.editButton != null && this.recreateButton != null && this.deleteButton != null) {
            if (summary == null) {
                this.playWorldButton.setMessage(LevelSummary.PLAY_WORLD);
                this.playWorldButton.active = false;
                this.editButton.active = false;
                this.recreateButton.active = false;
                this.deleteButton.active = false;
            } else {
                this.playWorldButton.setMessage(summary.primaryActionMessage());
                this.playWorldButton.active = summary.primaryActionActive();
                this.editButton.active = summary.canEdit();
                this.recreateButton.active = summary.canRecreate();
                this.deleteButton.active = summary.canDelete();
                if (summary.requiresFileFixing()) {
                    this.editButton.setTooltip(Tooltip.create(Component.translatable("selectWorld.requiresFileFixingTooltip.edit")));
                    this.playWorldButton.setTooltip(Tooltip.create(Component.translatable("selectWorld.requiresFileFixingTooltip.play")));
                    this.recreateButton.setTooltip(Tooltip.create(Component.translatable("selectWorld.requiresFileFixingTooltip.recreate")));
                } else {
                    this.editButton.setTooltip(null);
                    this.playWorldButton.setTooltip(null);
                    this.recreateButton.setTooltip(null);
                }
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