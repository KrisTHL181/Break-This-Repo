package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public interface DispenseItemBehavior {
   Logger LOGGER = LogUtils.getLogger();
   DispenseItemBehavior NOOP = (p_123400_, p_123401_) -> p_123401_;

   ItemStack dispense(BlockSource var1, ItemStack var2);

   static void bootStrap() {
      DispenserBlock.registerProjectileBehavior(Items.ARROW);
      DispenserBlock.registerProjectileBehavior(Items.TIPPED_ARROW);
      DispenserBlock.registerProjectileBehavior(Items.SPECTRAL_ARROW);
      DispenserBlock.registerProjectileBehavior(Items.EGG);
      DispenserBlock.registerProjectileBehavior(Items.BLUE_EGG);
      DispenserBlock.registerProjectileBehavior(Items.BROWN_EGG);
      DispenserBlock.registerProjectileBehavior(Items.SNOWBALL);
      DispenserBlock.registerProjectileBehavior(Items.EXPERIENCE_BOTTLE);
      DispenserBlock.registerProjectileBehavior(Items.SPLASH_POTION);
      DispenserBlock.registerProjectileBehavior(Items.LINGERING_POTION);
      DispenserBlock.registerProjectileBehavior(Items.FIREWORK_ROCKET);
      DispenserBlock.registerProjectileBehavior(Items.FIRE_CHARGE);
      DispenserBlock.registerProjectileBehavior(Items.WIND_CHARGE);
      DefaultDispenseItemBehavior defaultdispenseitembehavior = new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_327707_, ItemStack p_329825_) {
            Direction direction = p_327707_.state().getValue(DispenserBlock.FACING);
            EntityType<?> entitytype = ((SpawnEggItem)p_329825_.getItem()).getType(p_329825_);
            if (entitytype == null) {
               return p_329825_;
            }

            try {
               entitytype.spawn(
                  p_327707_.level(), p_329825_, null, p_327707_.pos().relative(direction), EntitySpawnReason.DISPENSER, direction != Direction.UP, false
               );
            } catch (Exception exception) {
               LOGGER.error("Error while dispensing spawn egg from dispenser at {}", p_327707_.pos(), exception);
               return ItemStack.EMPTY;
            }

            p_329825_.shrink(1);
            p_327707_.level().gameEvent(null, GameEvent.ENTITY_PLACE, p_327707_.pos());
            return p_329825_;
         }
      };

      for (SpawnEggItem spawneggitem : SpawnEggItem.eggs()) {
         DispenserBlock.registerBehavior(spawneggitem, defaultdispenseitembehavior);
      }

      DispenserBlock.registerBehavior(
         Items.ARMOR_STAND,
         new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource p_334267_, ItemStack p_328475_) {
               Direction direction = p_334267_.state().getValue(DispenserBlock.FACING);
               BlockPos blockpos = p_334267_.pos().relative(direction);
               ServerLevel serverlevel = p_334267_.level();
               Consumer<ArmorStand> consumer = EntityType.appendDefaultStackConfig(
                  p_405049_ -> p_405049_.setYRot(direction.toYRot()), serverlevel, p_328475_, null
               );
               ArmorStand armorstand = EntityType.ARMOR_STAND.spawn(serverlevel, consumer, blockpos, EntitySpawnReason.DISPENSER, false, false);
               if (armorstand != null) {
                  p_328475_.shrink(1);
               }

               return p_328475_;
            }
         }
      );
      DispenserBlock.registerBehavior(
         Items.CHEST,
         new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource p_328289_, ItemStack p_334031_) {
               BlockPos blockpos = p_328289_.pos().relative(p_328289_.state().getValue(DispenserBlock.FACING));

               for (AbstractChestedHorse abstractchestedhorse : p_328289_.level()
                  .getEntitiesOfClass(AbstractChestedHorse.class, new AABB(blockpos), p_448619_ -> p_448619_.isAlive() && !p_448619_.hasChest())) {
                  if (abstractchestedhorse.isTamed()) {
                     SlotAccess slotaccess = abstractchestedhorse.getSlot(499);
                     if (slotaccess != null && slotaccess.set(p_334031_)) {
                        p_334031_.shrink(1);
                        this.setSuccess(true);
                        return p_334031_;
                     }
                  }
               }

               return super.execute(p_328289_, p_334031_);
            }
         }
      );
      DispenserBlock.registerBehavior(Items.OAK_BOAT, new BoatDispenseItemBehavior(EntityType.OAK_BOAT));
      DispenserBlock.registerBehavior(Items.SPRUCE_BOAT, new BoatDispenseItemBehavior(EntityType.SPRUCE_BOAT));
      DispenserBlock.registerBehavior(Items.BIRCH_BOAT, new BoatDispenseItemBehavior(EntityType.BIRCH_BOAT));
      DispenserBlock.registerBehavior(Items.JUNGLE_BOAT, new BoatDispenseItemBehavior(EntityType.JUNGLE_BOAT));
      DispenserBlock.registerBehavior(Items.DARK_OAK_BOAT, new BoatDispenseItemBehavior(EntityType.DARK_OAK_BOAT));
      DispenserBlock.registerBehavior(Items.ACACIA_BOAT, new BoatDispenseItemBehavior(EntityType.ACACIA_BOAT));
      DispenserBlock.registerBehavior(Items.CHERRY_BOAT, new BoatDispenseItemBehavior(EntityType.CHERRY_BOAT));
      DispenserBlock.registerBehavior(Items.MANGROVE_BOAT, new BoatDispenseItemBehavior(EntityType.MANGROVE_BOAT));
      DispenserBlock.registerBehavior(Items.PALE_OAK_BOAT, new BoatDispenseItemBehavior(EntityType.PALE_OAK_BOAT));
      DispenserBlock.registerBehavior(Items.BAMBOO_RAFT, new BoatDispenseItemBehavior(EntityType.BAMBOO_RAFT));
      DispenserBlock.registerBehavior(Items.OAK_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.OAK_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.SPRUCE_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.SPRUCE_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.BIRCH_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.BIRCH_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.JUNGLE_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.JUNGLE_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.DARK_OAK_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.DARK_OAK_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.ACACIA_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.ACACIA_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.CHERRY_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.CHERRY_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.MANGROVE_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.MANGROVE_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.PALE_OAK_CHEST_BOAT, new BoatDispenseItemBehavior(EntityType.PALE_OAK_CHEST_BOAT));
      DispenserBlock.registerBehavior(Items.BAMBOO_CHEST_RAFT, new BoatDispenseItemBehavior(EntityType.BAMBOO_CHEST_RAFT));
      DispenseItemBehavior dispenseitembehavior = new DefaultDispenseItemBehavior() {
         private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

         @Override
         public ItemStack execute(BlockSource p_334868_, ItemStack p_334276_) {
            DispensibleContainerItem dispensiblecontaineritem = (DispensibleContainerItem)p_334276_.getItem();
            BlockPos blockpos = p_334868_.pos().relative(p_334868_.state().getValue(DispenserBlock.FACING));
            Level level = p_334868_.level();
            if (dispensiblecontaineritem.emptyContents(null, level, blockpos, null)) {
               dispensiblecontaineritem.checkExtraContent(null, level, p_334276_, blockpos);
               return this.consumeWithRemainder(p_334868_, p_334276_, new ItemStack(Items.BUCKET));
            } else {
               return this.defaultDispenseItemBehavior.dispense(p_334868_, p_334276_);
            }
         }
      };
      DispenserBlock.registerBehavior(Items.LAVA_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.WATER_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.POWDER_SNOW_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.SALMON_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.COD_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.PUFFERFISH_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.TROPICAL_FISH_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.AXOLOTL_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.TADPOLE_BUCKET, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.BUCKET, new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_335842_, ItemStack p_335219_) {
            LevelAccessor levelaccessor = p_335842_.level();
            BlockPos blockpos = p_335842_.pos().relative(p_335842_.state().getValue(DispenserBlock.FACING));
            BlockState blockstate = levelaccessor.getBlockState(blockpos);
            if (blockstate.getBlock() instanceof BucketPickup bucketpickup) {
               ItemStack itemstack = bucketpickup.pickupBlock(null, levelaccessor, blockpos, blockstate);
               if (itemstack.isEmpty()) {
                  return super.execute(p_335842_, p_335219_);
               }

               levelaccessor.gameEvent(null, GameEvent.FLUID_PICKUP, blockpos);
               Item item = itemstack.getItem();
               return this.consumeWithRemainder(p_335842_, p_335219_, new ItemStack(item));
            } else {
               return super.execute(p_335842_, p_335219_);
            }
         }
      });
      DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_333645_, ItemStack p_333855_) {
            ServerLevel serverlevel = p_333645_.level();
            this.setSuccess(true);
            Direction direction = p_333645_.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = p_333645_.pos().relative(direction);
            BlockState blockstate = serverlevel.getBlockState(blockpos);
            if (BaseFireBlock.canBePlacedAt(serverlevel, blockpos, direction)) {
               serverlevel.setBlockAndUpdate(blockpos, BaseFireBlock.getState(serverlevel, blockpos));
               serverlevel.gameEvent(null, GameEvent.BLOCK_PLACE, blockpos);
            } else if (CampfireBlock.canLight(blockstate) || CandleBlock.canLight(blockstate) || CandleCakeBlock.canLight(blockstate)) {
               serverlevel.setBlockAndUpdate(blockpos, blockstate.setValue(BlockStateProperties.LIT, true));
               serverlevel.gameEvent(null, GameEvent.BLOCK_CHANGE, blockpos);
            } else if (blockstate.getBlock() instanceof TntBlock) {
               if (TntBlock.prime(serverlevel, blockpos)) {
                  serverlevel.removeBlock(blockpos, false);
               } else {
                  this.setSuccess(false);
               }
            } else {
               this.setSuccess(false);
            }

            if (this.isSuccess()) {
               p_333855_.hurtAndBreak(1, serverlevel, null, p_394321_ -> {});
            }

            return p_333855_;
         }
      });
      DispenserBlock.registerBehavior(Items.BONE_MEAL, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_332842_, ItemStack p_335191_) {
            this.setSuccess(true);
            Level level = p_332842_.level();
            BlockPos blockpos = p_332842_.pos().relative(p_332842_.state().getValue(DispenserBlock.FACING));
            if (!BoneMealItem.growCrop(p_335191_, level, blockpos) && !BoneMealItem.growWaterPlant(p_335191_, level, blockpos, null)) {
               this.setSuccess(false);
            } else if (!level.isClientSide()) {
               level.levelEvent(1505, blockpos, 15);
            }

            return p_335191_;
         }
      });
      DispenserBlock.registerBehavior(Blocks.TNT, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_333039_, ItemStack p_335778_) {
            ServerLevel serverlevel = p_333039_.level();
            if (!serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
               this.setSuccess(false);
               return p_335778_;
            } else {
               BlockPos blockpos = p_333039_.pos().relative(p_333039_.state().getValue(DispenserBlock.FACING));
               PrimedTnt primedtnt = new PrimedTnt(serverlevel, blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5, null);
               serverlevel.addFreshEntity(primedtnt);
               serverlevel.playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
               serverlevel.gameEvent(null, GameEvent.ENTITY_PLACE, blockpos);
               p_335778_.shrink(1);
               this.setSuccess(true);
               return p_335778_;
            }
         }
      });
      DispenserBlock.registerBehavior(
         Items.WITHER_SKELETON_SKULL,
         new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource p_123523_, ItemStack p_123524_) {
               Level level = p_123523_.level();
               Direction direction = p_123523_.state().getValue(DispenserBlock.FACING);
               BlockPos blockpos = p_123523_.pos().relative(direction);
               if (level.isEmptyBlock(blockpos) && WitherSkullBlock.canSpawnMob(level, blockpos, p_123524_)) {
                  level.setBlock(
                     blockpos, Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, RotationSegment.convertToSegment(direction)), 3
                  );
                  level.gameEvent(null, GameEvent.BLOCK_PLACE, blockpos);
                  BlockEntity blockentity = level.getBlockEntity(blockpos);
                  if (blockentity instanceof SkullBlockEntity) {
                     WitherSkullBlock.checkSpawn(level, blockpos, (SkullBlockEntity)blockentity);
                  }

                  p_123524_.shrink(1);
                  this.setSuccess(true);
               } else {
                  this.setSuccess(EquipmentDispenseItemBehavior.dispenseEquipment(p_123523_, p_123524_));
               }

               return p_123524_;
            }
         }
      );
      DispenserBlock.registerBehavior(Blocks.CARVED_PUMPKIN, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_123461_, ItemStack p_123462_) {
            Level level = p_123461_.level();
            BlockPos blockpos = p_123461_.pos().relative(p_123461_.state().getValue(DispenserBlock.FACING));
            CarvedPumpkinBlock carvedpumpkinblock = (CarvedPumpkinBlock)Blocks.CARVED_PUMPKIN;
            if (level.isEmptyBlock(blockpos) && carvedpumpkinblock.canSpawnGolem(level, blockpos)) {
               if (!level.isClientSide()) {
                  level.setBlock(blockpos, carvedpumpkinblock.defaultBlockState(), 3);
                  level.gameEvent(null, GameEvent.BLOCK_PLACE, blockpos);
               }

               p_123462_.shrink(1);
               this.setSuccess(true);
            } else {
               this.setSuccess(EquipmentDispenseItemBehavior.dispenseEquipment(p_123461_, p_123462_));
            }

            return p_123462_;
         }
      });
      DispenserBlock.registerBehavior(Blocks.SHULKER_BOX.asItem(), new ShulkerBoxDispenseBehavior());

      for (DyeColor dyecolor : DyeColor.values()) {
         DispenserBlock.registerBehavior(ShulkerBoxBlock.getBlockByColor(dyecolor).asItem(), new ShulkerBoxDispenseBehavior());
      }

      DispenserBlock.registerBehavior(
         Items.GLASS_BOTTLE.asItem(),
         new OptionalDispenseItemBehavior() {
            private ItemStack takeLiquid(BlockSource p_391543_, ItemStack p_394314_, ItemStack p_394721_) {
               p_391543_.level().gameEvent(null, GameEvent.FLUID_PICKUP, p_391543_.pos());
               return this.consumeWithRemainder(p_391543_, p_394314_, p_394721_);
            }

            @Override
            public ItemStack execute(BlockSource p_123529_, ItemStack p_123530_) {
               this.setSuccess(false);
               ServerLevel serverlevel = p_123529_.level();
               BlockPos blockpos = p_123529_.pos().relative(p_123529_.state().getValue(DispenserBlock.FACING));
               BlockState blockstate = serverlevel.getBlockState(blockpos);
               if (blockstate.is(
                     BlockTags.BEEHIVES, p_392137_ -> p_392137_.hasProperty(BeehiveBlock.HONEY_LEVEL) && p_392137_.getBlock() instanceof BeehiveBlock
                  )
                  && blockstate.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                  ((BeehiveBlock)blockstate.getBlock())
                     .releaseBeesAndResetHoneyLevel(serverlevel, blockstate, blockpos, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                  this.setSuccess(true);
                  return this.takeLiquid(p_123529_, p_123530_, new ItemStack(Items.HONEY_BOTTLE));
               } else if (serverlevel.getFluidState(blockpos).is(FluidTags.WATER)) {
                  this.setSuccess(true);
                  return this.takeLiquid(p_123529_, p_123530_, PotionContents.createItemStack(Items.POTION, Potions.WATER));
               } else {
                  return super.execute(p_123529_, p_123530_);
               }
            }
         }
      );
      DispenserBlock.registerBehavior(Items.GLOWSTONE, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_123535_, ItemStack p_123536_) {
            Direction direction = p_123535_.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = p_123535_.pos().relative(direction);
            Level level = p_123535_.level();
            BlockState blockstate = level.getBlockState(blockpos);
            this.setSuccess(true);
            if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
               if (blockstate.getValue(RespawnAnchorBlock.CHARGE) != 4) {
                  RespawnAnchorBlock.charge(null, level, blockpos, blockstate);
                  p_123536_.shrink(1);
               } else {
                  this.setSuccess(false);
               }

               return p_123536_;
            } else {
               return super.execute(p_123535_, p_123536_);
            }
         }
      });
      DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenseItemBehavior());
      DispenserBlock.registerBehavior(Items.BRUSH.asItem(), new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_123541_, ItemStack p_123542_) {
            ServerLevel serverlevel = p_123541_.level();
            BlockPos blockpos = p_123541_.pos().relative(p_123541_.state().getValue(DispenserBlock.FACING));
            List<Armadillo> list = serverlevel.getEntitiesOfClass(Armadillo.class, new AABB(blockpos), EntitySelector.NO_SPECTATORS);
            if (list.isEmpty()) {
               this.setSuccess(false);
               return p_123542_;
            }

            for (Armadillo armadillo : list) {
               if (armadillo.brushOffScute(null, p_123542_)) {
                  p_123542_.hurtAndBreak(16, serverlevel, null, p_392701_ -> {});
                  return p_123542_;
               }
            }

            this.setSuccess(false);
            return p_123542_;
         }
      });
      DispenserBlock.registerBehavior(Items.HONEYCOMB, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_123547_, ItemStack p_123548_) {
            BlockPos blockpos = p_123547_.pos().relative(p_123547_.state().getValue(DispenserBlock.FACING));
            Level level = p_123547_.level();
            BlockState blockstate = level.getBlockState(blockpos);
            Optional<BlockState> optional = HoneycombItem.getWaxed(blockstate);
            if (optional.isPresent()) {
               level.setBlockAndUpdate(blockpos, optional.get());
               level.levelEvent(3003, blockpos, 0);
               p_123548_.shrink(1);
               this.setSuccess(true);
               return p_123548_;
            } else {
               return super.execute(p_123547_, p_123548_);
            }
         }
      });
      DispenserBlock.registerBehavior(
         Items.POTION,
         new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(BlockSource p_123556_, ItemStack p_123557_) {
               PotionContents potioncontents = p_123557_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
               if (!potioncontents.is(Potions.WATER)) {
                  return this.defaultDispenseItemBehavior.dispense(p_123556_, p_123557_);
               }

               ServerLevel serverlevel = p_123556_.level();
               BlockPos blockpos = p_123556_.pos();
               BlockPos blockpos1 = p_123556_.pos().relative(p_123556_.state().getValue(DispenserBlock.FACING));
               if (!serverlevel.getBlockState(blockpos1).is(BlockTags.CONVERTABLE_TO_MUD)) {
                  return this.defaultDispenseItemBehavior.dispense(p_123556_, p_123557_);
               }

               if (!serverlevel.isClientSide()) {
                  for (int i = 0; i < 5; i++) {
                     serverlevel.sendParticles(
                        ParticleTypes.SPLASH,
                        blockpos.getX() + serverlevel.random.nextDouble(),
                        blockpos.getY() + 1,
                        blockpos.getZ() + serverlevel.random.nextDouble(),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        1.0
                     );
                  }
               }

               serverlevel.playSound(null, blockpos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               serverlevel.gameEvent(null, GameEvent.FLUID_PLACE, blockpos);
               serverlevel.setBlockAndUpdate(blockpos1, Blocks.MUD.defaultBlockState());
               return this.consumeWithRemainder(p_123556_, p_123557_, new ItemStack(Items.GLASS_BOTTLE));
            }
         }
      );
      DispenserBlock.registerBehavior(Items.MINECART, new MinecartDispenseItemBehavior(EntityType.MINECART));
      DispenserBlock.registerBehavior(Items.CHEST_MINECART, new MinecartDispenseItemBehavior(EntityType.CHEST_MINECART));
      DispenserBlock.registerBehavior(Items.FURNACE_MINECART, new MinecartDispenseItemBehavior(EntityType.FURNACE_MINECART));
      DispenserBlock.registerBehavior(Items.TNT_MINECART, new MinecartDispenseItemBehavior(EntityType.TNT_MINECART));
      DispenserBlock.registerBehavior(Items.HOPPER_MINECART, new MinecartDispenseItemBehavior(EntityType.HOPPER_MINECART));
      DispenserBlock.registerBehavior(Items.COMMAND_BLOCK_MINECART, new MinecartDispenseItemBehavior(EntityType.COMMAND_BLOCK_MINECART));
   }
}
