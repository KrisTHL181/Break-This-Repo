package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JigsawBlockEditScreen extends Screen {
    private static final Component JOINT_LABEL = Component.translatable("jigsaw_block.joint_label");
    private static final Component POOL_LABEL = Component.translatable("jigsaw_block.pool");
    private static final Component NAME_LABEL = Component.translatable("jigsaw_block.name");
    private static final Component TARGET_LABEL = Component.translatable("jigsaw_block.target");
    private static final Component FINAL_STATE_LABEL = Component.translatable("jigsaw_block.final_state");
    private static final Component PLACEMENT_PRIORITY_LABEL = Component.translatable("jigsaw_block.placement_priority");
    private static final Component PLACEMENT_PRIORITY_TOOLTIP = Component.translatable("jigsaw_block.placement_priority.tooltip");
    private static final Component SELECTION_PRIORITY_LABEL = Component.translatable("jigsaw_block.selection_priority");
    private static final Component SELECTION_PRIORITY_TOOLTIP = Component.translatable("jigsaw_block.selection_priority.tooltip");
    private final JigsawBlockEntity jigsawEntity;
    private EditBox nameEdit;
    private EditBox targetEdit;
    private EditBox poolEdit;
    private EditBox finalStateEdit;
    private EditBox selectionPriorityEdit;
    private EditBox placementPriorityEdit;
    private int levels;
    private boolean keepJigsaws = true;
    private CycleButton<JigsawBlockEntity.JointType> jointButton;
    private Button doneButton;
    private Button generateButton;
    private JigsawBlockEntity.JointType joint;

    public JigsawBlockEditScreen(final JigsawBlockEntity jigsawEntity) {
        super(GameNarrator.NO_TITLE);
        this.jigsawEntity = jigsawEntity;
    }

    private void onDone() {
        this.sendToServer();
        this.minecraft.gui.setScreen(null);
    }

    private void onCancel() {
        this.minecraft.gui.setScreen(null);
    }

    private void sendToServer() {
        this.minecraft
            .getConnection()
            .send(
                new ServerboundSetJigsawBlockPacket(
                    this.jigsawEntity.getBlockPos(),
                    Identifier.parse(this.nameEdit.getValue()),
                    Identifier.parse(this.targetEdit.getValue()),
                    Identifier.parse(this.poolEdit.getValue()),
                    this.finalStateEdit.getValue(),
                    this.joint,
                    this.parseAsInt(this.selectionPriorityEdit.getValue()),
                    this.parseAsInt(this.placementPriorityEdit.getValue())
                )
            );
    }

    private int parseAsInt(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void sendGenerate() {
        this.minecraft.getConnection().send(new ServerboundJigsawGeneratePacket(this.jigsawEntity.getBlockPos(), this.levels, this.keepJigsaws));
    }

    @Override
    public void onClose() {
        this.onCancel();
    }

    @Override
    protected void init() {
        this.poolEdit = new EditBox(this.font, this.width / 2 - 153, 20, 300, 20, POOL_LABEL);
        this.poolEdit.setMaxLength(128);
        this.poolEdit.setValue(this.jigsawEntity.getPool().identifier().toString());
        this.poolEdit.setResponder(value -> this.updateValidity());
        this.addWidget(this.poolEdit);
        this.nameEdit = new EditBox(this.font, this.width / 2 - 153, 55, 300, 20, NAME_LABEL);
        this.nameEdit.setMaxLength(128);
        this.nameEdit.setValue(this.jigsawEntity.getName().toString());
        this.nameEdit.setResponder(value -> this.updateValidity());
        this.addWidget(this.nameEdit);
        this.targetEdit = new EditBox(this.font, this.width / 2 - 153, 90, 300, 20, TARGET_LABEL);
        this.targetEdit.setMaxLength(128);
        this.targetEdit.setValue(this.jigsawEntity.getTarget().toString());
        this.targetEdit.setResponder(value -> this.updateValidity());
        this.addWidget(this.targetEdit);
        this.finalStateEdit = new EditBox(this.font, this.width / 2 - 153, 125, 300, 20, FINAL_STATE_LABEL);
        this.finalStateEdit.setMaxLength(256);
        this.finalStateEdit.setValue(this.jigsawEntity.getFinalState());
        this.addWidget(this.finalStateEdit);
        this.selectionPriorityEdit = new EditBox(this.font, this.width / 2 - 153, 160, 98, 20, SELECTION_PRIORITY_LABEL);
        this.selectionPriorityEdit.setMaxLength(3);
        this.selectionPriorityEdit.setValue(Integer.toString(this.jigsawEntity.getSelectionPriority()));
        this.selectionPriorityEdit.setTooltip(Tooltip.create(SELECTION_PRIORITY_TOOLTIP));
        this.addWidget(this.selectionPriorityEdit);
        this.placementPriorityEdit = new EditBox(this.font, this.width / 2 - 50, 160, 98, 20, PLACEMENT_PRIORITY_LABEL);
        this.placementPriorityEdit.setMaxLength(3);
        this.placementPriorityEdit.setValue(Integer.toString(this.jigsawEntity.getPlacementPriority()));
        this.placementPriorityEdit.setTooltip(Tooltip.create(PLACEMENT_PRIORITY_TOOLTIP));
        this.addWidget(this.placementPriorityEdit);
        this.joint = this.jigsawEntity.getJoint();
        this.jointButton = this.addRenderableWidget(
            CycleButton.builder(JigsawBlockEntity.JointType::getTranslatedName, this.joint)
                .withValues(JigsawBlockEntity.JointType.values())
                .displayOnlyValue()
                .create(this.width / 2 + 54, 160, 100, 20, JOINT_LABEL, (button, value) -> this.joint = value)
        );
        boolean vertical = JigsawBlock.getFrontFacing(this.jigsawEntity.getBlockState()).getAxis().isVertical();
        this.jointButton.active = vertical;
        this.jointButton.visible = vertical;
        this.addRenderableWidget(new AbstractSliderButton(this.width / 2 - 154, 185, 100, 20, CommonComponents.EMPTY, 0.0) {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(Component.translatable("jigsaw_block.levels", JigsawBlockEditScreen.this.levels));
            }

            @Override
            protected void applyValue() {
                JigsawBlockEditScreen.this.levels = Mth.floor(Mth.clampedLerp(this.value, 0.0, 20.0));
            }
        });
        this.addRenderableWidget(
            CycleButton.onOffBuilder(this.keepJigsaws)
                .create(this.width / 2 - 50, 185, 100, 20, Component.translatable("jigsaw_block.keep_jigsaws"), (button, value) -> this.keepJigsaws = value)
        );
        this.generateButton = this.addRenderableWidget(Button.builder(Component.translatable("jigsaw_block.generate"), button -> {
            this.onDone();
            this.sendGenerate();
        }).bounds(this.width / 2 + 54, 185, 100, 20).build());
        this.doneButton = this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.width / 2 - 4 - 150, 210, 150, 20).build()
        );
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel()).bounds(this.width / 2 + 4, 210, 150, 20).build());
        this.updateValidity();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.poolEdit);
    }

    public static boolean isValidIdentifier(final String location) {
        return Identifier.tryParse(location) != null;
    }

    private void updateValidity() {
        boolean isValid = isValidIdentifier(this.nameEdit.getValue())
            && isValidIdentifier(this.targetEdit.getValue())
            && isValidIdentifier(this.poolEdit.getValue());
        this.doneButton.active = isValid;
        this.generateButton.active = isValid;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void resize(final int width, final int height) {
        String oldNameEdit = this.nameEdit.getValue();
        String oldTargetEdit = this.targetEdit.getValue();
        String oldPoolEdit = this.poolEdit.getValue();
        String oldFinalStateEdit = this.finalStateEdit.getValue();
        String oldSelectionPriorityEdit = this.selectionPriorityEdit.getValue();
        String oldPlacementPriorityEdit = this.placementPriorityEdit.getValue();
        int oldLevels = this.levels;
        JigsawBlockEntity.JointType oldJointType = this.joint;
        this.init(width, height);
        this.nameEdit.setValue(oldNameEdit);
        this.targetEdit.setValue(oldTargetEdit);
        this.poolEdit.setValue(oldPoolEdit);
        this.finalStateEdit.setValue(oldFinalStateEdit);
        this.levels = oldLevels;
        this.joint = oldJointType;
        this.jointButton.setValue(oldJointType);
        this.selectionPriorityEdit.setValue(oldSelectionPriorityEdit);
        this.placementPriorityEdit.setValue(oldPlacementPriorityEdit);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        } else if (this.doneButton.active && event.isConfirmation()) {
            this.onDone();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, POOL_LABEL, this.width / 2 - 153, 10, -6250336);
        this.poolEdit.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, NAME_LABEL, this.width / 2 - 153, 45, -6250336);
        this.nameEdit.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, TARGET_LABEL, this.width / 2 - 153, 80, -6250336);
        this.targetEdit.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, FINAL_STATE_LABEL, this.width / 2 - 153, 115, -6250336);
        this.finalStateEdit.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, SELECTION_PRIORITY_LABEL, this.width / 2 - 153, 150, -6250336);
        this.placementPriorityEdit.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, PLACEMENT_PRIORITY_LABEL, this.width / 2 - 50, 150, -6250336);
        this.selectionPriorityEdit.extractRenderState(graphics, mouseX, mouseY, a);
        if (JigsawBlock.getFrontFacing(this.jigsawEntity.getBlockState()).getAxis().isVertical()) {
            graphics.text(this.font, JOINT_LABEL, this.width / 2 + 53, 150, -6250336);
        }
    }
}