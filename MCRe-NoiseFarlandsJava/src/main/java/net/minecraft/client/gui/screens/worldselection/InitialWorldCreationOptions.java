package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record InitialWorldCreationOptions(
    WorldCreationUiState.SelectedGameMode selectedGameMode, GameRuleMap gameRuleOverwrites, @Nullable ResourceKey<FlatLevelGeneratorPreset> flatLevelPreset
) {
}