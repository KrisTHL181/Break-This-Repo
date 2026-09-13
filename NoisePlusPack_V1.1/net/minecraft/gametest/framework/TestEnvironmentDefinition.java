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
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition {
   Codec<TestEnvironmentDefinition> DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE
      .byNameCodec()
      .dispatch(TestEnvironmentDefinition::codec, p_391906_ -> p_391906_);
   Codec<Holder<TestEnvironmentDefinition>> CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

   static MapCodec<? extends TestEnvironmentDefinition> bootstrap(Registry<MapCodec<? extends TestEnvironmentDefinition>> p_391558_) {
      Registry.register(p_391558_, "all_of", TestEnvironmentDefinition.AllOf.CODEC);
      Registry.register(p_391558_, "game_rules", TestEnvironmentDefinition.SetGameRules.CODEC);
      Registry.register(p_391558_, "time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
      Registry.register(p_391558_, "weather", TestEnvironmentDefinition.Weather.CODEC);
      return Registry.register(p_391558_, "function", TestEnvironmentDefinition.Functions.CODEC);
   }

   void setup(ServerLevel var1);

   default void teardown(ServerLevel p_397794_) {
   }

   MapCodec<? extends TestEnvironmentDefinition> codec();

   record AllOf(List<Holder<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.AllOf> CODEC = RecordCodecBuilder.mapCodec(
         p_394480_ -> p_394480_.group(TestEnvironmentDefinition.CODEC.listOf().fieldOf("definitions").forGetter(TestEnvironmentDefinition.AllOf::definitions))
            .apply(p_394480_, TestEnvironmentDefinition.AllOf::new)
      );

      public AllOf(TestEnvironmentDefinition... p_396434_) {
         this(Arrays.stream(p_396434_).map(Holder::direct).toList());
      }

      @Override
      public void setup(ServerLevel p_395710_) {
         this.definitions.forEach(p_393848_ -> p_393848_.value().setup(p_395710_));
      }

      @Override
      public void teardown(ServerLevel p_391515_) {
         this.definitions.forEach(p_391387_ -> p_391387_.value().teardown(p_391515_));
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.AllOf> codec() {
         return CODEC;
      }
   }

   record Functions(Optional<Identifier> setupFunction, Optional<Identifier> teardownFunction) implements TestEnvironmentDefinition {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<TestEnvironmentDefinition.Functions> CODEC = RecordCodecBuilder.mapCodec(
         p_448764_ -> p_448764_.group(
               Identifier.CODEC.optionalFieldOf("setup").forGetter(TestEnvironmentDefinition.Functions::setupFunction),
               Identifier.CODEC.optionalFieldOf("teardown").forGetter(TestEnvironmentDefinition.Functions::teardownFunction)
            )
            .apply(p_448764_, TestEnvironmentDefinition.Functions::new)
      );

      @Override
      public void setup(ServerLevel p_394609_) {
         this.setupFunction.ifPresent(p_448766_ -> run(p_394609_, p_448766_));
      }

      @Override
      public void teardown(ServerLevel p_392344_) {
         this.teardownFunction.ifPresent(p_448763_ -> run(p_392344_, p_448763_));
      }

      private static void run(ServerLevel p_395465_, Identifier p_455968_) {
         MinecraftServer minecraftserver = p_395465_.getServer();
         ServerFunctionManager serverfunctionmanager = minecraftserver.getFunctions();
         Optional<CommandFunction<CommandSourceStack>> optional = serverfunctionmanager.get(p_455968_);
         if (optional.isPresent()) {
            CommandSourceStack commandsourcestack = minecraftserver.createCommandSourceStack()
               .withPermission(LevelBasedPermissionSet.GAMEMASTER)
               .withSuppressedOutput()
               .withLevel(p_395465_);
            serverfunctionmanager.execute(optional.get(), commandsourcestack);
         } else {
            LOGGER.error("Test Batch failed for non-existent function {}", p_455968_);
         }
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.Functions> codec() {
         return CODEC;
      }
   }

   record SetGameRules(GameRuleMap gameRulesMap) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.SetGameRules> CODEC = RecordCodecBuilder.mapCodec(
         p_448769_ -> p_448769_.group(GameRuleMap.CODEC.fieldOf("rules").forGetter(TestEnvironmentDefinition.SetGameRules::gameRulesMap))
            .apply(p_448769_, TestEnvironmentDefinition.SetGameRules::new)
      );

      @Override
      public void setup(ServerLevel p_395835_) {
         GameRules gamerules = p_395835_.getGameRules();
         MinecraftServer minecraftserver = p_395835_.getServer();
         gamerules.setAll(this.gameRulesMap, minecraftserver);
      }

      @Override
      public void teardown(ServerLevel p_393909_) {
         this.gameRulesMap.keySet().forEach(p_448768_ -> this.resetRule(p_393909_, (GameRule<?>)p_448768_));
      }

      private <T> void resetRule(ServerLevel p_454392_, GameRule<T> p_450537_) {
         p_454392_.getGameRules().set(p_450537_, p_450537_.defaultValue(), p_454392_.getServer());
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.SetGameRules> codec() {
         return CODEC;
      }
   }

   record TimeOfDay(int time) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.TimeOfDay> CODEC = RecordCodecBuilder.mapCodec(
         p_392107_ -> p_392107_.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TestEnvironmentDefinition.TimeOfDay::time))
            .apply(p_392107_, TestEnvironmentDefinition.TimeOfDay::new)
      );

      @Override
      public void setup(ServerLevel p_393027_) {
         p_393027_.setDayTime(this.time);
      }

      @Override
      public MapCodec<TestEnvironmentDefinition.TimeOfDay> codec() {
         return CODEC;
      }
   }

   record Weather(TestEnvironmentDefinition.Weather.Type weather) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.Weather> CODEC = RecordCodecBuilder.mapCodec(
         p_391509_ -> p_391509_.group(TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(TestEnvironmentDefinition.Weather::weather))
            .apply(p_391509_, TestEnvironmentDefinition.Weather::new)
      );

      @Override
      public void setup(ServerLevel p_395085_) {
         this.weather.apply(p_395085_);
      }

      @Override
      public void teardown(ServerLevel p_396783_) {
         p_396783_.resetWeatherCycle();
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

         Type(final String p_392127_, final int p_394718_, final int p_396701_, final boolean p_397651_, final boolean p_396720_) {
            this.id = p_392127_;
            this.clearTime = p_394718_;
            this.rainTime = p_396701_;
            this.raining = p_397651_;
            this.thundering = p_396720_;
         }

         void apply(ServerLevel p_396727_) {
            p_396727_.setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
         }

         @Override
         public String getSerializedName() {
            return this.id;
         }
      }
   }
}
