package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplate {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String PALETTE_TAG = "palette";
   public static final String PALETTE_LIST_TAG = "palettes";
   public static final String ENTITIES_TAG = "entities";
   public static final String BLOCKS_TAG = "blocks";
   public static final String BLOCK_TAG_POS = "pos";
   public static final String BLOCK_TAG_STATE = "state";
   public static final String BLOCK_TAG_NBT = "nbt";
   public static final String ENTITY_TAG_POS = "pos";
   public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
   public static final String ENTITY_TAG_NBT = "nbt";
   public static final String SIZE_TAG = "size";
   private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
   private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
   private Vec3i size = Vec3i.ZERO;
   private String author = "?";

   public Vec3i getSize() {
      return this.size;
   }

   public void setAuthor(String p_74613_) {
      this.author = p_74613_;
   }

   public String getAuthor() {
      return this.author;
   }

   public void fillFromWorld(Level p_163803_, BlockPos p_163804_, Vec3i p_163805_, boolean p_163806_, List<Block> p_410212_) {
      if (p_163805_.getX() >= 1 && p_163805_.getY() >= 1 && p_163805_.getZ() >= 1) {
         BlockPos blockpos = p_163804_.offset(p_163805_).offset(-1, -1, -1);
         List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
         BlockPos blockpos1 = new BlockPos(
            Math.min(p_163804_.getX(), blockpos.getX()), Math.min(p_163804_.getY(), blockpos.getY()), Math.min(p_163804_.getZ(), blockpos.getZ())
         );
         BlockPos blockpos2 = new BlockPos(
            Math.max(p_163804_.getX(), blockpos.getX()), Math.max(p_163804_.getY(), blockpos.getY()), Math.max(p_163804_.getZ(), blockpos.getZ())
         );
         this.size = p_163805_;

         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos1, blockpos2)) {
               BlockPos blockpos4 = blockpos3.subtract(blockpos1);
               BlockState blockstate = p_163803_.getBlockState(blockpos3);
               if (!p_410212_.stream().anyMatch(blockstate::is)) {
                  BlockEntity blockentity = p_163803_.getBlockEntity(blockpos3);
                  StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo;
                  if (blockentity != null) {
                     TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_163803_.registryAccess());
                     blockentity.saveWithId(tagvalueoutput);
                     structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, tagvalueoutput.buildResult());
                  } else {
                     structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, null);
                  }

                  addToLists(structuretemplate$structureblockinfo, list, list1, list2);
               }
            }

            List<StructureTemplate.StructureBlockInfo> list3 = buildInfoList(list, list1, list2);
            this.palettes.clear();
            this.palettes.add(new StructureTemplate.Palette(list3));
            if (p_163806_) {
               this.fillEntityList(p_163803_, blockpos1, blockpos2, problemreporter$scopedcollector);
            } else {
               this.entityInfoList.clear();
            }
         }
      }
   }

   private static void addToLists(
      StructureTemplate.StructureBlockInfo p_74574_,
      List<StructureTemplate.StructureBlockInfo> p_74575_,
      List<StructureTemplate.StructureBlockInfo> p_74576_,
      List<StructureTemplate.StructureBlockInfo> p_74577_
   ) {
      if (p_74574_.nbt != null) {
         p_74576_.add(p_74574_);
      } else if (!p_74574_.state.getBlock().hasDynamicShape() && p_74574_.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
         p_74575_.add(p_74574_);
      } else {
         p_74577_.add(p_74574_);
      }
   }

   private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
      List<StructureTemplate.StructureBlockInfo> p_74615_,
      List<StructureTemplate.StructureBlockInfo> p_74616_,
      List<StructureTemplate.StructureBlockInfo> p_74617_
   ) {
      Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
            p_74641_ -> p_74641_.pos.getY()
         )
         .thenComparingInt(p_74637_ -> p_74637_.pos.getX())
         .thenComparingInt(p_74572_ -> p_74572_.pos.getZ());
      p_74615_.sort(comparator);
      p_74617_.sort(comparator);
      p_74616_.sort(comparator);
      List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
      list.addAll(p_74615_);
      list.addAll(p_74617_);
      list.addAll(p_74616_);
      return list;
   }

   private void fillEntityList(Level p_74501_, BlockPos p_74502_, BlockPos p_74503_, ProblemReporter p_410106_) {
      List<Entity> list = p_74501_.getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(p_74502_, p_74503_), p_74499_ -> !(p_74499_ instanceof Player));
      this.entityInfoList.clear();

      for (Entity entity : list) {
         Vec3 vec3 = new Vec3(entity.getX() - p_74502_.getX(), entity.getY() - p_74502_.getY(), entity.getZ() - p_74502_.getZ());
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(p_410106_.forChild(entity.problemPath()), entity.registryAccess());
         entity.save(tagvalueoutput);
         BlockPos blockpos;
         if (entity instanceof Painting painting) {
            blockpos = painting.getPos().subtract(p_74502_);
         } else {
            blockpos = BlockPos.containing(vec3);
         }

         this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockpos, tagvalueoutput.buildResult().copy()));
      }
   }

   public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos p_74604_, StructurePlaceSettings p_74605_, Block p_74606_) {
      return this.filterBlocks(p_74604_, p_74605_, p_74606_, true);
   }

   public List<StructureTemplate.JigsawBlockInfo> getJigsaws(BlockPos p_361797_, Rotation p_365954_) {
      if (this.palettes.isEmpty()) {
         return new ArrayList<>();
      }

      StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setRotation(p_365954_);
      List<StructureTemplate.JigsawBlockInfo> list = structureplacesettings.getRandomPalette(this.palettes, p_361797_).jigsaws();
      List<StructureTemplate.JigsawBlockInfo> list1 = new ArrayList<>(list.size());

      for (StructureTemplate.JigsawBlockInfo structuretemplate$jigsawblockinfo : list) {
         StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = structuretemplate$jigsawblockinfo.info;
         list1.add(
            structuretemplate$jigsawblockinfo.withInfo(
               new StructureTemplate.StructureBlockInfo(
                  calculateRelativePosition(structureplacesettings, structuretemplate$structureblockinfo.pos()).offset(p_361797_),
                  structuretemplate$structureblockinfo.state.rotate(structureplacesettings.getRotation()),
                  structuretemplate$structureblockinfo.nbt
               )
            )
         );
      }

      return list1;
   }

   public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(
      BlockPos p_230336_, StructurePlaceSettings p_230337_, Block p_230338_, boolean p_230339_
   ) {
      ObjectArrayList<StructureTemplate.StructureBlockInfo> objectarraylist = new ObjectArrayList();
      BoundingBox boundingbox = p_230337_.getBoundingBox();
      if (this.palettes.isEmpty()) {
         return objectarraylist;
      }

      for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : p_230337_.getRandomPalette(this.palettes, p_230336_).blocks(p_230338_)) {
         BlockPos blockpos = p_230339_
            ? calculateRelativePosition(p_230337_, structuretemplate$structureblockinfo.pos).offset(p_230336_)
            : structuretemplate$structureblockinfo.pos;
         if (boundingbox == null || boundingbox.isInside(blockpos)) {
            objectarraylist.add(
               new StructureTemplate.StructureBlockInfo(
                  blockpos, structuretemplate$structureblockinfo.state.rotate(p_230337_.getRotation()), structuretemplate$structureblockinfo.nbt
               )
            );
         }
      }

      return objectarraylist;
   }

   public BlockPos calculateConnectedPosition(StructurePlaceSettings p_74567_, BlockPos p_74568_, StructurePlaceSettings p_74569_, BlockPos p_74570_) {
      BlockPos blockpos = calculateRelativePosition(p_74567_, p_74568_);
      BlockPos blockpos1 = calculateRelativePosition(p_74569_, p_74570_);
      return blockpos.subtract(blockpos1);
   }

   public static BlockPos calculateRelativePosition(StructurePlaceSettings p_74564_, BlockPos p_74565_) {
      return transform(p_74565_, p_74564_.getMirror(), p_74564_.getRotation(), p_74564_.getRotationPivot());
   }

   public boolean placeInWorld(
      ServerLevelAccessor p_230329_,
      BlockPos p_230330_,
      BlockPos p_230331_,
      StructurePlaceSettings p_230332_,
      RandomSource p_230333_,
      @Block.UpdateFlags int p_230334_
   ) {
      if (this.palettes.isEmpty()) {
         return false;
      }

      List<StructureTemplate.StructureBlockInfo> list = p_230332_.getRandomPalette(this.palettes, p_230330_).blocks();
      if ((!list.isEmpty() || !p_230332_.isIgnoreEntities() && !this.entityInfoList.isEmpty())
         && this.size.getX() >= 1
         && this.size.getY() >= 1
         && this.size.getZ() >= 1) {
         BoundingBox boundingbox = p_230332_.getBoundingBox();
         List<BlockPos> list1 = Lists.newArrayListWithCapacity(p_230332_.shouldApplyWaterlogging() ? list.size() : 0);
         List<BlockPos> list2 = Lists.newArrayListWithCapacity(p_230332_.shouldApplyWaterlogging() ? list.size() : 0);
         List<Pair<BlockPos, CompoundTag>> list3 = Lists.newArrayListWithCapacity(list.size());
         int i = Integer.MAX_VALUE;
         int j = Integer.MAX_VALUE;
         int k = Integer.MAX_VALUE;
         int l = Integer.MIN_VALUE;
         int i1 = Integer.MIN_VALUE;
         int j1 = Integer.MIN_VALUE;
         List<StructureTemplate.StructureBlockInfo> list4 = processBlockInfos(p_230329_, p_230330_, p_230331_, p_230332_, list);

         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : list4) {
               BlockPos blockpos = structuretemplate$structureblockinfo.pos;
               if (boundingbox == null || boundingbox.isInside(blockpos)) {
                  FluidState fluidstate = p_230332_.shouldApplyWaterlogging() ? p_230329_.getFluidState(blockpos) : null;
                  BlockState blockstate = structuretemplate$structureblockinfo.state.mirror(p_230332_.getMirror()).rotate(p_230332_.getRotation());
                  if (structuretemplate$structureblockinfo.nbt != null) {
                     p_230329_.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 820);
                  }

                  if (p_230329_.setBlock(blockpos, blockstate, p_230334_)) {
                     i = Math.min(i, blockpos.getX());
                     j = Math.min(j, blockpos.getY());
                     k = Math.min(k, blockpos.getZ());
                     l = Math.max(l, blockpos.getX());
                     i1 = Math.max(i1, blockpos.getY());
                     j1 = Math.max(j1, blockpos.getZ());
                     list3.add(Pair.of(blockpos, structuretemplate$structureblockinfo.nbt));
                     if (structuretemplate$structureblockinfo.nbt != null) {
                        BlockEntity blockentity = p_230329_.getBlockEntity(blockpos);
                        if (blockentity != null) {
                           if (!SharedConstants.DEBUG_STRUCTURE_EDIT_MODE && blockentity instanceof RandomizableContainer) {
                              structuretemplate$structureblockinfo.nbt.putLong("LootTableSeed", p_230333_.nextLong());
                           }

                           blockentity.loadWithComponents(
                              TagValueInput.create(
                                 problemreporter$scopedcollector.forChild(blockentity.problemPath()),
                                 p_230329_.registryAccess(),
                                 structuretemplate$structureblockinfo.nbt
                              )
                           );
                        }
                     }

                     if (fluidstate != null) {
                        if (blockstate.getFluidState().isSource()) {
                           list2.add(blockpos);
                        } else if (blockstate.getBlock() instanceof LiquidBlockContainer) {
                           ((LiquidBlockContainer)blockstate.getBlock()).placeLiquid(p_230329_, blockpos, blockstate, fluidstate);
                           if (!fluidstate.isSource()) {
                              list1.add(blockpos);
                           }
                        }
                     }
                  }
               }
            }

            boolean flag = true;
            Direction[] adirection = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

            while (flag && !list1.isEmpty()) {
               flag = false;
               Iterator<BlockPos> iterator = list1.iterator();

               while (iterator.hasNext()) {
                  BlockPos blockpos3 = iterator.next();
                  FluidState fluidstate2 = p_230329_.getFluidState(blockpos3);

                  for (int i2 = 0; i2 < adirection.length && !fluidstate2.isSource(); i2++) {
                     BlockPos blockpos1 = blockpos3.relative(adirection[i2]);
                     FluidState fluidstate1 = p_230329_.getFluidState(blockpos1);
                     if (fluidstate1.isSource() && !list2.contains(blockpos1)) {
                        fluidstate2 = fluidstate1;
                     }
                  }

                  if (fluidstate2.isSource()) {
                     BlockState blockstate1 = p_230329_.getBlockState(blockpos3);
                     Block block = blockstate1.getBlock();
                     if (block instanceof LiquidBlockContainer) {
                        ((LiquidBlockContainer)block).placeLiquid(p_230329_, blockpos3, blockstate1, fluidstate2);
                        flag = true;
                        iterator.remove();
                     }
                  }
               }
            }

            if (i <= l) {
               if (!p_230332_.getKnownShape()) {
                  DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
                  int k1 = i;
                  int l1 = j;
                  int j2 = k;

                  for (Pair<BlockPos, CompoundTag> pair1 : list3) {
                     BlockPos blockpos2 = (BlockPos)pair1.getFirst();
                     discretevoxelshape.fill(blockpos2.getX() - k1, blockpos2.getY() - l1, blockpos2.getZ() - j2);
                  }

                  updateShapeAtEdge(p_230329_, p_230334_, discretevoxelshape, k1, l1, j2);
               }

               for (Pair<BlockPos, CompoundTag> pair : list3) {
                  BlockPos blockpos4 = (BlockPos)pair.getFirst();
                  if (!p_230332_.getKnownShape()) {
                     BlockState blockstate2 = p_230329_.getBlockState(blockpos4);
                     BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate2, p_230329_, blockpos4);
                     if (blockstate2 != blockstate3) {
                        p_230329_.setBlock(blockpos4, blockstate3, p_230334_ & -2 | 16);
                     }

                     p_230329_.updateNeighborsAt(blockpos4, blockstate3.getBlock());
                  }

                  if (pair.getSecond() != null) {
                     BlockEntity blockentity1 = p_230329_.getBlockEntity(blockpos4);
                     if (blockentity1 != null) {
                        blockentity1.setChanged();
                     }
                  }
               }
            }

            if (!p_230332_.isIgnoreEntities()) {
               this.placeEntities(
                  p_230329_,
                  p_230330_,
                  p_230332_.getMirror(),
                  p_230332_.getRotation(),
                  p_230332_.getRotationPivot(),
                  boundingbox,
                  p_230332_.shouldFinalizeEntities(),
                  problemreporter$scopedcollector
               );
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static void updateShapeAtEdge(LevelAccessor p_331910_, @Block.UpdateFlags int p_330850_, DiscreteVoxelShape p_333161_, BlockPos p_335658_) {
      updateShapeAtEdge(p_331910_, p_330850_, p_333161_, p_335658_.getX(), p_335658_.getY(), p_335658_.getZ());
   }

   public static void updateShapeAtEdge(
      LevelAccessor p_74511_, @Block.UpdateFlags int p_74512_, DiscreteVoxelShape p_74513_, int p_74514_, int p_74515_, int p_74516_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
      p_74513_.forAllFaces(
         (p_360634_, p_360635_, p_360636_, p_360637_) -> {
            blockpos$mutableblockpos.set(p_74514_ + p_360635_, p_74515_ + p_360636_, p_74516_ + p_360637_);
            blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, p_360634_);
            BlockState blockstate = p_74511_.getBlockState(blockpos$mutableblockpos);
            BlockState blockstate1 = p_74511_.getBlockState(blockpos$mutableblockpos1);
            BlockState blockstate2 = blockstate.updateShape(
               p_74511_, p_74511_, blockpos$mutableblockpos, p_360634_, blockpos$mutableblockpos1, blockstate1, p_74511_.getRandom()
            );
            if (blockstate != blockstate2) {
               p_74511_.setBlock(blockpos$mutableblockpos, blockstate2, p_74512_ & -2);
            }

            BlockState blockstate3 = blockstate1.updateShape(
               p_74511_, p_74511_, blockpos$mutableblockpos1, p_360634_.getOpposite(), blockpos$mutableblockpos, blockstate2, p_74511_.getRandom()
            );
            if (blockstate1 != blockstate3) {
               p_74511_.setBlock(blockpos$mutableblockpos1, blockstate3, p_74512_ & -2);
            }
         }
      );
   }

   public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
      ServerLevelAccessor p_278297_, BlockPos p_74519_, BlockPos p_74520_, StructurePlaceSettings p_74521_, List<StructureTemplate.StructureBlockInfo> p_74522_
   ) {
      List<StructureTemplate.StructureBlockInfo> list = new ArrayList<>();
      List<StructureTemplate.StructureBlockInfo> list1 = new ArrayList<>();

      for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : p_74522_) {
         BlockPos blockpos = calculateRelativePosition(p_74521_, structuretemplate$structureblockinfo.pos).offset(p_74519_);
         StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 = new StructureTemplate.StructureBlockInfo(
            blockpos,
            structuretemplate$structureblockinfo.state,
            structuretemplate$structureblockinfo.nbt != null ? structuretemplate$structureblockinfo.nbt.copy() : null
         );
         Iterator<StructureProcessor> iterator = p_74521_.getProcessors().iterator();

         while (structuretemplate$structureblockinfo1 != null && iterator.hasNext()) {
            structuretemplate$structureblockinfo1 = iterator.next()
               .processBlock(p_278297_, p_74519_, p_74520_, structuretemplate$structureblockinfo, structuretemplate$structureblockinfo1, p_74521_);
         }

         if (structuretemplate$structureblockinfo1 != null) {
            list1.add(structuretemplate$structureblockinfo1);
            list.add(structuretemplate$structureblockinfo);
         }
      }

      for (StructureProcessor structureprocessor : p_74521_.getProcessors()) {
         list1 = structureprocessor.finalizeProcessing(p_278297_, p_74519_, p_74520_, list, list1, p_74521_);
      }

      return list1;
   }

   private void placeEntities(
      ServerLevelAccessor p_74524_,
      BlockPos p_74525_,
      Mirror p_74526_,
      Rotation p_74527_,
      BlockPos p_74528_,
      @Nullable BoundingBox p_74529_,
      boolean p_74530_,
      ProblemReporter p_408312_
   ) {
      for (StructureTemplate.StructureEntityInfo structuretemplate$structureentityinfo : this.entityInfoList) {
         BlockPos blockpos = transform(structuretemplate$structureentityinfo.blockPos, p_74526_, p_74527_, p_74528_).offset(p_74525_);
         if (p_74529_ == null || p_74529_.isInside(blockpos)) {
            CompoundTag compoundtag = structuretemplate$structureentityinfo.nbt.copy();
            Vec3 vec3 = transform(structuretemplate$structureentityinfo.pos, p_74526_, p_74527_, p_74528_);
            Vec3 vec31 = vec3.add(p_74525_.getX(), p_74525_.getY(), p_74525_.getZ());
            ListTag listtag = new ListTag();
            listtag.add(DoubleTag.valueOf(vec31.x));
            listtag.add(DoubleTag.valueOf(vec31.y));
            listtag.add(DoubleTag.valueOf(vec31.z));
            compoundtag.put("Pos", listtag);
            compoundtag.remove("UUID");
            createEntityIgnoreException(p_408312_, p_74524_, compoundtag).ifPresent(p_422248_ -> {
               float f = p_422248_.rotate(p_74527_);
               f += p_422248_.mirror(p_74526_) - p_422248_.getYRot();
               p_422248_.snapTo(vec31.x, vec31.y, vec31.z, f, p_422248_.getXRot());
               p_422248_.setYBodyRot(f);
               p_422248_.setYHeadRot(f);
               if (p_74530_ && p_422248_ instanceof Mob mob) {
                  mob.finalizeSpawn(p_74524_, p_74524_.getCurrentDifficultyAt(BlockPos.containing(vec31)), EntitySpawnReason.STRUCTURE, null);
               }

               p_74524_.addFreshEntityWithPassengers(p_422248_);
            });
         }
      }
   }

   private static Optional<Entity> createEntityIgnoreException(ProblemReporter p_410558_, ServerLevelAccessor p_74544_, CompoundTag p_74545_) {
      try {
         return EntityType.create(TagValueInput.create(p_410558_, p_74544_.registryAccess(), p_74545_), p_74544_.getLevel(), EntitySpawnReason.STRUCTURE);
      } catch (Exception exception) {
         return Optional.empty();
      }
   }

   public Vec3i getSize(Rotation p_163809_) {
      switch (p_163809_) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
         default:
            return this.size;
      }
   }

   public static BlockPos transform(BlockPos p_74594_, Mirror p_74595_, Rotation p_74596_, BlockPos p_74597_) {
      int i = p_74594_.getX();
      int j = p_74594_.getY();
      int k = p_74594_.getZ();
      boolean flag = true;
      switch (p_74595_) {
         case LEFT_RIGHT:
            k = -k;
            break;
         case FRONT_BACK:
            i = -i;
            break;
         default:
            flag = false;
      }

      int l = p_74597_.getX();
      int i1 = p_74597_.getZ();
      switch (p_74596_) {
         case COUNTERCLOCKWISE_90:
            return new BlockPos(l - i1 + k, j, l + i1 - i);
         case CLOCKWISE_90:
            return new BlockPos(l + i1 - k, j, i1 - l + i);
         case CLOCKWISE_180:
            return new BlockPos(l + l - i, j, i1 + i1 - k);
         default:
            return flag ? new BlockPos(i, j, k) : p_74594_;
      }
   }

   public static Vec3 transform(Vec3 p_74579_, Mirror p_74580_, Rotation p_74581_, BlockPos p_74582_) {
      double d0 = p_74579_.x;
      double d1 = p_74579_.y;
      double d2 = p_74579_.z;
      boolean flag = true;
      switch (p_74580_) {
         case LEFT_RIGHT:
            d2 = 1.0 - d2;
            break;
         case FRONT_BACK:
            d0 = 1.0 - d0;
            break;
         default:
            flag = false;
      }

      int i = p_74582_.getX();
      int j = p_74582_.getZ();
      switch (p_74581_) {
         case COUNTERCLOCKWISE_90:
            return new Vec3(i - j + d2, d1, i + j + 1 - d0);
         case CLOCKWISE_90:
            return new Vec3(i + j + 1 - d2, d1, j - i + d0);
         case CLOCKWISE_180:
            return new Vec3(i + i + 1 - d0, d1, j + j + 1 - d2);
         default:
            return flag ? new Vec3(d0, d1, d2) : p_74579_;
      }
   }

   public BlockPos getZeroPositionWithTransform(BlockPos p_74584_, Mirror p_74585_, Rotation p_74586_) {
      return getZeroPositionWithTransform(p_74584_, p_74585_, p_74586_, this.getSize().getX(), this.getSize().getZ());
   }

   public static BlockPos getZeroPositionWithTransform(BlockPos p_74588_, Mirror p_74589_, Rotation p_74590_, int p_74591_, int p_74592_) {
      p_74591_--;
      p_74592_--;
      int i = p_74589_ == Mirror.FRONT_BACK ? p_74591_ : 0;
      int j = p_74589_ == Mirror.LEFT_RIGHT ? p_74592_ : 0;
      BlockPos blockpos = p_74588_;
      switch (p_74590_) {
         case COUNTERCLOCKWISE_90:
            blockpos = p_74588_.offset(j, 0, p_74591_ - i);
            break;
         case CLOCKWISE_90:
            blockpos = p_74588_.offset(p_74592_ - j, 0, i);
            break;
         case CLOCKWISE_180:
            blockpos = p_74588_.offset(p_74591_ - i, 0, p_74592_ - j);
            break;
         case NONE:
            blockpos = p_74588_.offset(i, 0, j);
      }

      return blockpos;
   }

   public BoundingBox getBoundingBox(StructurePlaceSettings p_74634_, BlockPos p_74635_) {
      return this.getBoundingBox(p_74635_, p_74634_.getRotation(), p_74634_.getRotationPivot(), p_74634_.getMirror());
   }

   public BoundingBox getBoundingBox(BlockPos p_74599_, Rotation p_74600_, BlockPos p_74601_, Mirror p_74602_) {
      return getBoundingBox(p_74599_, p_74600_, p_74601_, p_74602_, this.size);
   }

   @VisibleForTesting
   protected static BoundingBox getBoundingBox(BlockPos p_163811_, Rotation p_163812_, BlockPos p_163813_, Mirror p_163814_, Vec3i p_163815_) {
      Vec3i vec3i = p_163815_.offset(-1, -1, -1);
      BlockPos blockpos = transform(BlockPos.ZERO, p_163814_, p_163812_, p_163813_);
      BlockPos blockpos1 = transform(BlockPos.ZERO.offset(vec3i), p_163814_, p_163812_, p_163813_);
      return BoundingBox.fromCorners(blockpos, blockpos1).move(p_163811_);
   }

   public CompoundTag save(CompoundTag p_74619_) {
      if (this.palettes.isEmpty()) {
         p_74619_.put("blocks", new ListTag());
         p_74619_.put("palette", new ListTag());
      } else {
         List<StructureTemplate.SimplePalette> list = Lists.newArrayList();
         StructureTemplate.SimplePalette structuretemplate$simplepalette = new StructureTemplate.SimplePalette();
         list.add(structuretemplate$simplepalette);

         for (int i = 1; i < this.palettes.size(); i++) {
            list.add(new StructureTemplate.SimplePalette());
         }

         ListTag listtag1 = new ListTag();
         List<StructureTemplate.StructureBlockInfo> list1 = this.palettes.get(0).blocks();

         for (int j = 0; j < list1.size(); j++) {
            StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = list1.get(j);
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.put(
               "pos",
               this.newIntegerList(
                  structuretemplate$structureblockinfo.pos.getX(),
                  structuretemplate$structureblockinfo.pos.getY(),
                  structuretemplate$structureblockinfo.pos.getZ()
               )
            );
            int k = structuretemplate$simplepalette.idFor(structuretemplate$structureblockinfo.state);
            compoundtag.putInt("state", k);
            if (structuretemplate$structureblockinfo.nbt != null) {
               compoundtag.put("nbt", structuretemplate$structureblockinfo.nbt);
            }

            listtag1.add(compoundtag);

            for (int l = 1; l < this.palettes.size(); l++) {
               StructureTemplate.SimplePalette structuretemplate$simplepalette1 = list.get(l);
               structuretemplate$simplepalette1.addMapping(this.palettes.get(l).blocks().get(j).state, k);
            }
         }

         p_74619_.put("blocks", listtag1);
         if (list.size() == 1) {
            ListTag listtag2 = new ListTag();

            for (BlockState blockstate : structuretemplate$simplepalette) {
               listtag2.add(NbtUtils.writeBlockState(blockstate));
            }

            p_74619_.put("palette", listtag2);
         } else {
            ListTag listtag3 = new ListTag();

            for (StructureTemplate.SimplePalette structuretemplate$simplepalette2 : list) {
               ListTag listtag4 = new ListTag();

               for (BlockState blockstate1 : structuretemplate$simplepalette2) {
                  listtag4.add(NbtUtils.writeBlockState(blockstate1));
               }

               listtag3.add(listtag4);
            }

            p_74619_.put("palettes", listtag3);
         }
      }

      ListTag listtag = new ListTag();

      for (StructureTemplate.StructureEntityInfo structuretemplate$structureentityinfo : this.entityInfoList) {
         CompoundTag compoundtag1 = new CompoundTag();
         compoundtag1.put(
            "pos",
            this.newDoubleList(
               structuretemplate$structureentityinfo.pos.x, structuretemplate$structureentityinfo.pos.y, structuretemplate$structureentityinfo.pos.z
            )
         );
         compoundtag1.put(
            "blockPos",
            this.newIntegerList(
               structuretemplate$structureentityinfo.blockPos.getX(),
               structuretemplate$structureentityinfo.blockPos.getY(),
               structuretemplate$structureentityinfo.blockPos.getZ()
            )
         );
         if (structuretemplate$structureentityinfo.nbt != null) {
            compoundtag1.put("nbt", structuretemplate$structureentityinfo.nbt);
         }

         listtag.add(compoundtag1);
      }

      p_74619_.put("entities", listtag);
      p_74619_.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
      return NbtUtils.addCurrentDataVersion(p_74619_);
   }

   public void load(HolderGetter<Block> p_255773_, CompoundTag p_248574_) {
      this.palettes.clear();
      this.entityInfoList.clear();
      ListTag listtag = p_248574_.getListOrEmpty("size");
      this.size = new Vec3i(listtag.getIntOr(0, 0), listtag.getIntOr(1, 0), listtag.getIntOr(2, 0));
      ListTag listtag1 = p_248574_.getListOrEmpty("blocks");
      Optional<ListTag> optional = p_248574_.getList("palettes");
      if (optional.isPresent()) {
         for (int i = 0; i < optional.get().size(); i++) {
            this.loadPalette(p_255773_, optional.get().getListOrEmpty(i), listtag1);
         }
      } else {
         this.loadPalette(p_255773_, p_248574_.getListOrEmpty("palette"), listtag1);
      }

      p_248574_.getListOrEmpty("entities").compoundStream().forEach(p_391093_ -> {
         ListTag listtag2 = p_391093_.getListOrEmpty("pos");
         Vec3 vec3 = new Vec3(listtag2.getDoubleOr(0, 0.0), listtag2.getDoubleOr(1, 0.0), listtag2.getDoubleOr(2, 0.0));
         ListTag listtag3 = p_391093_.getListOrEmpty("blockPos");
         BlockPos blockpos = new BlockPos(listtag3.getIntOr(0, 0), listtag3.getIntOr(1, 0), listtag3.getIntOr(2, 0));
         p_391093_.getCompound("nbt").ifPresent(p_391086_ -> this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockpos, p_391086_)));
      });
   }

   private void loadPalette(HolderGetter<Block> p_256546_, ListTag p_251056_, ListTag p_251493_) {
      StructureTemplate.SimplePalette structuretemplate$simplepalette = new StructureTemplate.SimplePalette();

      for (int i = 0; i < p_251056_.size(); i++) {
         structuretemplate$simplepalette.addMapping(NbtUtils.readBlockState(p_256546_, p_251056_.getCompoundOrEmpty(i)), i);
      }

      List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();
      p_251493_.compoundStream()
         .forEach(
            p_391091_ -> {
               ListTag listtag = p_391091_.getListOrEmpty("pos");
               BlockPos blockpos = new BlockPos(listtag.getIntOr(0, 0), listtag.getIntOr(1, 0), listtag.getIntOr(2, 0));
               BlockState blockstate = structuretemplate$simplepalette.stateFor(p_391091_.getIntOr("state", 0));
               CompoundTag compoundtag = p_391091_.getCompound("nbt").orElse(null);
               StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(
                  blockpos, blockstate, compoundtag
               );
               addToLists(structuretemplate$structureblockinfo, list3, list, list1);
            }
         );
      List<StructureTemplate.StructureBlockInfo> list2 = buildInfoList(list3, list, list1);
      this.palettes.add(new StructureTemplate.Palette(list2));
   }

   private ListTag newIntegerList(int... p_74626_) {
      ListTag listtag = new ListTag();

      for (int i : p_74626_) {
         listtag.add(IntTag.valueOf(i));
      }

      return listtag;
   }

   private ListTag newDoubleList(double... p_74624_) {
      ListTag listtag = new ListTag();

      for (double d0 : p_74624_) {
         listtag.add(DoubleTag.valueOf(d0));
      }

      return listtag;
   }

   public static JigsawBlockEntity.JointType getJointType(CompoundTag p_368468_, BlockState p_365504_) {
      return p_368468_.<JigsawBlockEntity.JointType>read("joint", JigsawBlockEntity.JointType.CODEC).orElseGet(() -> getDefaultJointType(p_365504_));
   }

   public static JigsawBlockEntity.JointType getDefaultJointType(BlockState p_395106_) {
      return JigsawBlock.getFrontFacing(p_395106_).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
   }

   public record JigsawBlockInfo(
      StructureTemplate.StructureBlockInfo info,
      JigsawBlockEntity.JointType jointType,
      Identifier name,
      ResourceKey<StructureTemplatePool> pool,
      Identifier target,
      int placementPriority,
      int selectionPriority
   ) {
      public static StructureTemplate.JigsawBlockInfo of(StructureTemplate.StructureBlockInfo p_365601_) {
         CompoundTag compoundtag = Objects.requireNonNull(p_365601_.nbt(), () -> p_365601_ + " nbt was null");
         return new StructureTemplate.JigsawBlockInfo(
            p_365601_,
            StructureTemplate.getJointType(compoundtag, p_365601_.state()),
            compoundtag.<Identifier>read("name", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
            compoundtag.<ResourceKey<StructureTemplatePool>>read("pool", JigsawBlockEntity.POOL_CODEC).orElse(Pools.EMPTY),
            compoundtag.<Identifier>read("target", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
            compoundtag.getIntOr("placement_priority", 0),
            compoundtag.getIntOr("selection_priority", 0)
         );
      }

      @Override
      public String toString() {
         return String.format(
            Locale.ROOT,
            "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>",
            this.info.pos,
            this.info.state,
            this.name,
            this.pool.identifier(),
            this.target,
            this.placementPriority,
            this.selectionPriority,
            this.info.nbt
         );
      }

      public StructureTemplate.JigsawBlockInfo withInfo(StructureTemplate.StructureBlockInfo p_363148_) {
         return new StructureTemplate.JigsawBlockInfo(
            p_363148_, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority
         );
      }
   }

   public static final class Palette {
      private final List<StructureTemplate.StructureBlockInfo> blocks;
      private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();
      private @Nullable List<StructureTemplate.JigsawBlockInfo> cachedJigsaws;

      Palette(List<StructureTemplate.StructureBlockInfo> p_74648_) {
         this.blocks = p_74648_;
      }

      public List<StructureTemplate.JigsawBlockInfo> jigsaws() {
         if (this.cachedJigsaws == null) {
            this.cachedJigsaws = this.blocks(Blocks.JIGSAW).stream().map(StructureTemplate.JigsawBlockInfo::of).toList();
         }

         return this.cachedJigsaws;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks() {
         return this.blocks;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks(Block p_74654_) {
         return this.cache
            .computeIfAbsent(p_74654_, p_74659_ -> this.blocks.stream().filter(p_163818_ -> p_163818_.state.is(p_74659_)).collect(Collectors.toList()));
      }
   }

   static class SimplePalette implements Iterable<BlockState> {
      public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
      private final IdMapper<BlockState> ids = new IdMapper<>(16);
      private int lastId;

      public int idFor(BlockState p_74670_) {
         int i = this.ids.getId(p_74670_);
         if (i == -1) {
            i = this.lastId++;
            this.ids.addMapping(p_74670_, i);
         }

         return i;
      }

      public @Nullable BlockState stateFor(int p_74668_) {
         BlockState blockstate = this.ids.byId(p_74668_);
         return blockstate == null ? DEFAULT_BLOCK_STATE : blockstate;
      }

      @Override
      public Iterator<BlockState> iterator() {
         return this.ids.iterator();
      }

      public void addMapping(BlockState p_74672_, int p_74673_) {
         this.ids.addMapping(p_74672_, p_74673_);
      }
   }

   public record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
      @Override
      public String toString() {
         return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
      }
   }

   public static class StructureEntityInfo {
      public final Vec3 pos;
      public final BlockPos blockPos;
      public final CompoundTag nbt;

      public StructureEntityInfo(Vec3 p_74687_, BlockPos p_74688_, CompoundTag p_74689_) {
         this.pos = p_74687_;
         this.blockPos = p_74688_;
         this.nbt = p_74689_;
      }
   }
}
