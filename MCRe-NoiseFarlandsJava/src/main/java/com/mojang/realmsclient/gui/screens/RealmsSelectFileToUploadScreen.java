package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectFileToUploadScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component TITLE = Component.translatable("mco.upload.select.world.title");
    private static final Component UNABLE_TO_LOAD_WORLD = Component.translatable("selectWorld.unable_to_load");
    private final @Nullable RealmCreationTask realmCreationTask;
    private final RealmsResetWorldScreen lastScreen;
    private final long realmId;
    private final int slotId;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 33);
    protected @Nullable EditBox searchBox;
    private @Nullable WorldSelectionList list;
    private @Nullable Button uploadButton;

    public RealmsSelectFileToUploadScreen(
        final @Nullable RealmCreationTask realmCreationTask, final long realmId, final int slotId, final RealmsResetWorldScreen lastScreen
    ) {
        super(TITLE);
        this.realmCreationTask = realmCreationTask;
        this.lastScreen = lastScreen;
        this.realmId = realmId;
        this.slotId = slotId;
    }

    @Override
    public void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.searchBox = header.addChild(
            new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("selectWorld.search"))
        );
        this.searchBox.setResponder(value -> {
            if (this.list != null) {
                this.list.updateFilter(value);
            }
        });

        try {
            this.list = this.layout
                .addToContents(
                    new WorldSelectionList.Builder(this.minecraft, this)
                        .width(this.width)
                        .height(this.layout.getContentHeight())
                        .filter(this.searchBox.getValue())
                        .oldList(this.list)
                        .uploadWorld()
                        .onEntrySelect(this::updateButtonState)
                        .onEntryInteract(this::upload)
                        .build()
                );
        } catch (Exception e) {
            LOGGER.error("Couldn't load level list", e);
            this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(UNABLE_TO_LOAD_WORLD, Component.nullToEmpty(e.getMessage()), this.lastScreen));
            return;
        }

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.defaultCellSetting().alignHorizontallyCenter();
        this.uploadButton = footer.addChild(
            Button.builder(Component.translatable("mco.upload.button.name"), button -> this.list.getSelectedOpt().ifPresent(this::upload)).build()
        );
        footer.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.updateButtonState(null);
        RealmsSelectFileToUploadScreen var3 = this;
        this.layout.visitWidgets(x$0 -> var3.addRenderableWidget(x$0));
        this.repositionElements();
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
        this.setInitialFocus(this.searchBox);
    }

    private void updateButtonState(final @Nullable LevelSummary ignored) {
        if (this.list != null && this.uploadButton != null) {
            this.uploadButton.active = this.list.getSelected() != null;
        }
    }

    private void upload(final WorldSelectionList.WorldListEntry worldListEntry) {
        this.minecraft
            .gui
            .setScreen(new RealmsUploadScreen(this.realmCreationTask, this.realmId, this.slotId, this.lastScreen, worldListEntry.getLevelSummary()));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}