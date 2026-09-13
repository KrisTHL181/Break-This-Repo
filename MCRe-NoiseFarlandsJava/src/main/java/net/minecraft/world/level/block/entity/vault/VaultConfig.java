package net.minecraft.world.level.block.entity.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public record VaultConfig(
    ResourceKey<LootTable> lootTable,
    double activationRange,
    double deactivationRange,
    ItemStack keyItem,
    Optional<ResourceKey<LootTable>> overrideLootTableToDisplay,
    PlayerDetector playerDetector,
    PlayerDetector.EntitySelector entitySelector
) {
    public static final String TAG_NAME = "config";
    public static final VaultConfig DEFAULT = new VaultConfig();
    public static final Codec<VaultConfig> CODEC = RecordCodecBuilder.<VaultConfig>create(
            i -> i.group(
                    LootTable.KEY_CODEC.lenientOptionalFieldOf("loot_table", DEFAULT.lootTable()).forGetter(VaultConfig::lootTable),
                    Codec.DOUBLE.lenientOptionalFieldOf("activation_range", DEFAULT.activationRange()).forGetter(VaultConfig::activationRange),
                    Codec.DOUBLE.lenientOptionalFieldOf("deactivation_range", DEFAULT.deactivationRange()).forGetter(VaultConfig::deactivationRange),
                    ItemStack.lenientOptionalFieldOf("key_item").forGetter(VaultConfig::keyItem),
                    LootTable.KEY_CODEC.lenientOptionalFieldOf("override_loot_table_to_display").forGetter(VaultConfig::overrideLootTableToDisplay)
                )
                .apply(i, VaultConfig::new)
        )
        .validate(VaultConfig::validate);

    private VaultConfig() {
        this(
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD,
            4.0,
            4.5,
            new ItemStack(Items.TRIAL_KEY),
            Optional.empty(),
            PlayerDetector.INCLUDING_CREATIVE_PLAYERS,
            PlayerDetector.EntitySelector.SELECT_FROM_LEVEL
        );
    }

    public VaultConfig(
        final ResourceKey<LootTable> lootTable,
        final double activationRange,
        final double deactivationRange,
        final ItemStack keyItem,
        final Optional<ResourceKey<LootTable>> overrideDisplayItems
    ) {
        this(lootTable, activationRange, deactivationRange, keyItem, overrideDisplayItems, DEFAULT.playerDetector(), DEFAULT.entitySelector());
    }

    public PlayerDetector playerDetector() {
        return SharedConstants.DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS ? PlayerDetector.SHEEP : this.playerDetector;
    }

    private DataResult<VaultConfig> validate() {
        return this.activationRange > this.deactivationRange
            ? DataResult.error(
                () -> "Activation range must (" + this.activationRange + ") be less or equal to deactivation range (" + this.deactivationRange + ")"
            )
            : DataResult.success(this);
    }
}