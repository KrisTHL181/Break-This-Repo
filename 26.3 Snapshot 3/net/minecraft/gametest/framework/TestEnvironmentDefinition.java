package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.timeline.Timeline;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition<SavedDataType> {
   Codec<TestEnvironmentDefinition<?>> DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE
      .byNameCodec()
      .dispatch(TestEnvironmentDefinition::codec, c -> c);
   Codec<Holder<TestEnvironmentDefinition<?>>> CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

   static MapCodec<? extends TestEnvironmentDefinition<?>> bootstrap(final Registry<MapCodec<? extends TestEnvironmentDefinition<?>>> registry) {
      Registry.register(registry, "all_of", TestEnvironmentDefinition.AllOf.CODEC);
      Registry.register(registry, "clock_time", TestEnvironmentDefinition.ClockTime.CODEC);
      Registry.register(registry, "difficulty", TestEnvironmentDefinition.SetDifficulty.CODEC);
      Registry.register(registry, "dimension", TestEnvironmentDefinition.Dimension.CODEC);
      Registry.register(registry, "function", TestEnvironmentDefinition.Functions.CODEC);
      Registry.register(registry, "game_rules", TestEnvironmentDefinition.SetGameRules.CODEC);
      Registry.register(registry, "timeline_attributes", TestEnvironmentDefinition.Timelines.CODEC);
      return Registry.register(registry, "weather", TestEnvironmentDefinition.Weather.CODEC);
   }

   SavedDataType setup(ServerLevel level);

   void teardown(final ServerLevel level, final SavedDataType saveData);

   MapCodec<? extends TestEnvironmentDefinition<SavedDataType>> codec();

   static <T> TestEnvironmentDefinition.Activation<T> activate(final TestEnvironmentDefinition<T> environment, final ServerLevel level) {
      return new TestEnvironmentDefinition.Activation<>(environment.setup(level), environment, level);
   }

   class Activation<T> {
      private final T value;
      private final TestEnvironmentDefinition<T> definition;
      private final ServerLevel level;

      private Activation(final T value, final TestEnvironmentDefinition<T> definition, final ServerLevel level) {
         this.value = value;
         this.definition = definition;
         this.level = level;
      }

      public void teardown() {
         this.definition.teardown(this.level, this.value);
      }
   }

   record AllOf(List<Holder<TestEnvironmentDefinition<?>>> definitions)
      implements TestEnvironmentDefinition<List<? extends TestEnvironmentDefinition.Activation<?>>> {
      public static final MapCodec<TestEnvironmentDefinition.AllOf> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(TestEnvironmentDefinition.CODEC.listOf().fieldOf("definitions").forGetter(TestEnvironmentDefinition.AllOf::definitions))
            .apply(i, TestEnvironmentDefinition.AllOf::new)
      );

      public AllOf(final TestEnvironmentDefinition<?>... defs) {
         this(Arrays.stream(defs).map(TestEnvironmentDefinition.AllOf::holder).toList());
      }

      private static Holder<TestEnvironmentDefinition<?>> holder(final TestEnvironmentDefinition<?> holder) {
         return Holder.direct(holder);
      }

      public List<? extends TestEnvironmentDefinition.Activation<?>> setup(final ServerLevel level) {
         return this.definitions.stream().map(b -> TestEnvironmentDefinition.activate(b.value(), level)).toList();
      }

      public void teardown(final ServerLevel level, final List<? extends TestEnvironmentDefinition.Activation<?>> activations) {
         activations.reversed().forEach(TestEnvironmentDefinition.Activation::teardown);
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.AllOf> codec() {
         return CODEC;
      }
   }

   record ClockTime(Holder<WorldClock> clock, int time) implements TestEnvironmentDefinition<Long> {
      public static final MapCodec<TestEnvironmentDefinition.ClockTime> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               WorldClock.CODEC.fieldOf("clock").forGetter(TestEnvironmentDefinition.ClockTime::clock),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TestEnvironmentDefinition.ClockTime::time)
            )
            .apply(i, TestEnvironmentDefinition.ClockTime::new)
      );

      public Long setup(final ServerLevel level) {
         MinecraftServer server = level.getServer();
         long previous = server.clockManager().getInstance(this.clock).totalTicks();
         server.clockManager().setTotalTicks(this.clock, this.time);
         return previous;
      }

      public void teardown(final ServerLevel level, final Long saveData) {
         MinecraftServer server = level.getServer();
         server.clockManager().setTotalTicks(this.clock, saveData);
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.ClockTime> codec() {
         return CODEC;
      }
   }

   record Dimension(TestEnvironmentDefinition.Dimension.Type type) implements TestEnvironmentDefinition<TestEnvironmentDefinition.Dimension.Type> {
      public static final MapCodec<TestEnvironmentDefinition.Dimension> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(TestEnvironmentDefinition.Dimension.Type.CODEC.fieldOf("dimension").forGetter(TestEnvironmentDefinition.Dimension::type))
            .apply(instance, TestEnvironmentDefinition.Dimension::new)
      );

      public TestEnvironmentDefinition.Dimension.Type setup(final ServerLevel level) {
         return this.type;
      }

      public void teardown(final ServerLevel level, final TestEnvironmentDefinition.Dimension.Type savedData) {
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.Dimension> codec() {
         return CODEC;
      }

      public enum Type implements StringRepresentable {
         OVERWORLD("overworld"),
         NETHER("nether"),
         END("end");

         public static final Codec<TestEnvironmentDefinition.Dimension.Type> CODEC = StringRepresentable.fromEnum(
            TestEnvironmentDefinition.Dimension.Type::values
         );
         private final String id;

         Type(final String id) {
            this.id = id;
         }

         @Override
         public String getSerializedName() {
            return this.id;
         }
      }
   }

   record Functions(Optional<Identifier> setupFunction, Optional<Identifier> teardownFunction) implements TestEnvironmentDefinition<Unit> {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<TestEnvironmentDefinition.Functions> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Identifier.CODEC.optionalFieldOf("setup").forGetter(TestEnvironmentDefinition.Functions::setupFunction),
               Identifier.CODEC.optionalFieldOf("teardown").forGetter(TestEnvironmentDefinition.Functions::teardownFunction)
            )
            .apply(i, TestEnvironmentDefinition.Functions::new)
      );

      public Unit setup(final ServerLevel level) {
         this.setupFunction.ifPresent(p -> run(level, p));
         return Unit.INSTANCE;
      }

      public void teardown(final ServerLevel level, final Unit saveData) {
         this.teardownFunction.ifPresent(p -> run(level, p));
      }

      private static void run(final ServerLevel level, final Identifier functionId) {
         MinecraftServer server = level.getServer();
         ServerFunctionManager functions = server.getFunctions();
         Optional<CommandFunction<CommandSourceStack>> function = functions.get(functionId);
         if (function.isPresent()) {
            CommandSourceStack source = server.createCommandSourceStack()
               .withPermission(LevelBasedPermissionSet.GAMEMASTER)
               .withSuppressedOutput()
               .withLevel(level);
            functions.execute(function.get(), source);
         } else {
            LOGGER.error("Test Batch failed for non-existent function {}", functionId);
         }
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.Functions> codec() {
         return CODEC;
      }
   }

   record SetDifficulty(Difficulty difficulty) implements TestEnvironmentDefinition<Difficulty> {
      public static final MapCodec<TestEnvironmentDefinition.SetDifficulty> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Difficulty.CODEC.fieldOf("difficulty").forGetter(TestEnvironmentDefinition.SetDifficulty::difficulty))
            .apply(i, TestEnvironmentDefinition.SetDifficulty::new)
      );

      public Difficulty setup(final ServerLevel level) {
         Difficulty oldDifficulty = level.getDifficulty();
         level.getServer().setDifficulty(this.difficulty, true);
         return oldDifficulty;
      }

      public void teardown(final ServerLevel level, final Difficulty saveData) {
         level.getServer().setDifficulty(saveData, true);
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.SetDifficulty> codec() {
         return CODEC;
      }
   }

   record SetGameRules(GameRuleMap gameRulesMap) implements TestEnvironmentDefinition<GameRuleMap> {
      public static final MapCodec<TestEnvironmentDefinition.SetGameRules> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(GameRuleMap.CODEC.fieldOf("rules").forGetter(TestEnvironmentDefinition.SetGameRules::gameRulesMap))
            .apply(i, TestEnvironmentDefinition.SetGameRules::new)
      );

      public GameRuleMap setup(final ServerLevel level) {
         GameRuleMap originalState = GameRuleMap.of();
         GameRules gameRules = level.getGameRules();
         this.gameRulesMap.keySet().forEach(rule -> setFromActive(originalState, (GameRule<?>)rule, gameRules));
         gameRules.setAll(this.gameRulesMap, level.getServer());
         return originalState;
      }

      private static <T> void setFromActive(final GameRuleMap map, final GameRule<T> rule, final GameRules rules) {
         map.set(rule, rules.get(rule));
      }

      public void teardown(final ServerLevel level, final GameRuleMap saveData) {
         level.getGameRules().setAll(saveData, level.getServer());
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.SetGameRules> codec() {
         return CODEC;
      }
   }

   record Timelines(List<Holder<Timeline>> timelines) implements TestEnvironmentDefinition<EnvironmentAttributeSystem> {
      public static final MapCodec<TestEnvironmentDefinition.Timelines> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Timeline.CODEC.listOf().fieldOf("timelines").forGetter(TestEnvironmentDefinition.Timelines::timelines))
            .apply(i, TestEnvironmentDefinition.Timelines::new)
      );

      public EnvironmentAttributeSystem setup(final ServerLevel level) {
         EnvironmentAttributeSystem.Builder builder = EnvironmentAttributeSystem.builder().addDefaultLayers(level);

         for (Holder<Timeline> timeline : this.timelines) {
            builder.addTimelineLayer(timeline, level.clockManager());
         }

         return level.setEnvironmentAttributes(builder.build());
      }

      public void teardown(final ServerLevel level, final EnvironmentAttributeSystem saveData) {
         level.setEnvironmentAttributes(saveData);
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.Timelines> codec() {
         return CODEC;
      }
   }

   record Weather(TestEnvironmentDefinition.Weather.Type weather) implements TestEnvironmentDefinition<TestEnvironmentDefinition.Weather.Type> {
      public static final MapCodec<TestEnvironmentDefinition.Weather> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(TestEnvironmentDefinition.Weather::weather))
            .apply(i, TestEnvironmentDefinition.Weather::new)
      );

      public TestEnvironmentDefinition.Weather.Type setup(final ServerLevel level) {
         TestEnvironmentDefinition.Weather.Type previous;
         if (level.isThundering()) {
            previous = TestEnvironmentDefinition.Weather.Type.THUNDER;
         } else if (level.isRaining()) {
            previous = TestEnvironmentDefinition.Weather.Type.RAIN;
         } else {
            previous = TestEnvironmentDefinition.Weather.Type.CLEAR;
         }

         this.weather.apply(level);
         return previous;
      }

      public void teardown(final ServerLevel level, final TestEnvironmentDefinition.Weather.Type saveData) {
         level.resetWeatherCycle();
         saveData.apply(level);
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.Weather> codec() {
         return CODEC;
      }

      public enum Type implements StringRepresentable {
         CLEAR("clear", 100000, 0, false, false),
         RAIN("rain", 0, 100000, true, false),
         THUNDER("thunder", 0, 100000, true, true);

         public static final Codec<TestEnvironmentDefinition.Weather.Type> CODEC = StringRepresentable.fromEnum(TestEnvironmentDefinition.Weather.Type::values);
         private final String id;
         private final int clearTime;
         private final int rainTime;
         private final boolean raining;
         private final boolean thundering;

         Type(final String id, final int clearTime, final int rainTime, final boolean raining, final boolean thundering) {
            this.id = id;
            this.clearTime = clearTime;
            this.rainTime = rainTime;
            this.raining = raining;
            this.thundering = thundering;
         }

         public void apply(final ServerLevel level) {
            level.getServer().setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
         }

         @Override
         public String getSerializedName() {
            return this.id;
         }
      }
   }
}
