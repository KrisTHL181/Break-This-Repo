package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {
   public static final int BIOME_CONTAINER_BITS = 2;
   private short nonEmptyBlockCount;
   private short fluidCount;
   private short tickingBlockCount;
   private short tickingFluidCount;
   private final PalettedContainer<BlockState> states;
   private PalettedContainerRO<Holder<Biome>> biomes;

   private LevelChunkSection(final LevelChunkSection source) {
      this.nonEmptyBlockCount = source.nonEmptyBlockCount;
      this.fluidCount = source.fluidCount;
      this.tickingBlockCount = source.tickingBlockCount;
      this.tickingFluidCount = source.tickingFluidCount;
      this.states = source.states.copy();
      this.biomes = source.biomes.copy();
   }

   public LevelChunkSection(final PalettedContainer<BlockState> states, final PalettedContainerRO<Holder<Biome>> biomes) {
      this.states = states;
      this.biomes = biomes;
      this.recalcBlockCounts();
   }

   public LevelChunkSection(final PalettedContainerFactory containerFactory) {
      this.states = containerFactory.createForBlockStates();
      this.biomes = containerFactory.createForBiomes();
   }

   public BlockState getBlockState(final int sectionX, final int sectionY, final int sectionZ) {
      return this.states.get(sectionX, sectionY, sectionZ);
   }

   public FluidState getFluidState(final int sectionX, final int sectionY, final int sectionZ) {
      return this.states.get(sectionX, sectionY, sectionZ).getFluidState();
   }

   public void acquire() {
      this.states.acquire();
   }

   public void release() {
      this.states.release();
   }

   public BlockState setBlockState(final int sectionX, final int sectionY, final int sectionZ, final BlockState state) {
      return this.setBlockState(sectionX, sectionY, sectionZ, state, true);
   }

   public BlockState setBlockState(final int sectionX, final int sectionY, final int sectionZ, final BlockState state, final boolean checkThreading) {
      BlockState previous;
      if (checkThreading) {
         previous = this.states.getAndSet(sectionX, sectionY, sectionZ, state);
      } else {
         previous = this.states.getAndSetUnchecked(sectionX, sectionY, sectionZ, state);
      }

      if (!previous.isAir()) {
         this.nonEmptyBlockCount--;
         if (previous.isRandomlyTicking()) {
            this.tickingBlockCount--;
         }

         FluidState previousFluid = previous.getFluidState();
         if (!previousFluid.isEmpty()) {
            this.fluidCount--;
            if (previousFluid.isRandomlyTicking()) {
               this.tickingFluidCount--;
            }
         }
      }

      if (!state.isAir()) {
         this.nonEmptyBlockCount++;
         if (state.isRandomlyTicking()) {
            this.tickingBlockCount++;
         }

         FluidState fluid = state.getFluidState();
         if (!fluid.isEmpty()) {
            this.fluidCount++;
            if (fluid.isRandomlyTicking()) {
               this.tickingFluidCount++;
            }
         }
      }

      return previous;
   }

   public boolean hasOnlyAir() {
      return this.nonEmptyBlockCount == 0;
   }

   public boolean hasFluid() {
      return this.fluidCount > 0;
   }

   public boolean isRandomlyTicking() {
      return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
   }

   public boolean isRandomlyTickingBlocks() {
      return this.tickingBlockCount > 0;
   }

   public boolean isRandomlyTickingFluids() {
      return this.tickingFluidCount > 0;
   }

   public void recalcBlockCounts() {
      class BlockCounter implements PalettedContainer.CountConsumer<BlockState> {
         public int nonEmptyBlockCount;
         public int fluidCount;
         public int tickingBlockCount;
         public int tickingFluidCount;

         public void accept(final BlockState state, final int count) {
            if (!state.isAir()) {
               this.nonEmptyBlockCount += count;
               if (state.isRandomlyTicking()) {
                  this.tickingBlockCount += count;
               }

               FluidState fluid = state.getFluidState();
               if (!fluid.isEmpty()) {
                  this.fluidCount += count;
                  if (fluid.isRandomlyTicking()) {
                     this.tickingFluidCount += count;
                  }
               }
            }
         }
      }

      BlockCounter blockCounter = new BlockCounter();
      this.states.count(blockCounter);
      this.nonEmptyBlockCount = (short)blockCounter.nonEmptyBlockCount;
      this.fluidCount = (short)blockCounter.fluidCount;
      this.tickingBlockCount = (short)blockCounter.tickingBlockCount;
      this.tickingFluidCount = (short)blockCounter.tickingFluidCount;
   }

   public PalettedContainer<BlockState> getStates() {
      return this.states;
   }

   public PalettedContainerRO<Holder<Biome>> getBiomes() {
      return this.biomes;
   }

   public void read(final FriendlyByteBuf buffer) {
      this.nonEmptyBlockCount = buffer.readShort();
      this.fluidCount = buffer.readShort();
      this.states.read(buffer);
      PalettedContainer<Holder<Biome>> biomes = this.biomes.recreate();
      biomes.read(buffer);
      this.biomes = biomes;
   }

   public void readBiomes(final FriendlyByteBuf buffer) {
      PalettedContainer<Holder<Biome>> biomes = this.biomes.recreate();
      biomes.read(buffer);
      this.biomes = biomes;
   }

   public void write(final FriendlyByteBuf buffer) {
      buffer.writeShort(this.nonEmptyBlockCount);
      buffer.writeShort(this.fluidCount);
      this.states.write(buffer);
      this.biomes.write(buffer);
   }

   public int getSerializedSize() {
      return 4 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
   }

   public boolean maybeHas(final Predicate<BlockState> predicate) {
      return this.states.maybeHas(predicate);
   }

   public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ) {
      return this.biomes.get(quartX, quartY, quartZ);
   }

   public void fillBiomesFromNoise(
      final BiomeResolver biomeResolver, final Climate.Sampler sampler, final int quartMinX, final int quartMinY, final int quartMinZ
   ) {
      PalettedContainer<Holder<Biome>> newBiomes = this.biomes.recreate();
      int size = 4;

      for (int x = 0; x < 4; x++) {
         for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
               newBiomes.getAndSetUnchecked(x, y, z, biomeResolver.getNoiseBiome(quartMinX + x, quartMinY + y, quartMinZ + z, sampler));
            }
         }
      }

      this.biomes = newBiomes;
   }

   public LevelChunkSection copy() {
      return new LevelChunkSection(this);
   }
}
