package net.minecraft.client.renderer.block.model.multipart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Selector(Optional<Condition> condition, BlockStateModel.Unbaked variant) {
   public static final Codec<Selector> CODEC = RecordCodecBuilder.create(
      p_397009_ -> p_397009_.group(
            Condition.CODEC.optionalFieldOf("when").forGetter(Selector::condition), BlockStateModel.Unbaked.CODEC.fieldOf("apply").forGetter(Selector::variant)
         )
         .apply(p_397009_, Selector::new)
   );

   public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> p_391682_) {
      return this.condition.<Predicate<S>>map(p_393307_ -> p_393307_.instantiate(p_391682_)).orElse(p_392027_ -> true);
   }
}
