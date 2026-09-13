package net.minecraft.world.level.block.entity;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

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
   private static final Map<Item, ResourceKey<DecoratedPotPattern>> ITEM_TO_POT_TEXTURE = Map.ofEntries(
      Map.entry(Items.BRICK, BLANK),
      Map.entry(Items.ANGLER_POTTERY_SHERD, ANGLER),
      Map.entry(Items.ARCHER_POTTERY_SHERD, ARCHER),
      Map.entry(Items.ARMS_UP_POTTERY_SHERD, ARMS_UP),
      Map.entry(Items.BLADE_POTTERY_SHERD, BLADE),
      Map.entry(Items.BREWER_POTTERY_SHERD, BREWER),
      Map.entry(Items.BURN_POTTERY_SHERD, BURN),
      Map.entry(Items.DANGER_POTTERY_SHERD, DANGER),
      Map.entry(Items.EXPLORER_POTTERY_SHERD, EXPLORER),
      Map.entry(Items.FLOW_POTTERY_SHERD, FLOW),
      Map.entry(Items.FRIEND_POTTERY_SHERD, FRIEND),
      Map.entry(Items.GUSTER_POTTERY_SHERD, GUSTER),
      Map.entry(Items.HEART_POTTERY_SHERD, HEART),
      Map.entry(Items.HEARTBREAK_POTTERY_SHERD, HEARTBREAK),
      Map.entry(Items.HOWL_POTTERY_SHERD, HOWL),
      Map.entry(Items.MINER_POTTERY_SHERD, MINER),
      Map.entry(Items.MOURNER_POTTERY_SHERD, MOURNER),
      Map.entry(Items.PLENTY_POTTERY_SHERD, PLENTY),
      Map.entry(Items.PRIZE_POTTERY_SHERD, PRIZE),
      Map.entry(Items.SCRAPE_POTTERY_SHERD, SCRAPE),
      Map.entry(Items.SHEAF_POTTERY_SHERD, SHEAF),
      Map.entry(Items.SHELTER_POTTERY_SHERD, SHELTER),
      Map.entry(Items.SKULL_POTTERY_SHERD, SKULL),
      Map.entry(Items.SNORT_POTTERY_SHERD, SNORT)
   );

   public static @Nullable ResourceKey<DecoratedPotPattern> getPatternFromItem(Item p_273094_) {
      return ITEM_TO_POT_TEXTURE.get(p_273094_);
   }

   private static ResourceKey<DecoratedPotPattern> create(String p_272919_) {
      return ResourceKey.create(Registries.DECORATED_POT_PATTERN, Identifier.withDefaultNamespace(p_272919_));
   }

   public static DecoratedPotPattern bootstrap(Registry<DecoratedPotPattern> p_273479_) {
      register(p_273479_, ANGLER, "angler_pottery_pattern");
      register(p_273479_, ARCHER, "archer_pottery_pattern");
      register(p_273479_, ARMS_UP, "arms_up_pottery_pattern");
      register(p_273479_, BLADE, "blade_pottery_pattern");
      register(p_273479_, BREWER, "brewer_pottery_pattern");
      register(p_273479_, BURN, "burn_pottery_pattern");
      register(p_273479_, DANGER, "danger_pottery_pattern");
      register(p_273479_, EXPLORER, "explorer_pottery_pattern");
      register(p_273479_, FLOW, "flow_pottery_pattern");
      register(p_273479_, FRIEND, "friend_pottery_pattern");
      register(p_273479_, GUSTER, "guster_pottery_pattern");
      register(p_273479_, HEART, "heart_pottery_pattern");
      register(p_273479_, HEARTBREAK, "heartbreak_pottery_pattern");
      register(p_273479_, HOWL, "howl_pottery_pattern");
      register(p_273479_, MINER, "miner_pottery_pattern");
      register(p_273479_, MOURNER, "mourner_pottery_pattern");
      register(p_273479_, PLENTY, "plenty_pottery_pattern");
      register(p_273479_, PRIZE, "prize_pottery_pattern");
      register(p_273479_, SCRAPE, "scrape_pottery_pattern");
      register(p_273479_, SHEAF, "sheaf_pottery_pattern");
      register(p_273479_, SHELTER, "shelter_pottery_pattern");
      register(p_273479_, SKULL, "skull_pottery_pattern");
      register(p_273479_, SNORT, "snort_pottery_pattern");
      return register(p_273479_, BLANK, "decorated_pot_side");
   }

   private static DecoratedPotPattern register(Registry<DecoratedPotPattern> p_345377_, ResourceKey<DecoratedPotPattern> p_345043_, String p_344357_) {
      return Registry.register(p_345377_, p_345043_, new DecoratedPotPattern(Identifier.withDefaultNamespace(p_344357_)));
   }
}
