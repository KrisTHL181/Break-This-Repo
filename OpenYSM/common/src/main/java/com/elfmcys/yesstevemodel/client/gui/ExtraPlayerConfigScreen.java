package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.LoadingStateConfig;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import rip.ysm.gui.Option;
import rip.ysm.gui.OptionGroup;
import rip.ysm.gui.OptionScreen;
import rip.ysm.gui.components.BooleanOptionRow;
import rip.ysm.gui.components.EnumOptionRow;
import rip.ysm.gui.components.SliderOptionRow;

public class ExtraPlayerConfigScreen extends OptionScreen {

    public ExtraPlayerConfigScreen(@Nullable PlayerModelScreen modelScreen) {
        super(Component.literal("OpenYSM"), modelScreen);
    }

    @Override
    protected void registerGroups() {
        OptionGroup general = new OptionGroup("general")
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofDouble("sound_volume", GeneralConfig.SOUND_VOLUME), 0.0d, 100.0d, 1.0d, "%"))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_self_model", GeneralConfig.DISABLE_SELF_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_other_model", GeneralConfig.DISABLE_OTHER_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_self_hands", GeneralConfig.DISABLE_SELF_HANDS)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("force_client_mode", GeneralConfig.FORCE_CLIENT_MODE)))
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofDouble("handshake_timeout", GeneralConfig.HANDSHAKE_TIMEOUT), 1.0d, 60.0d, 1.0d, "s"))
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofDouble("search_suggestion_count", GeneralConfig.SEARCH_SUGGESTION_COUNT), 1.0d, 30.0d, 1.0d, ""));

        OptionGroup rendering = new OptionGroup("rendering")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_player_render", ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_player_render_third_person", ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER_THIRD_PERSON)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_projectile_model", GeneralConfig.DISABLE_PROJECTILE_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_vehicle_model", GeneralConfig.DISABLE_VEHICLE_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_external_first_person_anim", GeneralConfig.DISABLE_EXTERNAL_FP_ANIM)));

        OptionGroup performance = new OptionGroup("performance")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("lazy_model_loading", GeneralConfig.LAZY_MODEL_LOADING)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("use_compatibility_renderer", GeneralConfig.USE_COMPATIBILITY_RENDERER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("use_gpu_renderer", GeneralConfig.USE_GPU_RENDERER)));

        OptionGroup misc = new OptionGroup("misc")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("print_animation_roulette_msg", GeneralConfig.PRINT_ANIMATION_ROULETTE_MSG)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_loading_state_screen", LoadingStateConfig.DISABLE_LOADING_STATE_SCREEN)))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("loading_state_position", LoadingStateConfig.LOADING_STATE_POSITION), LoadingStateConfig.Position.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("roulette_settings_mode", GeneralConfig.ROULETTE_SETTINGS_MODE), GeneralConfig.RouletteSettingsMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("roulette_mode", GeneralConfig.ROULETTE_MODE), GeneralConfig.RouletteMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("texture_screen_mode", GeneralConfig.TEXTURE_SCREEN_MODE), GeneralConfig.TextureScreenMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("model_info_screen_mode", GeneralConfig.MODEL_INFO_SCREEN_MODE), GeneralConfig.ModelInfoScreenMode.values()))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("blur_gui", GeneralConfig.BLUR_GUI)));

        groups.add(general);
        groups.add(rendering);
        groups.add(performance);
        groups.add(misc);
    }
}
