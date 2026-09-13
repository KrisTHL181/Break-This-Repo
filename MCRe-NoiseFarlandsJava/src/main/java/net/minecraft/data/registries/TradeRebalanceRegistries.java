package net.minecraft.data.registries;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.trading.TradeRebalanceVillagerTrades;

public class TradeRebalanceRegistries {
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder().add(Registries.VILLAGER_TRADE, TradeRebalanceVillagerTrades::bootstrap);

    public static CompletableFuture<RegistrySetBuilder.PatchedRegistries> createLookup(final CompletableFuture<HolderLookup.Provider> vanilla) {
        return RegistryPatchGenerator.createLookup(vanilla, BUILDER);
    }
}