package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Objects;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BrushableBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String LOOT_TABLE_TAG = "LootTable";
   private static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
   private static final String HIT_DIRECTION_TAG = "hit_direction";
   private static final String ITEM_TAG = "item";
   private static final int BRUSH_COOLDOWN_TICKS = 10;
   private static final int BRUSH_RESET_TICKS = 40;
   private static final int REQUIRED_BRUSHES_TO_BREAK = 10;
   private int brushCount;
   private long brushCountResetsAtTick;
   private long coolDownEndsAtTick;
   private ItemStack item = ItemStack.EMPTY;
   private @Nullable Direction hitDirection;
   private @Nullable ResourceKey<LootTable> lootTable;
   private long lootTableSeed;

   public BrushableBlockEntity(BlockPos p_277558_, BlockState p_278093_) {
      super(BlockEntityType.BRUSHABLE_BLOCK, p_277558_, p_278093_);
   }

   public boolean brush(long p_277786_, ServerLevel p_368993_, LivingEntity p_393398_, Direction p_277424_, ItemStack p_363346_) {
      if (this.hitDirection == null) {
         this.hitDirection = p_277424_;
      }

      this.brushCountResetsAtTick = p_277786_ + 40L;
      if (p_277786_ < this.coolDownEndsAtTick) {
         return false;
      }

      this.coolDownEndsAtTick = p_277786_ + 10L;
      this.unpackLootTable(p_368993_, p_393398_, p_363346_);
      int i = this.getCompletionState();
      if (++this.brushCount >= 10) {
         this.brushingCompleted(p_368993_, p_393398_, p_363346_);
         return true;
      }

      p_368993_.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
      int j = this.getCompletionState();
      if (i != j) {
         BlockState blockstate = this.getBlockState();
         BlockState blockstate1 = blockstate.setValue(BlockStateProperties.DUSTED, j);
         p_368993_.setBlock(this.getBlockPos(), blockstate1, 3);
      }

      return false;
   }

   private void unpackLootTable(ServerLevel p_367709_, LivingEntity p_392563_, ItemStack p_365744_) {
      if (this.lootTable != null) {
         LootTable loottable = p_367709_.getServer().reloadableRegistries().getLootTable(this.lootTable);
         if (p_392563_ instanceof ServerPlayer serverplayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverplayer, this.lootTable);
         }

         LootParams lootparams = new LootParams.Builder(p_367709_)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
            .withLuck(p_392563_.getLuck())
            .withParameter(LootContextParams.THIS_ENTITY, p_392563_)
            .withParameter(LootContextParams.TOOL, p_365744_)
            .create(LootContextParamSets.ARCHAEOLOGY);
         ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, this.lootTableSeed);

         this.item = switch (objectarraylist.size()) {
            case 0 -> ItemStack.EMPTY;
            case 1 -> (ItemStack)objectarraylist.getFirst();
            default -> {
               LOGGER.warn("Expected max 1 loot from loot table {}, but got {}", this.lootTable.identifier(), objectarraylist.size());
               yield (ItemStack)objectarraylist.getFirst();
            }
         };
         this.lootTable = null;
         this.setChanged();
      }
   }

   private void brushingCompleted(ServerLevel p_369249_, LivingEntity p_395246_, ItemStack p_364625_) {
      this.dropContent(p_369249_, p_395246_, p_364625_);
      BlockState blockstate = this.getBlockState();
      p_369249_.levelEvent(3008, this.getBlockPos(), Block.getId(blockstate));
      Block block;
      if (this.getBlockState().getBlock() instanceof BrushableBlock brushableblock) {
         block = brushableblock.getTurnsInto();
      } else {
         block = Blocks.AIR;
      }

      p_369249_.setBlock(this.worldPosition, block.defaultBlockState(), 3);
   }

   private void dropContent(ServerLevel p_360769_, LivingEntity p_391963_, ItemStack p_369575_) {
      this.unpackLootTable(p_360769_, p_391963_, p_369575_);
      if (!this.item.isEmpty()) {
         double d0 = EntityType.ITEM.getWidth();
         double d1 = 1.0 - d0;
         double d2 = d0 / 2.0;
         Direction direction = Objects.requireNonNullElse(this.hitDirection, Direction.UP);
         BlockPos blockpos = this.worldPosition.relative(direction, 1);
         double d3 = blockpos.getX() + 0.5 * d1 + d2;
         double d4 = blockpos.getY() + 0.5 + EntityType.ITEM.getHeight() / 2.0F;
         double d5 = blockpos.getZ() + 0.5 * d1 + d2;
         ItemEntity itementity = new ItemEntity(p_360769_, d3, d4, d5, this.item.split(p_360769_.random.nextInt(21) + 10));
         itementity.setDeltaMovement(Vec3.ZERO);
         p_360769_.addFreshEntity(itementity);
         this.item = ItemStack.EMPTY;
      }
   }

   public void checkReset(ServerLevel p_367879_) {
      if (this.brushCount != 0 && p_367879_.getGameTime() >= this.brushCountResetsAtTick) {
         int i = this.getCompletionState();
         this.brushCount = Math.max(0, this.brushCount - 2);
         int j = this.getCompletionState();
         if (i != j) {
            p_367879_.setBlock(this.getBlockPos(), this.getBlockState().setValue(BlockStateProperties.DUSTED, j), 3);
         }

         int k = 4;
         this.brushCountResetsAtTick = p_367879_.getGameTime() + 4L;
      }

      if (this.brushCount == 0) {
         this.hitDirection = null;
         this.brushCountResetsAtTick = 0L;
         this.coolDownEndsAtTick = 0L;
      } else {
         p_367879_.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
      }
   }

   private boolean tryLoadLootTable(ValueInput p_407555_) {
      this.lootTable = p_407555_.<ResourceKey<LootTable>>read("LootTable", LootTable.KEY_CODEC).orElse(null);
      this.lootTableSeed = p_407555_.getLongOr("LootTableSeed", 0L);
      return this.lootTable != null;
   }

   private boolean trySaveLootTable(ValueOutput p_407896_) {
      if (this.lootTable == null) {
         return false;
      }

      p_407896_.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
      if (this.lootTableSeed != 0L) {
         p_407896_.putLong("LootTableSeed", this.lootTableSeed);
      }

      return true;
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_329297_) {
      CompoundTag compoundtag = super.getUpdateTag(p_329297_);
      compoundtag.storeNullable("hit_direction", Direction.LEGACY_ID_CODEC, this.hitDirection);
      if (!this.item.isEmpty()) {
         RegistryOps<Tag> registryops = p_329297_.createSerializationContext(NbtOps.INSTANCE);
         compoundtag.store("item", ItemStack.CODEC, registryops, this.item);
      }

      return compoundtag;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   protected void loadAdditional(ValueInput p_410042_) {
      super.loadAdditional(p_410042_);
      if (!this.tryLoadLootTable(p_410042_)) {
         this.item = p_410042_.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      } else {
         this.item = ItemStack.EMPTY;
      }

      this.hitDirection = p_410042_.<Direction>read("hit_direction", Direction.LEGACY_ID_CODEC).orElse(null);
   }

   @Override
   protected void saveAdditional(ValueOutput p_407877_) {
      super.saveAdditional(p_407877_);
      if (!this.trySaveLootTable(p_407877_) && !this.item.isEmpty()) {
         p_407877_.store("item", ItemStack.CODEC, this.item);
      }
   }

   public void setLootTable(ResourceKey<LootTable> p_330093_, long p_277991_) {
      this.lootTable = p_330093_;
      this.lootTableSeed = p_277991_;
   }

   private int getCompletionState() {
      if (this.brushCount == 0) {
         return 0;
      } else if (this.brushCount < 3) {
         return 1;
      } else {
         return this.brushCount < 6 ? 2 : 3;
      }
   }

   public @Nullable Direction getHitDirection() {
      return this.hitDirection;
   }

   public ItemStack getItem() {
      return this.item;
   }
}
