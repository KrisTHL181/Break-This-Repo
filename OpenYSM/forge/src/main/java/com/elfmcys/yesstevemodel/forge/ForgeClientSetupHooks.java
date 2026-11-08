package com.elfmcys.yesstevemodel.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.acceleratedrendering.AcceleratedRenderingCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.cosmeticarmorreworked.CosmeticArmorCompat;
import com.elfmcys.yesstevemodel.client.compat.create.CreateCompat;
import com.elfmcys.yesstevemodel.client.compat.curios.CuriosCompat;
import com.elfmcys.yesstevemodel.client.compat.elytraslot.ElytraSlotCompat;
import com.elfmcys.yesstevemodel.client.compat.firstperson.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.compat.gun.tacz.TacCompat;
import com.elfmcys.yesstevemodel.client.compat.immersiveaircraft.ImmersiveAirCraftCompat;
import com.elfmcys.yesstevemodel.client.compat.immersivemelodies.ImmersiveMelodiesCompat;
import com.elfmcys.yesstevemodel.client.compat.ironsspellbooks.SpellbooksCompat;
import com.elfmcys.yesstevemodel.client.compat.oculus.OculusCompat;
import com.elfmcys.yesstevemodel.client.compat.optifine.OptiFineDetector;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParcoolCompat;
import com.elfmcys.yesstevemodel.client.compat.playeranimator.PlayerAnimatorCompat;
import com.elfmcys.yesstevemodel.client.compat.realcamera.RealCameraCompat;
import com.elfmcys.yesstevemodel.client.compat.sbackpack.SBackpackCompat;
import com.elfmcys.yesstevemodel.client.compat.simplehats.SimpleHatsHelper;
import com.elfmcys.yesstevemodel.client.compat.simpleplanes.SimplePlanesCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SWEMCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.commons.lang3.tuple.Pair;
import rip.ysm.api.client.HudOverlay;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeClientSetupHooks {

    private ForgeClientSetupHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.enqueueWork(() -> {
            CuriosCompat.init();
            FirstPersonCompat.init();
            RealCameraCompat.init();
            PlayerAnimatorCompat.init();
            BetterCombatCompat.init();
            OculusCompat.init();
            AcceleratedRenderingCompat.init();
            OptiFineDetector.init();
            CosmeticArmorCompat.init();
            ElytraSlotCompat.init();
            TacCompat.init();
            SWarfareCompat.init();
            TouhouLittleMaidCompat.init();
            CarryOnCompat.init();
            ParcoolCompat.init();
            SlashBladeCompat.init();
            SWEMCompat.init();
            CreateCompat.init();
            SBackpackCompat.init();
            SimpleHatsHelper.init();
            ImmersiveMelodiesCompat.init();
            SpellbooksCompat.init();
            SimplePlanesCompat.init();
            ImmersiveAirCraftCompat.init();
            showInCompatibleMod(SBackpackCompat.getInCompatibleInfo());
            showInCompatibleMod(ParcoolCompat.getInCompatibleInfo());
            showInCompatibleMod("epicfight", "Epic Fight");

            ClientModelManager.loadDefaultModel();
        });
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        HudOverlay debugOverlay = AnimationDebugOverlay.createOverlay();
        HudOverlay loadingOverlay = new ExtraPlayerOverlay();
        HudOverlay syncOverlay = new ModelSyncStateOverlay();
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "ysm_debug_info", (gui, gfx, partial, w, h) -> debugOverlay.render(gfx, gui.getFont(), partial, w, h));
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "ysm_extra_player", (gui, gfx, partial, w, h) -> loadingOverlay.render(gfx, gui.getFont(), partial, w, h));
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "ysm_loading_state", (gui, gfx, partial, w, h) -> syncOverlay.render(gfx, gui.getFont(), partial, w, h));
    }

    private static void showInCompatibleMod(Optional<Pair<String, String>> optional) {
        optional.ifPresent(pair -> ModLoader.get().addWarning(new ModLoadingWarning(LoadingModList.get().getModFileById(YesSteveModel.MOD_ID).getMods().get(0), ModLoadingStage.SIDED_SETUP, "error.yes_steve_model.incompatible_mod_version", pair.getKey(), pair.getValue())));
    }

    private static void showInCompatibleMod(String str, String str2) {
        if (LoadingModList.get().getModFileById(str) != null) {
            ModLoader.get().addWarning(new ModLoadingWarning(LoadingModList.get().getModFileById(YesSteveModel.MOD_ID).getMods().get(0), ModLoadingStage.SIDED_SETUP, "error.yes_steve_model.incompatible_mod", str2));
        }
    }
}
