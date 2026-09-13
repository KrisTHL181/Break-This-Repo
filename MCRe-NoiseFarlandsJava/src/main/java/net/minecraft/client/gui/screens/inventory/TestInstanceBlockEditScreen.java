package net.minecraft.client.gui.screens.inventory;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TestInstanceBlockEditScreen extends Screen {
    private static final Component ID_LABEL = Component.translatable("test_instance_block.test_id");
    private static final Component SIZE_LABEL = Component.translatable("test_instance_block.size");
    private static final Component INCLUDE_ENTITIES_LABEL = Component.translatable("test_instance_block.entities");
    private static final Component ROTATION_LABEL = Component.translatable("test_instance_block.rotation");
    private static final int BUTTON_PADDING = 8;
    private static final int WIDTH = 316;
    private final TestInstanceBlockEntity blockEntity;
    private @Nullable EditBox idEdit;
    private @Nullable EditBox sizeXEdit;
    private @Nullable EditBox sizeYEdit;
    private @Nullable EditBox sizeZEdit;
    private @Nullable FittingMultiLineTextWidget infoWidget;
    private @Nullable Button saveButton;
    private @Nullable Button exportButton;
    private @Nullable CycleButton<Boolean> includeEntitiesButton;
    private @Nullable CycleButton<Rotation> rotationButton;

    public TestInstanceBlockEditScreen(final TestInstanceBlockEntity blockEntity) {
        super(blockEntity.getBlockState().getBlock().getName());
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int leftEdge = this.width / 2 - 158;
        boolean includeExport = SharedConstants.IS_RUNNING_IN_IDE;
        int actionButtonCount = includeExport ? 3 : 2;
        int buttonSize = widgetSize(actionButtonCount);
        this.idEdit = new EditBox(this.font, leftEdge, 40, 316, 20, Component.translatable("test_instance_block.test_id"));
        this.idEdit.setMaxLength(128);
        Optional<ResourceKey<GameTestInstance>> test = this.blockEntity.test();
        if (test.isPresent()) {
            this.idEdit.setValue(test.get().identifier().toString());
        }

        this.idEdit.setResponder(s -> this.updateTestInfo(false));
        this.addRenderableWidget(this.idEdit);
        this.infoWidget = new FittingMultiLineTextWidget(leftEdge, 70, 316, 8 * 9, Component.literal(""), this.font);
        this.addRenderableWidget(this.infoWidget);
        Vec3i size = this.blockEntity.getSize();
        int index = 0;
        this.sizeXEdit = new EditBox(this.font, this.widgetX(index++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.x"));
        this.sizeXEdit.setMaxLength(15);
        this.addRenderableWidget(this.sizeXEdit);
        this.sizeYEdit = new EditBox(this.font, this.widgetX(index++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.y"));
        this.sizeYEdit.setMaxLength(15);
        this.addRenderableWidget(this.sizeYEdit);
        this.sizeZEdit = new EditBox(this.font, this.widgetX(index++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.z"));
        this.sizeZEdit.setMaxLength(15);
        this.addRenderableWidget(this.sizeZEdit);
        this.setSize(size);
        this.rotationButton = this.addRenderableWidget(
            CycleButton.builder(TestInstanceBlockEditScreen::rotationDisplay, this.blockEntity.getRotation())
                .withValues(Rotation.values())
                .displayOnlyValue()
                .create(this.widgetX(index++, 5), 160, widgetSize(5), 20, ROTATION_LABEL, (button, value) -> this.updateSaveState())
        );
        this.includeEntitiesButton = this.addRenderableWidget(
            CycleButton.onOffBuilder(!this.blockEntity.ignoreEntities())
                .displayOnlyValue()
                .create(this.widgetX(index++, 5), 160, widgetSize(5), 20, INCLUDE_ENTITIES_LABEL)
        );
        index = 0;
        this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.reset"), button -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RESET);
            this.minecraft.gui.setScreen(null);
        }).bounds(this.widgetX(index++, actionButtonCount), 185, buttonSize, 20).build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.save"), button -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SAVE);
            this.minecraft.gui.setScreen(null);
        }).bounds(this.widgetX(index++, actionButtonCount), 185, buttonSize, 20).build());
        if (includeExport) {
            this.exportButton = this.addRenderableWidget(Button.builder(Component.literal("Export Structure"), button -> {
                this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.EXPORT);
                this.minecraft.gui.setScreen(null);
            }).bounds(this.widgetX(index++, actionButtonCount), 185, buttonSize, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.run"), button -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RUN);
            this.minecraft.gui.setScreen(null);
        }).bounds(this.widgetX(0, 3), 210, widgetSize(3), 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.widgetX(1, 3), 210, widgetSize(3), 20).build());
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel()).bounds(this.widgetX(2, 3), 210, widgetSize(3), 20).build()
        );
        this.updateTestInfo(true);
    }

    private void updateSaveState() {
        boolean allowSaving = this.rotationButton.getValue() == Rotation.NONE && Identifier.tryParse(this.idEdit.getValue()) != null;
        this.saveButton.active = allowSaving;
        if (this.exportButton != null) {
            this.exportButton.active = allowSaving;
        }
    }

    private static Component rotationDisplay(final Rotation rotation) {
        return Component.literal(switch (rotation) {
            case NONE -> "0";
            case CLOCKWISE_90 -> "90";
            case CLOCKWISE_180 -> "180";
            case COUNTERCLOCKWISE_90 -> "270";
        });
    }

    private void setSize(final Vec3i size) {
        this.sizeXEdit.setValue(Integer.toString(size.getX()));
        this.sizeYEdit.setValue(Integer.toString(size.getY()));
        this.sizeZEdit.setValue(Integer.toString(size.getZ()));
    }

    private int widgetX(final int index, final int count) {
        int leftEdge = this.width / 2 - 158;
        float buttonSize = exactWidgetSize(count);
        return (int)(leftEdge + index * (8.0F + buttonSize));
    }

    private static int widgetSize(final int count) {
        return (int)exactWidgetSize(count);
    }

    private static float exactWidgetSize(final int count) {
        return (float)(316 - (count - 1) * 8) / count;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int leftEdge = this.width / 2 - 158;
        graphics.centeredText(this.font, this.title, this.width / 2, 10, -1);
        graphics.text(this.font, ID_LABEL, leftEdge, 30, -6250336);
        graphics.text(this.font, SIZE_LABEL, leftEdge, 150, -6250336);
        graphics.text(this.font, ROTATION_LABEL, this.rotationButton.getX(), 150, -6250336);
        graphics.text(this.font, INCLUDE_ENTITIES_LABEL, this.includeEntitiesButton.getX(), 150, -6250336);
    }

    private void updateTestInfo(final boolean isInit) {
        boolean valid = this.sendToServer(isInit ? ServerboundTestInstanceBlockActionPacket.Action.INIT : ServerboundTestInstanceBlockActionPacket.Action.QUERY);
        if (!valid) {
            this.infoWidget.setMessage(Component.translatable("test_instance.description.invalid_id").withStyle(ChatFormatting.RED));
        }

        this.updateSaveState();
    }

    private void onDone() {
        this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SET);
        this.onClose();
    }

    private boolean sendToServer(final ServerboundTestInstanceBlockActionPacket.Action action) {
        Optional<Identifier> id = Optional.ofNullable(Identifier.tryParse(this.idEdit.getValue()));
        Optional<ResourceKey<GameTestInstance>> key = id.map(i -> ResourceKey.create(Registries.TEST_INSTANCE, i));
        Vec3i size = new Vec3i(parseSize(this.sizeXEdit.getValue()), parseSize(this.sizeYEdit.getValue()), parseSize(this.sizeZEdit.getValue()));
        boolean ignoreEntities = !this.includeEntitiesButton.getValue();
        this.minecraft
            .getConnection()
            .send(
                new ServerboundTestInstanceBlockActionPacket(this.blockEntity.getBlockPos(), action, key, size, this.rotationButton.getValue(), ignoreEntities)
            );
        return id.isPresent();
    }

    public void setStatus(final Component status, final Optional<Vec3i> size) {
        MutableComponent description = Component.empty();
        this.blockEntity
            .errorMessage()
            .ifPresent(
                error -> description.append(
                        Component.translatable("test_instance.description.failed", Component.empty().withStyle(ChatFormatting.RED).append(error))
                    )
                    .append("\n\n")
            );
        description.append(status);
        this.infoWidget.setMessage(description);
        size.ifPresent(this::setSize);
    }

    private void onCancel() {
        this.onClose();
    }

    private static int parseSize(final String value) {
        try {
            return Mth.clamp(Integer.parseInt(value), 1, 48);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}