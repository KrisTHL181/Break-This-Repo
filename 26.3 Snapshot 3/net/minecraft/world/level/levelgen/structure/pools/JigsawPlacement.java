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
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int UNSET_HEIGHT = Integer.MIN_VALUE;

   public static Optional<Structure.GenerationStub> addPieces(
      final Structure.GenerationContext context,
      final Holder<StructureTemplatePool> startPool,
      final Optional<Identifier> startJigsaw,
      final int maxDepth,
      final BlockPos position,
      final boolean doExpansionHack,
      final Optional<Heightmap.Types> projectStartToHeightmap,
      final JigsawStructure.MaxDistance maxDistanceFromCenter,
      final PoolAliasLookup poolAliasLookup,
      final DimensionPadding dimensionPadding,
      final LiquidSettings liquidSettings
   ) {
      RegistryAccess registryAccess = context.registryAccess();
      ChunkGenerator chunkGenerator = context.chunkGenerator();
      StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
      LevelHeightAccessor heightAccessor = context.heightAccessor();
      WorldgenRandom random = context.random();
      Registry<StructureTemplatePool> pools = registryAccess.lookupOrThrow(Registries.TEMPLATE_POOL);
      Rotation centerRotation = Rotation.getRandom(random);
      StructureTemplatePool centerPool = startPool.unwrapKey()
         .flatMap(key -> pools.getOptional(poolAliasLookup.lookup((ResourceKey<StructureTemplatePool>)key)))
         .orElse(startPool.value());
      StructurePoolElement centerElement = centerPool.getRandomTemplate(random);
      if (centerElement == EmptyPoolElement.INSTANCE) {
         return Optional.empty();
      }

      BlockPos anchoredPosition;
      if (startJigsaw.isPresent()) {
         Identifier targetJigsawId = startJigsaw.get();
         Optional<BlockPos> anchor = getRandomNamedJigsaw(centerElement, targetJigsawId, position, centerRotation, structureTemplateManager, random);
         if (anchor.isEmpty()) {
            LOGGER.error(
               "No starting jigsaw {} found in start pool {}",
               targetJigsawId,
               startPool.unwrapKey().map(key -> key.identifier().toString()).orElse("<unregistered>")
            );
            return Optional.empty();
         }

         anchoredPosition = anchor.get();
      } else {
         anchoredPosition = position;
      }

      Vec3i localAnchorPosition = anchoredPosition.subtract(position);
      BlockPos adjustedPosition = position.subtract(localAnchorPosition);
      PoolElementStructurePiece centerPiece = new PoolElementStructurePiece(
         structureTemplateManager,
         centerElement,
         adjustedPosition,
         centerElement.getGroundLevelDelta(),
         centerRotation,
         centerElement.getBoundingBox(structureTemplateManager, adjustedPosition, centerRotation),
         liquidSettings
      );
      BoundingBox box = centerPiece.getBoundingBox();
      int centerX = (box.maxX() + box.minX()) / 2;
      int centerZ = (box.maxZ() + box.minZ()) / 2;
      int bottomY = projectStartToHeightmap.isEmpty()
         ? adjustedPosition.getY()
         : position.getY() + chunkGenerator.getFirstFreeHeight(centerX, centerZ, projectStartToHeightmap.get(), heightAccessor, context.randomState());
      int oldAbsoluteGroundY = box.minY() + centerPiece.getGroundLevelDelta();
      centerPiece.move(0, bottomY - oldAbsoluteGroundY, 0);
      if (isStartTooCloseToWorldHeightLimits(heightAccessor, dimensionPadding, centerPiece.getBoundingBox())) {
         LOGGER.debug(
            "Center piece {} with bounding box {} does not fit dimension padding {}",
            new Object[]{centerElement, centerPiece.getBoundingBox(), dimensionPadding}
         );
         return Optional.empty();
      } else {
         int centerY = bottomY + localAnchorPosition.getY();
         return Optional.of(
            new Structure.GenerationStub(
               new BlockPos(centerX, centerY, centerZ),
               builder -> {
                  List<PoolElementStructurePiece> pieces = Lists.newArrayList();
                  pieces.add(centerPiece);
                  if (maxDepth > 0) {
                     AABB aabb = new AABB(
                        centerX - maxDistanceFromCenter.horizontal(),
                        Math.max(centerY - maxDistanceFromCenter.vertical(), heightAccessor.getMinY() + dimensionPadding.bottom()),
                        centerZ - maxDistanceFromCenter.horizontal(),
                        centerX + maxDistanceFromCenter.horizontal() + 1,
                        Math.min(centerY + maxDistanceFromCenter.vertical() + 1, heightAccessor.getMaxY() + 1 - dimensionPadding.top()),
                        centerZ + maxDistanceFromCenter.horizontal() + 1
                     );
                     VoxelShape shape = Shapes.join(Shapes.create(aabb), Shapes.create(AABB.of(box)), BooleanOp.ONLY_FIRST);
                     addPieces(
                        context.randomState(),
                        maxDepth,
                        doExpansionHack,
                        chunkGenerator,
                        structureTemplateManager,
                        heightAccessor,
                        random,
                        pools,
                        centerPiece,
                        pieces,
                        shape,
                        poolAliasLookup,
                        liquidSettings
                     );
                     pieces.forEach(builder::addPiece);
                  }
               }
            )
         );
      }
   }

   private static boolean isStartTooCloseToWorldHeightLimits(
      final LevelHeightAccessor heightAccessor, final DimensionPadding dimensionPadding, final BoundingBox centerPieceBb
   ) {
      if (dimensionPadding == DimensionPadding.ZERO) {
         return false;
      }

      int minYWithPadding = heightAccessor.getMinY() + dimensionPadding.bottom();
      int maxYWithPadding = heightAccessor.getMaxY() - dimensionPadding.top();
      return centerPieceBb.minY() < minYWithPadding || centerPieceBb.maxY() > maxYWithPadding;
   }

   private static Optional<BlockPos> getRandomNamedJigsaw(
      final StructurePoolElement element,
      final Identifier targetJigsawId,
      final BlockPos position,
      final Rotation rotation,
      final StructureTemplateManager structureTemplateManager,
      final WorldgenRandom random
   ) {
      for (StructureTemplate.JigsawBlockInfo jigsaw : element.getShuffledJigsawBlocks(structureTemplateManager, position, rotation, random)) {
         if (targetJigsawId.equals(jigsaw.name())) {
            return Optional.of(jigsaw.info().pos());
         }
      }

      return Optional.empty();
   }

   private static void addPieces(
      final RandomState randomState,
      final int maxDepth,
      final boolean doExpansionHack,
      final ChunkGenerator chunkGenerator,
      final StructureTemplateManager structureTemplateManager,
      final LevelHeightAccessor heightAccessor,
      final RandomSource random,
      final Registry<StructureTemplatePool> pools,
      final PoolElementStructurePiece centerPiece,
      final List<PoolElementStructurePiece> pieces,
      final VoxelShape shape,
      final PoolAliasLookup poolAliasLookup,
      final LiquidSettings liquidSettings
   ) {
      JigsawPlacement.Placer placer = new JigsawPlacement.Placer(pools, maxDepth, chunkGenerator, structureTemplateManager, pieces, random);
      placer.tryPlacingChildren(centerPiece, new MutableObject(shape), 0, doExpansionHack, heightAccessor, randomState, poolAliasLookup, liquidSettings);

      while (placer.placing.hasNext()) {
         JigsawPlacement.PieceState state = (JigsawPlacement.PieceState)placer.placing.next();
         placer.tryPlacingChildren(state.piece, state.free, state.depth, doExpansionHack, heightAccessor, randomState, poolAliasLookup, liquidSettings);
      }
   }

   public static boolean generateJigsaw(
      final ServerLevel level,
      final Holder<StructureTemplatePool> pool,
      final Identifier target,
      final int maxDepth,
      final BlockPos position,
      final boolean keepJigsaws
   ) {
      ChunkGenerator generator = level.getChunkSource().getGenerator();
      StructureTemplateManager structureTemplateManager = level.getStructureManager();
      StructureManager structureManager = level.structureManager();
      RandomSource random = level.getRandom();
      Structure.GenerationContext generationContext = new Structure.GenerationContext(
         level.registryAccess(),
         generator,
         generator.getBiomeSource(),
         level.getChunkSource().randomState(),
         structureTemplateManager,
         level.getSeed(),
         ChunkPos.containing(position),
         level,
         b -> true
      );
      Optional<Structure.GenerationStub> stub = addPieces(
         generationContext,
         pool,
         Optional.of(target),
         maxDepth,
         position,
         false,
         Optional.empty(),
         new JigsawStructure.MaxDistance(128),
         PoolAliasLookup.EMPTY,
         JigsawStructure.DEFAULT_DIMENSION_PADDING,
         JigsawStructure.DEFAULT_LIQUID_SETTINGS
      );
      if (stub.isPresent()) {
         StructurePiecesBuilder builder = stub.get().getPiecesBuilder();

         for (StructurePiece piece : builder.build().pieces()) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
               poolPiece.place(level, structureManager, generator, random, BoundingBox.infinite(), position, keepJigsaws);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private record PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth) {
   }

   private static final class Placer {
      private final Registry<StructureTemplatePool> pools;
      private final int maxDepth;
      private final ChunkGenerator chunkGenerator;
      private final StructureTemplateManager structureTemplateManager;
      private final List<? super PoolElementStructurePiece> pieces;
      private final RandomSource random;
      private final SequencedPriorityIterator<JigsawPlacement.PieceState> placing = new SequencedPriorityIterator<>();

      private Placer(
         final Registry<StructureTemplatePool> pools,
         final int maxDepth,
         final ChunkGenerator chunkGenerator,
         final StructureTemplateManager structureTemplateManager,
         final List<? super PoolElementStructurePiece> pieces,
         final RandomSource random
      ) {
         this.pools = pools;
         this.maxDepth = maxDepth;
         this.chunkGenerator = chunkGenerator;
         this.structureTemplateManager = structureTemplateManager;
         this.pieces = pieces;
         this.random = random;
      }

      private void tryPlacingChildren(
         final PoolElementStructurePiece sourcePiece,
         final MutableObject<VoxelShape> contextFree,
         final int depth,
         final boolean doExpansionHack,
         final LevelHeightAccessor heightAccessor,
         final RandomState randomState,
         final PoolAliasLookup poolAliasLookup,
         final LiquidSettings liquidSettings
      ) {
         StructurePoolElement sourceElement = sourcePiece.getElement();
         BlockPos sourceBoxPosition = sourcePiece.getPosition();
         Rotation sourceRotation = sourcePiece.getRotation();
         StructureTemplatePool.Projection sourceProjection = sourceElement.getProjection();
         boolean sourceRigid = sourceProjection == StructureTemplatePool.Projection.RIGID;
         MutableObject<VoxelShape> sourceFree = new MutableObject();
         BoundingBox sourceBB = sourcePiece.getBoundingBox();
         int sourceBoxY = sourceBB.minY();

         label129:
         for (StructureTemplate.JigsawBlockInfo sourceJigsaw : sourceElement.getShuffledJigsawBlocks(
            this.structureTemplateManager, sourceBoxPosition, sourceRotation, this.random
         )) {
            StructureTemplate.StructureBlockInfo sourceJigsawInfo = sourceJigsaw.info();
            Direction sourceDirection = JigsawBlock.getFrontFacing(sourceJigsawInfo.state());
            BlockPos sourceJigsawPos = sourceJigsawInfo.pos();
            BlockPos targetJigsawPos = sourceJigsawPos.relative(sourceDirection);
            int sourceJigsawLocalY = sourceJigsawPos.getY() - sourceBoxY;
            int sourceJigsawBaseHeight = Integer.MIN_VALUE;
            ResourceKey<StructureTemplatePool> poolName = poolAliasLookup.lookup(sourceJigsaw.pool());
            Optional<? extends Holder<StructureTemplatePool>> maybeTargetPool = this.pools.get(poolName);
            if (maybeTargetPool.isEmpty()) {
               JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", poolName.identifier());
            } else {
               Holder<StructureTemplatePool> targetPool = (Holder<StructureTemplatePool>)maybeTargetPool.get();
               if (targetPool.value().size() == 0 && !targetPool.is(Pools.EMPTY)) {
                  JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", poolName.identifier());
               } else {
                  Holder<StructureTemplatePool> fallback = targetPool.value().getFallback();
                  if (fallback.value().size() == 0 && !fallback.is(Pools.EMPTY)) {
                     JigsawPlacement.LOGGER
                        .warn("Empty or non-existent fallback pool: {}", fallback.unwrapKey().map(e -> e.identifier().toString()).orElse("<unregistered>"));
                  } else {
                     boolean attachInsideSource = sourceBB.isInside(targetJigsawPos);
                     MutableObject<VoxelShape> childrenFree;
                     if (attachInsideSource) {
                        childrenFree = sourceFree;
                        if (sourceFree.get() == null) {
                           sourceFree.setValue(Shapes.create(AABB.of(sourceBB)));
                        }
                     } else {
                        childrenFree = contextFree;
                     }

                     List<StructurePoolElement> targetPieces = Lists.newArrayList();
                     if (depth != this.maxDepth) {
                        targetPieces.addAll(targetPool.value().getShuffledTemplates(this.random));
                     }

                     targetPieces.addAll(fallback.value().getShuffledTemplates(this.random));
                     int placementPriority = sourceJigsaw.placementPriority();

                     for (StructurePoolElement targetElement : targetPieces) {
                        if (targetElement == EmptyPoolElement.INSTANCE) {
                           break;
                        }

                        for (Rotation targetRotation : Rotation.getShuffled(this.random)) {
                           List<StructureTemplate.JigsawBlockInfo> targetJigsaws = targetElement.getShuffledJigsawBlocks(
                              this.structureTemplateManager, BlockPos.ZERO, targetRotation, this.random
                           );
                           BoundingBox hackBox = targetElement.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, targetRotation);
                           int expandTo;
                           if (doExpansionHack && hackBox.getYSpan() <= 16) {
                              expandTo = targetJigsaws.stream().mapToInt(targetJigsawx -> {
                                 StructureTemplate.StructureBlockInfo targetJigsawInfo = targetJigsawx.info();
                                 if (!hackBox.isInside(targetJigsawInfo.pos().relative(JigsawBlock.getFrontFacing(targetJigsawInfo.state())))) {
                                    return 0;
                                 }

                                 ResourceKey<StructureTemplatePool> childPoolName = poolAliasLookup.lookup(targetJigsawx.pool());
                                 Optional<? extends Holder<StructureTemplatePool>> childPool = this.pools.get(childPoolName);
                                 Optional<Holder<StructureTemplatePool>> childFallbackPool = childPool.map(p -> p.value().getFallback());
                                 int childPoolSize = childPool.<Integer>map(p -> p.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                 int childFallbackSize = childFallbackPool.<Integer>map(p -> p.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                 return Math.max(childPoolSize, childFallbackSize);
                              }).max().orElse(0);
                           } else {
                              expandTo = 0;
                           }

                           for (StructureTemplate.JigsawBlockInfo targetJigsaw : targetJigsaws) {
                              if (JigsawBlock.canAttach(sourceJigsaw, targetJigsaw)) {
                                 BlockPos targetJigsawLocalPos = targetJigsaw.info().pos();
                                 BlockPos rawTargetBoxPos = targetJigsawPos.subtract(targetJigsawLocalPos);
                                 BoundingBox rawTargetBB = targetElement.getBoundingBox(this.structureTemplateManager, rawTargetBoxPos, targetRotation);
                                 int rawTargetY = rawTargetBB.minY();
                                 StructureTemplatePool.Projection targetProjection = targetElement.getProjection();
                                 boolean targetRigid = targetProjection == StructureTemplatePool.Projection.RIGID;
                                 int targetJigsawLocalY = targetJigsawLocalPos.getY();
                                 int deltaY = sourceJigsawLocalY - targetJigsawLocalY + JigsawBlock.getFrontFacing(sourceJigsawInfo.state()).getStepY();
                                 int targetBoxY;
                                 if (sourceRigid && targetRigid) {
                                    targetBoxY = sourceBoxY + deltaY;
                                 } else {
                                    if (sourceJigsawBaseHeight == Integer.MIN_VALUE) {
                                       sourceJigsawBaseHeight = this.chunkGenerator
                                          .getFirstFreeHeight(
                                             sourceJigsawPos.getX(), sourceJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState
                                          );
                                    }

                                    targetBoxY = sourceJigsawBaseHeight - targetJigsawLocalY;
                                 }

                                 int yOffset = targetBoxY - rawTargetY;
                                 BoundingBox targetBB = rawTargetBB.moved(0, yOffset, 0);
                                 BlockPos targetBoxPosition = rawTargetBoxPos.offset(0, yOffset, 0);
                                 if (expandTo > 0) {
                                    int newSize = Math.max(expandTo + 1, targetBB.maxY() - targetBB.minY());
                                    targetBB.encapsulate(new BlockPos(targetBB.minX(), targetBB.minY() + newSize, targetBB.minZ()));
                                 }

                                 if (!Shapes.joinIsNotEmpty(
                                    (VoxelShape)childrenFree.get(), Shapes.create(AABB.of(targetBB).deflate(0.25)), BooleanOp.ONLY_SECOND
                                 )) {
                                    childrenFree.setValue(
                                       Shapes.joinUnoptimized((VoxelShape)childrenFree.get(), Shapes.create(AABB.of(targetBB)), BooleanOp.ONLY_FIRST)
                                    );
                                    int sourceGroundLevelDelta = sourcePiece.getGroundLevelDelta();
                                    int targetGroundLevelDelta;
                                    if (targetRigid) {
                                       targetGroundLevelDelta = sourceGroundLevelDelta - deltaY;
                                    } else {
                                       targetGroundLevelDelta = targetElement.getGroundLevelDelta();
                                    }

                                    PoolElementStructurePiece targetPiece = new PoolElementStructurePiece(
                                       this.structureTemplateManager,
                                       targetElement,
                                       targetBoxPosition,
                                       targetGroundLevelDelta,
                                       targetRotation,
                                       targetBB,
                                       liquidSettings
                                    );
                                    int junctionY;
                                    if (sourceRigid) {
                                       junctionY = sourceBoxY + sourceJigsawLocalY;
                                    } else if (targetRigid) {
                                       junctionY = targetBoxY + targetJigsawLocalY;
                                    } else {
                                       if (sourceJigsawBaseHeight == Integer.MIN_VALUE) {
                                          sourceJigsawBaseHeight = this.chunkGenerator
                                             .getFirstFreeHeight(
                                                sourceJigsawPos.getX(), sourceJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState
                                             );
                                       }

                                       junctionY = sourceJigsawBaseHeight + deltaY / 2;
                                    }

                                    sourcePiece.addJunction(
                                       new JigsawJunction(
                                          targetJigsawPos.getX(),
                                          junctionY - sourceJigsawLocalY + sourceGroundLevelDelta,
                                          targetJigsawPos.getZ(),
                                          deltaY,
                                          targetProjection
                                       )
                                    );
                                    targetPiece.addJunction(
                                       new JigsawJunction(
                                          sourceJigsawPos.getX(),
                                          junctionY - targetJigsawLocalY + targetGroundLevelDelta,
                                          sourceJigsawPos.getZ(),
                                          -deltaY,
                                          sourceProjection
                                       )
                                    );
                                    this.pieces.add(targetPiece);
                                    if (depth + 1 <= this.maxDepth) {
                                       JigsawPlacement.PieceState state = new JigsawPlacement.PieceState(targetPiece, childrenFree, depth + 1);
                                       this.placing.add(state, placementPriority);
                                    }
                                    continue label129;
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
