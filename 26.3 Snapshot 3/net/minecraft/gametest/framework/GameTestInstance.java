package net.minecraft.gametest.framework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;

public abstract class GameTestInstance {
   public static final Codec<GameTestInstance> DIRECT_CODEC = BuiltInRegistries.TEST_INSTANCE_TYPE.byNameCodec().dispatch(GameTestInstance::codec, i -> i);
   private final TestData<Holder<TestEnvironmentDefinition<?>>> info;

   public static MapCodec<? extends GameTestInstance> bootstrap(final Registry<MapCodec<? extends GameTestInstance>> registry) {
      register(registry, "block_based", BlockBasedTestInstance.CODEC);
      return register(registry, "function", FunctionGameTestInstance.CODEC);
   }

   private static MapCodec<? extends GameTestInstance> register(
      final Registry<MapCodec<? extends GameTestInstance>> registry, final String name, final MapCodec<? extends GameTestInstance> codec
   ) {
      return Registry.register(registry, ResourceKey.create(Registries.TEST_INSTANCE_TYPE, Identifier.withDefaultNamespace(name)), codec);
   }

   protected GameTestInstance(final TestData<Holder<TestEnvironmentDefinition<?>>> info) {
      this.info = info;
   }

   public abstract void run(GameTestHelper helper);

   public abstract MapCodec<? extends GameTestInstance> codec();

   public Holder<TestEnvironmentDefinition<?>> batch() {
      return this.info.environment();
   }

   public Identifier structure() {
      return this.info.structure();
   }

   public int maxTicks() {
      return this.info.maxTicks();
   }

   public int setupTicks() {
      return this.info.setupTicks();
   }

   public boolean required() {
      return this.info.required();
   }

   public boolean manualOnly() {
      return this.info.manualOnly();
   }

   public int maxAttempts() {
      return this.info.maxAttempts();
   }

   public int requiredSuccesses() {
      return this.info.requiredSuccesses();
   }

   public boolean skyAccess() {
      return this.info.skyAccess();
   }

   public Rotation rotation() {
      return this.info.rotation();
   }

   public int padding() {
      return this.info.padding();
   }

   protected TestData<Holder<TestEnvironmentDefinition<?>>> info() {
      return this.info;
   }

   protected abstract MutableComponent typeDescription();

   public Component describe() {
      return this.describeType().append(this.describeInfo());
   }

   protected MutableComponent describeType() {
      return this.descriptionRow("test_instance.description.type", this.typeDescription());
   }

   protected Component describeInfo() {
      return this.descriptionRow("test_instance.description.structure", this.info.structure().toString())
         .append(this.descriptionRow("test_instance.description.batch", this.info.environment().getRegisteredName()));
   }

   protected MutableComponent descriptionRow(final String translationKey, final String value) {
      return this.descriptionRow(translationKey, Component.literal(value));
   }

   protected MutableComponent descriptionRow(final String translationKey, final MutableComponent value) {
      return Component.translatable(translationKey, value.withStyle(ChatFormatting.BLUE)).append(Component.literal("\n"));
   }
}
