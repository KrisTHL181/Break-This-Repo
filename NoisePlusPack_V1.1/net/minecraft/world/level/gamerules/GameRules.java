package net.minecraft.world.level.gamerules;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.Nullable;

public class GameRules {
   public static final GameRule<Boolean> ADVANCE_TIME = registerBoolean("advance_time", GameRuleCategory.UPDATES, !SharedConstants.DEBUG_WORLD_RECREATE);
   public static final GameRule<Boolean> ADVANCE_WEATHER = registerBoolean("advance_weather", GameRuleCategory.UPDATES, !SharedConstants.DEBUG_WORLD_RECREATE);
   public static final GameRule<Boolean> ALLOW_ENTERING_NETHER_USING_PORTALS = registerBoolean(
      "allow_entering_nether_using_portals", GameRuleCategory.MISC, true
   );
   public static final GameRule<Boolean> BLOCK_DROPS = registerBoolean("block_drops", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY = registerBoolean("block_explosion_drop_decay", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> COMMAND_BLOCKS_WORK = registerBoolean("command_blocks_work", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> COMMAND_BLOCK_OUTPUT = registerBoolean("command_block_output", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> DROWNING_DAMAGE = registerBoolean("drowning_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> ELYTRA_MOVEMENT_CHECK = registerBoolean("elytra_movement_check", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH = registerBoolean("ender_pearls_vanish_on_death", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> ENTITY_DROPS = registerBoolean("entity_drops", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> FALL_DAMAGE = registerBoolean("fall_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> FIRE_DAMAGE = registerBoolean("fire_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Integer> FIRE_SPREAD_RADIUS_AROUND_PLAYER = registerInteger(
      "fire_spread_radius_around_player", GameRuleCategory.UPDATES, 128, -1
   );
   public static final GameRule<Boolean> FORGIVE_DEAD_PLAYERS = registerBoolean("forgive_dead_players", GameRuleCategory.MOBS, true);
   public static final GameRule<Boolean> FREEZE_DAMAGE = registerBoolean("freeze_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> GLOBAL_SOUND_EVENTS = registerBoolean("global_sound_events", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> IMMEDIATE_RESPAWN = registerBoolean("immediate_respawn", GameRuleCategory.PLAYER, false);
   public static final GameRule<Boolean> KEEP_INVENTORY = registerBoolean("keep_inventory", GameRuleCategory.PLAYER, false);
   public static final GameRule<Boolean> LAVA_SOURCE_CONVERSION = registerBoolean("lava_source_conversion", GameRuleCategory.UPDATES, false);
   public static final GameRule<Boolean> LIMITED_CRAFTING = registerBoolean("limited_crafting", GameRuleCategory.PLAYER, false);
   public static final GameRule<Boolean> LOCATOR_BAR = registerBoolean("locator_bar", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> LOG_ADMIN_COMMANDS = registerBoolean("log_admin_commands", GameRuleCategory.CHAT, true);
   public static final GameRule<Integer> MAX_BLOCK_MODIFICATIONS = registerInteger("max_block_modifications", GameRuleCategory.MISC, 32768, 1);
   public static final GameRule<Integer> MAX_COMMAND_FORKS = registerInteger("max_command_forks", GameRuleCategory.MISC, 65536, 0);
   public static final GameRule<Integer> MAX_COMMAND_SEQUENCE_LENGTH = registerInteger("max_command_sequence_length", GameRuleCategory.MISC, 65536, 0);
   public static final GameRule<Integer> MAX_ENTITY_CRAMMING = registerInteger("max_entity_cramming", GameRuleCategory.MOBS, 24, 0);
   public static final GameRule<Integer> MAX_MINECART_SPEED = registerInteger(
      "max_minecart_speed", GameRuleCategory.MISC, 8, 1, 1000, FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
   );
   public static final GameRule<Integer> MAX_SNOW_ACCUMULATION_HEIGHT = registerInteger("max_snow_accumulation_height", GameRuleCategory.UPDATES, 1, 0, 8);
   public static final GameRule<Boolean> MOB_DROPS = registerBoolean("mob_drops", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> MOB_EXPLOSION_DROP_DECAY = registerBoolean("mob_explosion_drop_decay", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> MOB_GRIEFING = registerBoolean("mob_griefing", GameRuleCategory.MOBS, true);
   public static final GameRule<Boolean> NATURAL_HEALTH_REGENERATION = registerBoolean("natural_health_regeneration", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> PLAYER_MOVEMENT_CHECK = registerBoolean("player_movement_check", GameRuleCategory.PLAYER, true);
   public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = registerInteger(
      "players_nether_portal_creative_delay", GameRuleCategory.PLAYER, 0, 0
   );
   public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = registerInteger(
      "players_nether_portal_default_delay", GameRuleCategory.PLAYER, 80, 0
   );
   public static final GameRule<Integer> PLAYERS_SLEEPING_PERCENTAGE = registerInteger("players_sleeping_percentage", GameRuleCategory.PLAYER, 100, 0);
   public static final GameRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS = registerBoolean("projectiles_can_break_blocks", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> PVP = registerBoolean("pvp", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> RAIDS = registerBoolean("raids", GameRuleCategory.MOBS, true);
   public static final GameRule<Integer> RANDOM_TICK_SPEED = registerInteger("random_tick_speed", GameRuleCategory.UPDATES, 3, 0);
   public static final GameRule<Boolean> REDUCED_DEBUG_INFO = registerBoolean("reduced_debug_info", GameRuleCategory.MISC, false);
   public static final GameRule<Integer> RESPAWN_RADIUS = registerInteger("respawn_radius", GameRuleCategory.PLAYER, 10, 0);
   public static final GameRule<Boolean> SEND_COMMAND_FEEDBACK = registerBoolean("send_command_feedback", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> SHOW_ADVANCEMENT_MESSAGES = registerBoolean("show_advancement_messages", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> SHOW_DEATH_MESSAGES = registerBoolean("show_death_messages", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> SPAWNER_BLOCKS_WORK = registerBoolean("spawner_blocks_work", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> SPAWN_MOBS = registerBoolean("spawn_mobs", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_MONSTERS = registerBoolean("spawn_monsters", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_PATROLS = registerBoolean("spawn_patrols", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_PHANTOMS = registerBoolean("spawn_phantoms", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_WANDERING_TRADERS = registerBoolean("spawn_wandering_traders", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_WARDENS = registerBoolean("spawn_wardens", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPECTATORS_GENERATE_CHUNKS = registerBoolean("spectators_generate_chunks", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> SPREAD_VINES = registerBoolean("spread_vines", GameRuleCategory.UPDATES, true);
   public static final GameRule<Boolean> TNT_EXPLODES = registerBoolean("tnt_explodes", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> TNT_EXPLOSION_DROP_DECAY = registerBoolean("tnt_explosion_drop_decay", GameRuleCategory.DROPS, false);
   public static final GameRule<Boolean> UNIVERSAL_ANGER = registerBoolean("universal_anger", GameRuleCategory.MOBS, false);
   public static final GameRule<Boolean> WATER_SOURCE_CONVERSION = registerBoolean("water_source_conversion", GameRuleCategory.UPDATES, true);
   private final GameRuleMap rules;

   public static Codec<GameRules> codec(FeatureFlagSet p_459260_) {
      return GameRuleMap.CODEC.xmap(p_455040_ -> new GameRules(p_459260_, p_455040_), p_457611_ -> p_457611_.rules);
   }

   public GameRules(FeatureFlagSet p_451018_, GameRuleMap p_460445_) {
      this(p_451018_);
      this.rules.setFromIf(p_460445_, this.rules::has);
   }

   public GameRules(FeatureFlagSet p_453502_) {
      this.rules = GameRuleMap.of(BuiltInRegistries.GAME_RULE.filterFeatures(p_453502_).listElements().map(Holder::value));
   }

   public Stream<GameRule<?>> availableRules() {
      return this.rules.keySet().stream();
   }

   public <T> T get(GameRule<T> p_458251_) {
      T t = this.rules.get(p_458251_);
      if (t == null) {
         throw new IllegalArgumentException("Tried to access invalid game rule");
      } else {
         return t;
      }
   }

   public <T> void set(GameRule<T> p_457739_, T p_450832_, @Nullable MinecraftServer p_453139_) {
      if (!this.rules.has(p_457739_)) {
         throw new IllegalArgumentException("Tried to set invalid game rule");
      }

      this.rules.set(p_457739_, p_450832_);
      if (p_453139_ != null) {
         p_453139_.onGameRuleChanged(p_457739_, p_450832_);
      }
   }

   public GameRules copy(FeatureFlagSet p_460814_) {
      return new GameRules(p_460814_, this.rules);
   }

   public void setAll(GameRules p_456658_, @Nullable MinecraftServer p_455033_) {
      this.setAll(p_456658_.rules, p_455033_);
   }

   public void setAll(GameRuleMap p_452499_, @Nullable MinecraftServer p_450469_) {
      p_452499_.keySet().forEach(p_455800_ -> this.setFromOther(p_452499_, (GameRule<?>)p_455800_, p_450469_));
   }

   private <T> void setFromOther(GameRuleMap p_450857_, GameRule<T> p_459006_, @Nullable MinecraftServer p_456260_) {
      this.set(p_459006_, Objects.requireNonNull(p_450857_.get(p_459006_)), p_456260_);
   }

   public void visitGameRuleTypes(GameRuleTypeVisitor p_456778_) {
      this.rules.keySet().forEach(p_452255_ -> {
         p_456778_.visit((GameRule<?>)p_452255_);
         p_452255_.callVisitor(p_456778_);
      });
   }

   private static GameRule<Boolean> registerBoolean(String p_455902_, GameRuleCategory p_459816_, boolean p_453746_) {
      return register(
         p_455902_,
         p_459816_,
         GameRuleType.BOOL,
         BoolArgumentType.bool(),
         Codec.BOOL,
         p_453746_,
         FeatureFlagSet.of(),
         GameRuleTypeVisitor::visitBoolean,
         p_456160_ -> p_456160_ ? 1 : 0
      );
   }

   private static GameRule<Integer> registerInteger(String p_458768_, GameRuleCategory p_459992_, int p_458612_, int p_456323_) {
      return registerInteger(p_458768_, p_459992_, p_458612_, p_456323_, Integer.MAX_VALUE, FeatureFlagSet.of());
   }

   private static GameRule<Integer> registerInteger(String p_455197_, GameRuleCategory p_457049_, int p_453077_, int p_460312_, int p_454872_) {
      return registerInteger(p_455197_, p_457049_, p_453077_, p_460312_, p_454872_, FeatureFlagSet.of());
   }

   private static GameRule<Integer> registerInteger(
      String p_451631_, GameRuleCategory p_451138_, int p_451705_, int p_456148_, int p_454377_, FeatureFlagSet p_460866_
   ) {
      return register(
         p_451631_,
         p_451138_,
         GameRuleType.INT,
         IntegerArgumentType.integer(p_456148_, p_454377_),
         Codec.intRange(p_456148_, p_454377_),
         p_451705_,
         p_460866_,
         GameRuleTypeVisitor::visitInteger,
         p_456700_ -> p_456700_
      );
   }

   private static <T> GameRule<T> register(
      String p_460600_,
      GameRuleCategory p_455360_,
      GameRuleType p_454019_,
      ArgumentType<T> p_459286_,
      Codec<T> p_453993_,
      T p_455662_,
      FeatureFlagSet p_459432_,
      GameRules.VisitorCaller<T> p_456018_,
      ToIntFunction<T> p_456872_
   ) {
      return Registry.register(
         BuiltInRegistries.GAME_RULE, p_460600_, new GameRule<>(p_455360_, p_454019_, p_459286_, p_456018_, p_453993_, p_456872_, p_455662_, p_459432_)
      );
   }

   public static GameRule<?> bootstrap(Registry<GameRule<?>> p_456866_) {
      return ADVANCE_TIME;
   }

   public <T> String getAsString(GameRule<T> p_454443_) {
      return p_454443_.serialize(this.get(p_454443_));
   }

   public interface VisitorCaller<T> {
      void call(GameRuleTypeVisitor var1, GameRule<T> var2);
   }
}
