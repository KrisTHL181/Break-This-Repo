package net.minecraft.client.gui.screens.options.controls;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ControlsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("controls.title");

    private static OptionInstance<?>[] options(final Options options) {
        return new OptionInstance[]{
            options.toggleCrouch(),
            options.toggleSprint(),
            options.toggleAttack(),
            options.toggleUse(),
            options.autoJump(),
            options.sprintWindow(),
            options.operatorItemsTab()
        };
    }

    public ControlsScreen(final Screen lastScreen, final Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        this.list
            .addSmall(
                Button.builder(
                        Component.translatable("options.mouse_settings"), var1 -> this.minecraft.gui.setScreen(new MouseSettingsScreen(this, this.options))
                    )
                    .build(),
                Button.builder(Component.translatable("controls.keybinds"), var1 -> this.minecraft.gui.setScreen(new KeyBindsScreen(this, this.options)))
                    .build()
            );
        this.list.addSmall(options(this.options));
    }
}