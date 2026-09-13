package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gamerules.GameRules;

public class InfestedBlock extends Block {
   private final Block hostBlock;
   private static final Map<Block, Block> BLOCK_BY_HOST_BLOCK = Maps.newIdentityHashMap();
   private static final Map<BlockState, BlockState> HOST_TO_INFESTED_STATES = Maps.newIdentityHashMap();
   private static final Map<BlockState, BlockState> INFESTED_TO_HOST_STATES = Maps.newIdentityHashMap();

   public InfestedBlock(final Block hostBlock, final BlockBehaviour.Properties properties) {
      super(properties.destroyTime(hostBlock.defaultDestroyTime() / 2.0F).explosionResistance(0.75F));
      this.hostBlock = hostBlock;
      BLOCK_BY_HOST_BLOCK.put(hostBlock, this);
   }

   public Block getHostBlock() {
      return this.hostBlock;
   }

   public static boolean isCompatibleHostBlock(final BlockState blockState) {
      return BLOCK_BY_HOST_BLOCK.containsKey(blockState.getBlock());
   }

   private void spawnInfestation(final ServerLevel level, final BlockPos pos) {
      Silverfish silverfish = EntityTypes.SILVERFISH.create(level, EntitySpawnReason.TRIGGERED);
      if (silverfish != null) {
         silverfish.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
         level.addFreshEntity(silverfish);
         silverfish.spawnAnim();
      }
   }

   @Override
   protected void spawnAfterBreak(final BlockState state, final ServerLevel level, final BlockPos pos, final ItemStack tool, final boolean dropExperience) {
      super.spawnAfterBreak(state, level, pos, tool, dropExperience);
      if (level.getGameRules().get(GameRules.BLOCK_DROPS) && !EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
         this.spawnInfestation(level, pos);
      }
   }

   public static BlockState infestedStateByHost(final BlockState hostState) {
      return getNewStateWithProperties(HOST_TO_INFESTED_STATES, hostState, () -> BLOCK_BY_HOST_BLOCK.get(hostState.getBlock()).defaultBlockState());
   }

   public BlockState hostStateByInfested(final BlockState infestedState) {
      return getNewStateWithProperties(INFESTED_TO_HOST_STATES, infestedState, () -> this.getHostBlock().defaultBlockState());
   }

   private static BlockState getNewStateWithProperties(
      final Map<BlockState, BlockState> map, final BlockState oldState, final Supplier<BlockState> newStateSupplier
   ) {
      return map.computeIfAbsent(oldState, k -> {
         BlockState newState = newStateSupplier.get();

         for (Property<?> property : k.getProperties()) {
            newState = BlockBehaviour.BlockStateBase.copyProperty(k, newState, property);
         }

         return newState;
      });
   }
}
