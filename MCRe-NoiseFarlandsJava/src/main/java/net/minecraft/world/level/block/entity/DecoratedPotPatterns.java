package net.minecraft.world.level.block.entity;

import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class DecoratedPotPatterns {
    public static final ResourceKey<DecoratedPotPattern> BLANK = create("blank");
    public static final ResourceKey<DecoratedPotPattern> ANGLER = create("angler");
    public static final ResourceKey<DecoratedPotPattern> ARCHER = create("archer");
    public static final ResourceKey<DecoratedPotPattern> ARMS_UP = create("arms_up");
    public static final ResourceKey<DecoratedPotPattern> BLADE = create("blade");
    public static final ResourceKey<DecoratedPotPattern> BREWER = create("brewer");
    public static final ResourceKey<DecoratedPotPattern> BURN = create("burn");
    public static final ResourceKey<DecoratedPotPattern> DANGER = create("danger");
    public static final ResourceKey<DecoratedPotPattern> EXPLORER = create("explorer");
    public static final ResourceKey<DecoratedPotPattern> FLOW = create("flow");
    public static final ResourceKey<DecoratedPotPattern> FRIEND = create("friend");
    public static final ResourceKey<DecoratedPotPattern> GUSTER = create("guster");
    public static final ResourceKey<DecoratedPotPattern> HEART = create("heart");
    public static final ResourceKey<DecoratedPotPattern> HEARTBREAK = create("heartbreak");
    public static final ResourceKey<DecoratedPotPattern> HOWL = create("howl");
    public static final ResourceKey<DecoratedPotPattern> MINER = create("miner");
    public static final ResourceKey<DecoratedPotPattern> MOURNER = create("mourner");
    public static final ResourceKey<DecoratedPotPattern> PLENTY = create("plenty");
    public static final ResourceKey<DecoratedPotPattern> PRIZE = create("prize");
    public static final ResourceKey<DecoratedPotPattern> SCRAPE = create("scrape");
    public static final ResourceKey<DecoratedPotPattern> SHEAF = create("sheaf");
    public static final ResourceKey<DecoratedPotPattern> SHELTER = create("shelter");
    public static final ResourceKey<DecoratedPotPattern> SKULL = create("skull");
    public static final ResourceKey<DecoratedPotPattern> SNORT = create("snort");

    public static void itemToPatternMappings(final BiConsumer<ResourceKey<Item>, ResourceKey<DecoratedPotPattern>> itemToPattern) {
        itemToPattern.accept(ItemIds.BRICK, BLANK);
        itemToPattern.accept(ItemIds.ANGLER_POTTERY_SHERD, ANGLER);
        itemToPattern.accept(ItemIds.ARCHER_POTTERY_SHERD, ARCHER);
        itemToPattern.accept(ItemIds.ARMS_UP_POTTERY_SHERD, ARMS_UP);
        itemToPattern.accept(ItemIds.BLADE_POTTERY_SHERD, BLADE);
        itemToPattern.accept(ItemIds.BREWER_POTTERY_SHERD, BREWER);
        itemToPattern.accept(ItemIds.BURN_POTTERY_SHERD, BURN);
        itemToPattern.accept(ItemIds.DANGER_POTTERY_SHERD, DANGER);
        itemToPattern.accept(ItemIds.EXPLORER_POTTERY_SHERD, EXPLORER);
        itemToPattern.accept(ItemIds.FLOW_POTTERY_SHERD, FLOW);
        itemToPattern.accept(ItemIds.FRIEND_POTTERY_SHERD, FRIEND);
        itemToPattern.accept(ItemIds.GUSTER_POTTERY_SHERD, GUSTER);
        itemToPattern.accept(ItemIds.HEART_POTTERY_SHERD, HEART);
        itemToPattern.accept(ItemIds.HEARTBREAK_POTTERY_SHERD, HEARTBREAK);
        itemToPattern.accept(ItemIds.HOWL_POTTERY_SHERD, HOWL);
        itemToPattern.accept(ItemIds.MINER_POTTERY_SHERD, MINER);
        itemToPattern.accept(ItemIds.MOURNER_POTTERY_SHERD, MOURNER);
        itemToPattern.accept(ItemIds.PLENTY_POTTERY_SHERD, PLENTY);
        itemToPattern.accept(ItemIds.PRIZE_POTTERY_SHERD, PRIZE);
        itemToPattern.accept(ItemIds.SCRAPE_POTTERY_SHERD, SCRAPE);
        itemToPattern.accept(ItemIds.SHEAF_POTTERY_SHERD, SHEAF);
        itemToPattern.accept(ItemIds.SHELTER_POTTERY_SHERD, SHELTER);
        itemToPattern.accept(ItemIds.SKULL_POTTERY_SHERD, SKULL);
        itemToPattern.accept(ItemIds.SNORT_POTTERY_SHERD, SNORT);
    }

    private static ResourceKey<DecoratedPotPattern> create(final String id) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERN, Identifier.withDefaultNamespace(id));
    }

    public static DecoratedPotPattern bootstrap(final Registry<DecoratedPotPattern> registry) {
        register(registry, ANGLER, "angler_pottery_pattern");
        register(registry, ARCHER, "archer_pottery_pattern");
        register(registry, ARMS_UP, "arms_up_pottery_pattern");
        register(registry, BLADE, "blade_pottery_pattern");
        register(registry, BREWER, "brewer_pottery_pattern");
        register(registry, BURN, "burn_pottery_pattern");
        register(registry, DANGER, "danger_pottery_pattern");
        register(registry, EXPLORER, "explorer_pottery_pattern");
        register(registry, FLOW, "flow_pottery_pattern");
        register(registry, FRIEND, "friend_pottery_pattern");
        register(registry, GUSTER, "guster_pottery_pattern");
        register(registry, HEART, "heart_pottery_pattern");
        register(registry, HEARTBREAK, "heartbreak_pottery_pattern");
        register(registry, HOWL, "howl_pottery_pattern");
        register(registry, MINER, "miner_pottery_pattern");
        register(registry, MOURNER, "mourner_pottery_pattern");
        register(registry, PLENTY, "plenty_pottery_pattern");
        register(registry, PRIZE, "prize_pottery_pattern");
        register(registry, SCRAPE, "scrape_pottery_pattern");
        register(registry, SHEAF, "sheaf_pottery_pattern");
        register(registry, SHELTER, "shelter_pottery_pattern");
        register(registry, SKULL, "skull_pottery_pattern");
        register(registry, SNORT, "snort_pottery_pattern");
        return register(registry, BLANK, "decorated_pot_side");
    }

    private static DecoratedPotPattern register(final Registry<DecoratedPotPattern> registry, final ResourceKey<DecoratedPotPattern> id, final String assetId) {
        return Registry.register(registry, id, new DecoratedPotPattern(Identifier.withDefaultNamespace(assetId)));
    }
}