package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SequencedPriorityIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int UNSET_HEIGHT = Integer.MIN_VALUE;

   public static Optional<Structure.GenerationStub> addPieces(
      Structure.GenerationContext p_227239_,
      Holder<StructureTemplatePool> p_227240_,
      Optional<Identifier> p_227241_,
      int p_227242_,
      BlockPos p_227243_,
      boolean p_227244_,
      Optional<Heightmap.Types> p_227245_,
      JigsawStructure.MaxDistance p_422367_,
      PoolAliasLookup p_312146_,
      DimensionPadding p_342449_,
      LiquidSettings p_344414_
   ) {
      RegistryAccess registryaccess = p_227239_.registryAccess();
      ChunkGenerator chunkgenerator = p_227239_.chunkGenerator();
      StructureTemplateManager structuretemplatemanager = p_227239_.structureTemplateManager();
      LevelHeightAccessor levelheightaccessor = p_227239_.heightAccessor();
      WorldgenRandom worldgenrandom = p_227239_.random();
      Registry<StructureTemplatePool> registry = registryaccess.lookupOrThrow(Registries.TEMPLATE_POOL);
      Rotation rotation = Rotation.getRandom(worldgenrandom);
      StructureTemplatePool structuretemplatepool = p_227240_.unwrapKey()
         .flatMap(p_309329_ -> registry.getOptional(p_312146_.lookup((ResourceKey<StructureTemplatePool>)p_309329_)))
         .orElse(p_227240_.value());
      StructurePoolElement structurepoolelement = structuretemplatepool.getRandomTemplate(worldgenrandom);
      if (structurepoolelement == EmptyPoolElement.INSTANCE) {
         return Optional.empty();
      }

      BlockPos blockpos;
      if (p_227241_.isPresent()) {
         Identifier identifier = p_227241_.get();
         Optional<BlockPos> optional = getRandomNamedJigsaw(structurepoolelement, identifier, p_227243_, rotation, structuretemplatemanager, worldgenrandom);
         if (optional.isEmpty()) {
            LOGGER.error(
               "No starting jigsaw {} found in start pool {}",
               identifier,
               p_227240_.unwrapKey().map(p_450020_ -> p_450020_.identifier().toString()).orElse("<unregistered>")
            );
            return Optional.empty();
         }

         blockpos = optional.get();
      } else {
         blockpos = p_227243_;
      }

      Vec3i vec3i = blockpos.subtract(p_227243_);
      BlockPos blockpos1 = p_227243_.subtract(vec3i);
      PoolElementStructurePiece poolelementstructurepiece = new PoolElementStructurePiece(
         structuretemplatemanager,
         structurepoolelement,
         blockpos1,
         structurepoolelement.getGroundLevelDelta(),
         rotation,
         structurepoolelement.getBoundingBox(structuretemplatemanager, blockpos1, rotation),
         p_344414_
      );
      BoundingBox boundingbox = poolelementstructurepiece.getBoundingBox();
      int i = (boundingbox.maxX() + boundingbox.minX()) / 2;
      int j = (boundingbox.maxZ() + boundingbox.minZ()) / 2;
      int k = p_227245_.isEmpty()
         ? blockpos1.getY()
         : p_227243_.getY() + chunkgenerator.getFirstFreeHeight(i, j, p_227245_.get(), levelheightaccessor, p_227239_.randomState());
      int l = boundingbox.minY() + poolelementstructurepiece.getGroundLevelDelta();
      poolelementstructurepiece.move(0, k - l, 0);
      if (isStartTooCloseToWorldHeightLimits(levelheightaccessor, p_342449_, poolelementstructurepiece.getBoundingBox())) {
         LOGGER.debug(
            "Center piece {} with bounding box {} does not fit dimension padding {}",
            new Object[]{structurepoolelement, poolelementstructurepiece.getBoundingBox(), p_342449_}
         );
         return Optional.empty();
      } else {
         int i1 = k + vec3i.getY();
         return Optional.of(
            new Structure.GenerationStub(
               new BlockPos(i, i1, j),
               p_341930_ -> {
                  List<PoolElementStructurePiece> list = Lists.newArrayList();
                  list.add(poolelementstructurepiece);
                  if (p_227242_ > 0) {
                     AABB aabb = new AABB(
                        i - p_422367_.horizontal(),
                        Math.max(i1 - p_422367_.vertical(), levelheightaccessor.getMinY() + p_342449_.bottom()),
                        j - p_422367_.horizontal(),
                        i + p_422367_.horizontal() + 1,
                        Math.min(i1 + p_422367_.vertical() + 1, levelheightaccessor.getMaxY() + 1 - p_342449_.top()),
                        j + p_422367_.horizontal() + 1
                     );
                     VoxelShape voxelshape = Shapes.join(Shapes.create(aabb), Shapes.create(AABB.of(boundingbox)), BooleanOp.ONLY_FIRST);
                     addPieces(
                        p_227239_.randomState(),
                        p_227242_,
                        p_227244_,
                        chunkgenerator,
                        structuretemplatemanager,
                        levelheightaccessor,
                        worldgenrandom,
                        registry,
                        poolelementstructurepiece,
                        list,
                        voxelshape,
                        p_312146_,
                        p_344414_
                     );
                     list.forEach(p_341930_::addPiece);
                  }
               }
            )
         );
      }
   }

   private static boolean isStartTooCloseToWorldHeightLimits(LevelHeightAccessor p_375709_, DimensionPadding p_376622_, BoundingBox p_378736_) {
      if (p_376622_ == DimensionPadding.ZERO) {
         return false;
      }

      int i = p_375709_.getMinY() + p_376622_.bottom();
      int j = p_375709_.getMaxY() - p_376622_.top();
      return p_378736_.minY() < i || p_378736_.maxY() > j;
   }

   private static Optional<BlockPos> getRandomNamedJigsaw(
      StructurePoolElement p_227248_,
      Identifier p_458833_,
      BlockPos p_227250_,
      Rotation p_227251_,
      StructureTemplateManager p_227252_,
      WorldgenRandom p_227253_
   ) {
      for (StructureTemplate.JigsawBlockInfo structuretemplate$jigsawblockinfo : p_227248_.getShuffledJigsawBlocks(p_227252_, p_227250_, p_227251_, p_227253_)) {
         if (p_458833_.equals(structuretemplate$jigsawblockinfo.name())) {
            return Optional.of(structuretemplate$jigsawblockinfo.info().pos());
         }
      }

      return Optional.empty();
   }

   private static void addPieces(
      RandomState p_227211_,
      int p_227212_,
      boolean p_227213_,
      ChunkGenerator p_227214_,
      StructureTemplateManager p_227215_,
      LevelHeightAccessor p_227216_,
      RandomSource p_227217_,
      Registry<StructureTemplatePool> p_227218_,
      PoolElementStructurePiece p_227219_,
      List<PoolElementStructurePiece> p_227220_,
      VoxelShape p_227221_,
      PoolAliasLookup p_312553_,
      LiquidSettings p_343434_
   ) {
      JigsawPlacement.Placer jigsawplacement$placer = new JigsawPlacement.Placer(p_227218_, p_227212_, p_227214_, p_227215_, p_227220_, p_227217_);
      jigsawplacement$placer.tryPlacingChildren(p_227219_, new MutableObject(p_227221_), 0, p_227213_, p_227216_, p_227211_, p_312553_, p_343434_);

      while (jigsawplacement$placer.placing.hasNext()) {
         JigsawPlacement.PieceState jigsawplacement$piecestate = (JigsawPlacement.PieceState)jigsawplacement$placer.placing.next();
         jigsawplacement$placer.tryPlacingChildren(
            jigsawplacement$piecestate.piece,
            jigsawplacement$piecestate.free,
            jigsawplacement$piecestate.depth,
            p_227213_,
            p_227216_,
            p_227211_,
            p_312553_,
            p_343434_
         );
      }
   }

   public static boolean generateJigsaw(
      ServerLevel p_227204_, Holder<StructureTemplatePool> p_227205_, Identifier p_452846_, int p_227207_, BlockPos p_227208_, boolean p_227209_
   ) {
      ChunkGenerator chunkgenerator = p_227204_.getChunkSource().getGenerator();
      StructureTemplateManager structuretemplatemanager = p_227204_.getStructureManager();
      StructureManager structuremanager = p_227204_.structureManager();
      RandomSource randomsource = p_227204_.getRandom();
      Structure.GenerationContext structure$generationcontext = new Structure.GenerationContext(
         p_227204_.registryAccess(),
         chunkgenerator,
         chunkgenerator.getBiomeSource(),
         p_227204_.getChunkSource().randomState(),
         structuretemplatemanager,
         p_227204_.getSeed(),
         new ChunkPos(p_227208_),
         p_227204_,
         p_227255_ -> true
      );
      Optional<Structure.GenerationStub> optional = addPieces(
         structure$generationcontext,
         p_227205_,
         Optional.of(p_452846_),
         p_227207_,
         p_227208_,
         false,
         Optional.empty(),
         new JigsawStructure.MaxDistance(128),
         PoolAliasLookup.EMPTY,
         JigsawStructure.DEFAULT_DIMENSION_PADDING,
         JigsawStructure.DEFAULT_LIQUID_SETTINGS
      );
      if (optional.isPresent()) {
         StructurePiecesBuilder structurepiecesbuilder = optional.get().getPiecesBuilder();

         for (StructurePiece structurepiece : structurepiecesbuilder.build().pieces()) {
            if (structurepiece instanceof PoolElementStructurePiece poolelementstructurepiece) {
               poolelementstructurepiece.place(p_227204_, structuremanager, chunkgenerator, randomsource, BoundingBox.infinite(), p_227208_, p_227209_);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   record PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth) {
   }

   static final class Placer {
      private final Registry<StructureTemplatePool> pools;
      private final int maxDepth;
      private final ChunkGenerator chunkGenerator;
      private final StructureTemplateManager structureTemplateManager;
      private final List<? super PoolElementStructurePiece> pieces;
      private final RandomSource random;
      final SequencedPriorityIterator<JigsawPlacement.PieceState> placing = new SequencedPriorityIterator<>();

      Placer(
         Registry<StructureTemplatePool> p_227258_,
         int p_227259_,
         ChunkGenerator p_227260_,
         StructureTemplateManager p_227261_,
         List<? super PoolElementStructurePiece> p_227262_,
         RandomSource p_227263_
      ) {
         this.pools = p_227258_;
         this.maxDepth = p_227259_;
         this.chunkGenerator = p_227260_;
         this.structureTemplateManager = p_227261_;
         this.pieces = p_227262_;
         this.random = p_227263_;
      }

      void tryPlacingChildren(
         PoolElementStructurePiece p_227265_,
         MutableObject<VoxelShape> p_227266_,
         int p_227267_,
         boolean p_227268_,
         LevelHeightAccessor p_227269_,
         RandomState p_227270_,
         PoolAliasLookup p_311045_,
         LiquidSettings p_344945_
      ) {
         StructurePoolElement structurepoolelement = p_227265_.getElement();
         BlockPos blockpos = p_227265_.getPosition();
         Rotation rotation = p_227265_.getRotation();
         StructureTemplatePool.Projection structuretemplatepool$projection = structurepoolelement.getProjection();
         boolean flag = structuretemplatepool$projection == StructureTemplatePool.Projection.RIGID;
         MutableObject<VoxelShape> mutableobject = new MutableObject();
         BoundingBox boundingbox = p_227265_.getBoundingBox();
         int i = boundingbox.minY();

         label128:
         for (StructureTemplate.JigsawBlockInfo structuretemplate$jigsawblockinfo : structurepoolelement.getShuffledJigsawBlocks(
            this.structureTemplateManager, blockpos, rotation, this.random
         )) {
            StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = structuretemplate$jigsawblockinfo.info();
            Direction direction = JigsawBlock.getFrontFacing(structuretemplate$structureblockinfo.state());
            BlockPos blockpos1 = structuretemplate$structureblockinfo.pos();
            BlockPos blockpos2 = blockpos1.relative(direction);
            int j = blockpos1.getY() - i;
            int k = Integer.MIN_VALUE;
            ResourceKey<StructureTemplatePool> resourcekey = p_311045_.lookup(structuretemplate$jigsawblockinfo.pool());
            Optional<? extends Holder<StructureTemplatePool>> optional = this.pools.get(resourcekey);
            if (optional.isEmpty()) {
               JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", resourcekey.identifier());
            } else {
               Holder<StructureTemplatePool> holder = (Holder<StructureTemplatePool>)optional.get();
               if (holder.value().size() == 0 && !holder.is(Pools.EMPTY)) {
                  JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", resourcekey.identifier());
               } else {
                  Holder<StructureTemplatePool> holder1 = holder.value().getFallback();
                  if (holder1.value().size() == 0 && !holder1.is(Pools.EMPTY)) {
                     JigsawPlacement.LOGGER
                        .warn(
                           "Empty or non-existent fallback pool: {}",
                           holder1.unwrapKey().map(p_450021_ -> p_450021_.identifier().toString()).orElse("<unregistered>")
                        );
                  } else {
                     boolean flag1 = boundingbox.isInside(blockpos2);
                     MutableObject<VoxelShape> mutableobject1;
                     if (flag1) {
                        mutableobject1 = mutableobject;
                        if (mutableobject.get() == null) {
                           mutableobject.setValue(Shapes.create(AABB.of(boundingbox)));
                        }
                     } else {
                        mutableobject1 = p_227266_;
                     }

                     List<StructurePoolElement> list = Lists.newArrayList();
                     if (p_227267_ != this.maxDepth) {
                        list.addAll(holder.value().getShuffledTemplates(this.random));
                     }

                     list.addAll(holder1.value().getShuffledTemplates(this.random));
                     int l = structuretemplate$jigsawblockinfo.placementPriority();

                     for (StructurePoolElement structurepoolelement1 : list) {
                        if (structurepoolelement1 == EmptyPoolElement.INSTANCE) {
                           break;
                        }

                        for (Rotation rotation1 : Rotation.getShuffled(this.random)) {
                           List<StructureTemplate.JigsawBlockInfo> list1 = structurepoolelement1.getShuffledJigsawBlocks(
                              this.structureTemplateManager, BlockPos.ZERO, rotation1, this.random
                           );
                           BoundingBox boundingbox1 = structurepoolelement1.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, rotation1);
                           int i1;
                           if (p_227268_ && boundingbox1.getYSpan() <= 16) {
                              i1 = list1.stream()
                                 .mapToInt(
                                    p_360623_ -> {
                                       StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 = p_360623_.info();
                                       if (!boundingbox1.isInside(
                                          structuretemplate$structureblockinfo1.pos()
                                             .relative(JigsawBlock.getFrontFacing(structuretemplate$structureblockinfo1.state()))
                                       )) {
                                          return 0;
                                       }

                                       ResourceKey<StructureTemplatePool> resourcekey1 = p_311045_.lookup(p_360623_.pool());
                                       Optional<? extends Holder<StructureTemplatePool>> optional1 = this.pools.get(resourcekey1);
                                       Optional<Holder<StructureTemplatePool>> optional2 = optional1.map(p_255600_ -> p_255600_.value().getFallback());
                                       int k3 = optional1.<Integer>map(p_255596_ -> p_255596_.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                       int l3 = optional2.<Integer>map(p_255601_ -> p_255601_.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                       return Math.max(k3, l3);
                                    }
                                 )
                                 .max()
                                 .orElse(0);
                           } else {
                              i1 = 0;
                           }

                           for (StructureTemplate.JigsawBlockInfo structuretemplate$jigsawblockinfo1 : list1) {
                              if (JigsawBlock.canAttach(structuretemplate$jigsawblockinfo, structuretemplate$jigsawblockinfo1)) {
                                 BlockPos blockpos3 = structuretemplate$jigsawblockinfo1.info().pos();
                                 BlockPos blockpos4 = blockpos2.subtract(blockpos3);
                                 BoundingBox boundingbox2 = structurepoolelement1.getBoundingBox(this.structureTemplateManager, blockpos4, rotation1);
                                 int j1 = boundingbox2.minY();
                                 StructureTemplatePool.Projection structuretemplatepool$projection1 = structurepoolelement1.getProjection();
                                 boolean flag2 = structuretemplatepool$projection1 == StructureTemplatePool.Projection.RIGID;
                                 int k1 = blockpos3.getY();
                                 int l1 = j - k1 + JigsawBlock.getFrontFacing(structuretemplate$structureblockinfo.state()).getStepY();
                                 int i2;
                                 if (flag && flag2) {
                                    i2 = i + l1;
                                 } else {
                                    if (k == Integer.MIN_VALUE) {
                                       k = this.chunkGenerator
                                          .getFirstFreeHeight(blockpos1.getX(), blockpos1.getZ(), Heightmap.Types.WORLD_SURFACE_WG, p_227269_, p_227270_);
                                    }

                                    i2 = k - k1;
                                 }

                                 int j2 = i2 - j1;
                                 BoundingBox boundingbox3 = boundingbox2.moved(0, j2, 0);
                                 BlockPos blockpos5 = blockpos4.offset(0, j2, 0);
                                 if (i1 > 0) {
                                    int k2 = Math.max(i1 + 1, boundingbox3.maxY() - boundingbox3.minY());
                                    boundingbox3.encapsulate(new BlockPos(boundingbox3.minX(), boundingbox3.minY() + k2, boundingbox3.minZ()));
                                 }

                                 if (!Shapes.joinIsNotEmpty(
                                    (VoxelShape)mutableobject1.get(), Shapes.create(AABB.of(boundingbox3).deflate(0.25)), BooleanOp.ONLY_SECOND
                                 )) {
                                    mutableobject1.setValue(
                                       Shapes.joinUnoptimized((VoxelShape)mutableobject1.get(), Shapes.create(AABB.of(boundingbox3)), BooleanOp.ONLY_FIRST)
                                    );
                                    int j3 = p_227265_.getGroundLevelDelta();
                                    int l2;
                                    if (flag2) {
                                       l2 = j3 - l1;
                                    } else {
                                       l2 = structurepoolelement1.getGroundLevelDelta();
                                    }

                                    PoolElementStructurePiece poolelementstructurepiece = new PoolElementStructurePiece(
                                       this.structureTemplateManager, structurepoolelement1, blockpos5, l2, rotation1, boundingbox3, p_344945_
                                    );
                                    int i3;
                                    if (flag) {
                                       i3 = i + j;
                                    } else if (flag2) {
                                       i3 = i2 + k1;
                                    } else {
                                       if (k == Integer.MIN_VALUE) {
                                          k = this.chunkGenerator
                                             .getFirstFreeHeight(blockpos1.getX(), blockpos1.getZ(), Heightmap.Types.WORLD_SURFACE_WG, p_227269_, p_227270_);
                                       }

                                       i3 = k + l1 / 2;
                                    }

                                    p_227265_.addJunction(
                                       new JigsawJunction(blockpos2.getX(), i3 - j + j3, blockpos2.getZ(), l1, structuretemplatepool$projection1)
                                    );
                                    poolelementstructurepiece.addJunction(
                                       new JigsawJunction(blockpos1.getX(), i3 - k1 + l2, blockpos1.getZ(), -l1, structuretemplatepool$projection)
                                    );
                                    this.pieces.add(poolelementstructurepiece);
                                    if (p_227267_ + 1 <= this.maxDepth) {
                                       JigsawPlacement.PieceState jigsawplacement$piecestate = new JigsawPlacement.PieceState(
                                          poolelementstructurepiece, mutableobject1, p_227267_ + 1
                                       );
                                       this.placing.add(jigsawplacement$piecestate, l);
                                    }
                                    continue label128;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
