package net.minecraft.gametest.framework;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;

public class FunctionGameTestInstance extends GameTestInstance {
   public static final MapCodec<FunctionGameTestInstance> CODEC = RecordCodecBuilder.mapCodec(
      p_397556_ -> p_397556_.group(
            ResourceKey.codec(Registries.TEST_FUNCTION).fieldOf("function").forGetter(FunctionGameTestInstance::function),
            TestData.CODEC.forGetter(GameTestInstance::info)
         )
         .apply(p_397556_, FunctionGameTestInstance::new)
   );
   private final ResourceKey<Consumer<GameTestHelper>> function;

   public FunctionGameTestInstance(ResourceKey<Consumer<GameTestHelper>> p_394332_, TestData<Holder<TestEnvironmentDefinition>> p_393413_) {
      super(p_393413_);
      this.function = p_394332_;
   }

   @Override
   public void run(GameTestHelper p_395038_) {
      p_395038_.getLevel()
         .registryAccess()
         .get(this.function)
         .map(Holder.Reference::value)
         .orElseThrow(() -> new IllegalStateException("Trying to access missing test function: " + this.function.identifier()))
         .accept(p_395038_);
   }

   private ResourceKey<Consumer<GameTestHelper>> function() {
      return this.function;
   }

   @Override
   public MapCodec<FunctionGameTestInstance> codec() {
      return CODEC;
   }

   @Override
   protected MutableComponent typeDescription() {
      return Component.translatable("test_instance.type.function");
   }

   @Override
   public Component describe() {
      return this.describeType()
         .append(this.descriptionRow("test_instance.description.function", this.function.identifier().toString()))
         .append(this.describeInfo());
   }
}
