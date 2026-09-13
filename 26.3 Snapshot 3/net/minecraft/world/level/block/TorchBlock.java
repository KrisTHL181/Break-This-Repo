package net.minecraft.world.level.block;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TorchBlock extends BaseTorchBlock {
   protected static final MapCodec<SimpleParticleType> PARTICLE_OPTIONS_FIELD = BuiltInRegistries.PARTICLE_TYPE
      .byNameCodec()
      .comapFlatMap(
         type -> type instanceof SimpleParticleType simple ? DataResult.success(simple) : DataResult.error(() -> "Not a SimpleParticleType: " + type),
         type -> type
      )
      .fieldOf("particle_options");
   protected final SimpleParticleType flameParticle;

   protected TorchBlock(final SimpleParticleType flameParticle, final BlockBehaviour.Properties properties) {
      super(properties);
      this.flameParticle = flameParticle;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      double x = pos.getX() + 0.5;
      double y = pos.getY() + 0.7;
      double z = pos.getZ() + 0.5;
      level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
      level.addParticle(this.flameParticle, x, y, z, 0.0, 0.0, 0.0);
   }
}
