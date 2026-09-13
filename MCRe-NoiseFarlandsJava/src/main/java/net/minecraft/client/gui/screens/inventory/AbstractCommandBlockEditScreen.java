package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractCommandBlockEditScreen extends Screen {
    private static final Component SET_COMMAND_LABEL = Component.translatable("advMode.setCommand");
    private static final Component COMMAND_LABEL = Component.translatable("advMode.command");
    private static final Component PREVIOUS_OUTPUT_LABEL = Component.translatable("advMode.previousOutput");
    protected EditBox commandEdit;
    protected EditBox previousEdit;
    protected Button doneButton;
    protected Button cancelButton;
    protected CycleButton<Boolean> outputButton;
    private CommandSuggestions commandSuggestions;

    public AbstractCommandBlockEditScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    public void tick() {
        if (!this.getCommandBlock().isValid()) {
            this.onClose();
        }
    }

    protected abstract BaseCommandBlock getCommandBlock();

    protected abstract int getPreviousY();

    @Override
    protected void init() {
        boolean trackOutput = this.getCommandBlock().isTrackOutput();
        this.commandEdit = new EditBox(this.font, this.width / 2 - 150, 50, 300, 20, Component.translatable("advMode.command")) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(AbstractCommandBlockEditScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.commandEdit.setMaxLength(32500);
        this.commandEdit.setResponder(this::onEdited);
        this.addWidget(this.commandEdit);
        this.previousEdit = new EditBox(this.font, this.width / 2 - 150, this.getPreviousY(), 276, 20, Component.translatable("advMode.previousOutput"));
        this.previousEdit.setMaxLength(32500);
        this.previousEdit.setEditable(false);
        this.previousEdit.setValue("-");
        this.addWidget(this.previousEdit);
        this.outputButton = this.addRenderableWidget(
            CycleButton.booleanBuilder(Component.literal("O"), Component.literal("X"), trackOutput)
                .displayOnlyValue()
                .create(this.width / 2 + 150 - 20, this.getPreviousY(), 20, 20, Component.translatable("advMode.trackOutput"), (button, value) -> {
                    BaseCommandBlock commandBlock = this.getCommandBlock();
                    commandBlock.setTrackOutput(value);
                    this.updatePreviousOutput(value);
                })
        );
        this.addExtraControls();
        this.doneButton = this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.width / 2 - 4 - 150, this.height / 4 + 120 + 12, 150, 20).build()
        );
        this.cancelButton = this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(this.width / 2 + 4, this.height / 4 + 120 + 12, 150, 20).build()
        );
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.commandEdit, this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.updatePreviousOutput(trackOutput);
    }

    protected void addExtraControls() {
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.commandEdit);
    }

    @Override
    protected Component getUsageNarration() {
        return this.commandSuggestions.isVisible() ? this.commandSuggestions.getUsageNarration() : super.getUsageNarration();
    }

    @Override
    public void resize(final int width, final int height) {
        String oldText = this.commandEdit.getValue();
        this.init(width, height);
        this.commandEdit.setValue(oldText);
        this.commandSuggestions.updateCommandInfo();
    }

    protected void updatePreviousOutput(final boolean isTracking) {
        this.previousEdit.setValue(isTracking ? this.getCommandBlock().getLastOutput().getString() : "-");
    }

    protected void onDone() {
        this.populateAndSendPacket();
        BaseCommandBlock commandBlock = this.getCommandBlock();
        if (!commandBlock.isTrackOutput()) {
            commandBlock.setLastOutput(null);
        }

        this.minecraft.gui.setScreen(null);
    }

    protected abstract void populateAndSendPacket();

    private void onEdited(final String value) {
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.commandSuggestions.keyPressed(event)) {
            return true;
        } else if (super.keyPressed(event)) {
            return true;
        } else if (event.isConfirmation()) {
            this.onDone();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return this.commandSuggestions.mouseScrolled(scrollY) ? true : super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return this.commandSuggestions.mouseClicked(event) ? true : super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, SET_COMMAND_LABEL, this.width / 2, 20, -1);
        graphics.text(this.font, COMMAND_LABEL, this.width / 2 - 150 + 1, 40, -6250336);
        this.commandEdit.extractRenderState(graphics, mouseX, mouseY, a);
        int y = 75;
        if (!this.previousEdit.getValue().isEmpty()) {
            y += 5 * 9 + 1 + this.getPreviousY() - 135;
            graphics.text(this.font, PREVIOUS_OUTPUT_LABEL, this.width / 2 - 150 + 1, y + 4, -6250336);
            this.previousEdit.extractRenderState(graphics, mouseX, mouseY, a);
        }

        this.commandSuggestions.extractRenderState(graphics, mouseX, mouseY);
    }
}