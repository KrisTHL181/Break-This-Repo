package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class EndCityPieces {
   private static final int MAX_GEN_DEPTH = 8;
   private static final EndCityPieces.SectionGenerator HOUSE_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         final StructureTemplateManager structureTemplateManager,
         final int genDepth,
         final EndCityPieces.EndCityPiece parent,
         final BlockPos offset,
         final List<StructurePiece> pieces,
         final RandomSource random
      ) {
         if (genDepth > 8) {
            return false;
         }

         Rotation rotation = parent.placeSettings().getRotation();
         EndCityPieces.EndCityPiece lastPiece = EndCityPieces.addHelper(
            pieces, EndCityPieces.addPiece(structureTemplateManager, parent, offset, "base_floor", rotation, true)
         );
         int numFloors = random.nextInt(3);
         if (numFloors == 0) {
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 4, -1), "base_roof", rotation, true)
            );
         } else if (numFloors == 1) {
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 0, -1), "second_floor_2", rotation, false)
            );
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 8, -1), "second_roof", rotation, false)
            );
            EndCityPieces.recursiveChildren(structureTemplateManager, EndCityPieces.TOWER_GENERATOR, genDepth + 1, lastPiece, null, pieces, random);
         } else if (numFloors == 2) {
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 0, -1), "second_floor_2", rotation, false)
            );
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 4, -1), "third_floor_2", rotation, false)
            );
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 8, -1), "third_roof", rotation, true)
            );
            EndCityPieces.recursiveChildren(structureTemplateManager, EndCityPieces.TOWER_GENERATOR, genDepth + 1, lastPiece, null, pieces, random);
         }

         return true;
      }
   };
   private static final List<Pair<Rotation, BlockPos>> TOWER_BRIDGES = List.of(
      Pair.of(Rotation.NONE, new BlockPos(1, -1, 0)),
      Pair.of(Rotation.CLOCKWISE_90, new BlockPos(6, -1, 1)),
      Pair.of(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 5)),
      Pair.of(Rotation.CLOCKWISE_180, new BlockPos(5, -1, 6))
   );
   private static final EndCityPieces.SectionGenerator TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         final StructureTemplateManager structureTemplateManager,
         final int genDepth,
         final EndCityPieces.EndCityPiece parent,
         final BlockPos offset,
         final List<StructurePiece> pieces,
         final RandomSource random
      ) {
         Rotation rotation = parent.placeSettings().getRotation();
         EndCityPieces.EndCityPiece lastPiece = parent;
         lastPiece = EndCityPieces.addHelper(
            pieces,
            EndCityPieces.addPiece(
               structureTemplateManager, lastPiece, new BlockPos(3 + random.nextInt(2), -3, 3 + random.nextInt(2)), "tower_base", rotation, true
            )
         );
         lastPiece = EndCityPieces.addHelper(
            pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, 7, 0), "tower_piece", rotation, true)
         );
         EndCityPieces.EndCityPiece bridgePiece = random.nextInt(3) == 0 ? lastPiece : null;
         int towerHeight = 1 + random.nextInt(3);

         for (int i = 0; i < towerHeight; i++) {
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, 4, 0), "tower_piece", rotation, true)
            );
            if (i < towerHeight - 1 && random.nextBoolean()) {
               bridgePiece = lastPiece;
            }
         }

         if (bridgePiece != null) {
            for (Pair<Rotation, BlockPos> bridge : EndCityPieces.TOWER_BRIDGES) {
               if (random.nextBoolean()) {
                  EndCityPieces.EndCityPiece bridgeStart = EndCityPieces.addHelper(
                     pieces,
                     EndCityPieces.addPiece(
                        structureTemplateManager,
                        bridgePiece,
                        (BlockPos)bridge.getSecond(),
                        "bridge_end",
                        rotation.getRotated((Rotation)bridge.getFirst()),
                        true
                     )
                  );
                  EndCityPieces.recursiveChildren(
                     structureTemplateManager, EndCityPieces.TOWER_BRIDGE_GENERATOR, genDepth + 1, bridgeStart, null, pieces, random
                  );
               }
            }

            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 4, -1), "tower_top", rotation, true)
            );
         } else {
            if (genDepth != 7) {
               return EndCityPieces.recursiveChildren(
                  structureTemplateManager, EndCityPieces.FAT_TOWER_GENERATOR, genDepth + 1, lastPiece, null, pieces, random
               );
            }

            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 4, -1), "tower_top", rotation, true)
            );
         }

         return true;
      }
   };
   private static final EndCityPieces.SectionGenerator TOWER_BRIDGE_GENERATOR = new EndCityPieces.SectionGenerator() {
      public boolean shipCreated;

      @Override
      public void init() {
         this.shipCreated = false;
      }

      @Override
      public boolean generate(
         final StructureTemplateManager structureTemplateManager,
         final int genDepth,
         final EndCityPieces.EndCityPiece parent,
         final BlockPos offset,
         final List<StructurePiece> pieces,
         final RandomSource random
      ) {
         Rotation rotation = parent.placeSettings().getRotation();
         int bridgeLength = random.nextInt(4) + 1;
         EndCityPieces.EndCityPiece lastPiece = EndCityPieces.addHelper(
            pieces, EndCityPieces.addPiece(structureTemplateManager, parent, new BlockPos(0, 0, -4), "bridge_piece", rotation, true)
         );
         lastPiece.setGenDepth(-1);
         int nextY = 0;

         for (int i = 0; i < bridgeLength; i++) {
            if (random.nextBoolean()) {
               lastPiece = EndCityPieces.addHelper(
                  pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, nextY, -4), "bridge_piece", rotation, true)
               );
               nextY = 0;
            } else {
               if (random.nextBoolean()) {
                  lastPiece = EndCityPieces.addHelper(
                     pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, nextY, -4), "bridge_steep_stairs", rotation, true)
                  );
               } else {
                  lastPiece = EndCityPieces.addHelper(
                     pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, nextY, -8), "bridge_gentle_stairs", rotation, true)
                  );
               }

               nextY = 4;
            }
         }

         if (!this.shipCreated && random.nextInt(10 - genDepth) == 0) {
            EndCityPieces.addHelper(
               pieces,
               EndCityPieces.addPiece(
                  structureTemplateManager, lastPiece, new BlockPos(-8 + random.nextInt(8), nextY, -70 + random.nextInt(10)), "ship", rotation, true
               )
            );
            this.shipCreated = true;
         } else if (!EndCityPieces.recursiveChildren(
            structureTemplateManager, EndCityPieces.HOUSE_TOWER_GENERATOR, genDepth + 1, lastPiece, new BlockPos(-3, nextY + 1, -11), pieces, random
         )) {
            return false;
         }

         lastPiece = EndCityPieces.addHelper(
            pieces,
            EndCityPieces.addPiece(
               structureTemplateManager, lastPiece, new BlockPos(4, nextY, 0), "bridge_end", rotation.getRotated(Rotation.CLOCKWISE_180), true
            )
         );
         lastPiece.setGenDepth(-1);
         return true;
      }
   };
   private static final List<Pair<Rotation, BlockPos>> FAT_TOWER_BRIDGES = List.of(
      Pair.of(Rotation.NONE, new BlockPos(4, -1, 0)),
      Pair.of(Rotation.CLOCKWISE_90, new BlockPos(12, -1, 4)),
      Pair.of(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 8)),
      Pair.of(Rotation.CLOCKWISE_180, new BlockPos(8, -1, 12))
   );
   private static final EndCityPieces.SectionGenerator FAT_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         final StructureTemplateManager structureTemplateManager,
         final int genDepth,
         final EndCityPieces.EndCityPiece parent,
         final BlockPos offset,
         final List<StructurePiece> pieces,
         final RandomSource random
      ) {
         Rotation rotation = parent.placeSettings().getRotation();
         EndCityPieces.EndCityPiece lastPiece = EndCityPieces.addHelper(
            pieces, EndCityPieces.addPiece(structureTemplateManager, parent, new BlockPos(-3, 4, -3), "fat_tower_base", rotation, true)
         );
         lastPiece = EndCityPieces.addHelper(
            pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, 4, 0), "fat_tower_middle", rotation, true)
         );

         for (int i = 0; i < 2 && random.nextInt(3) != 0; i++) {
            lastPiece = EndCityPieces.addHelper(
               pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(0, 8, 0), "fat_tower_middle", rotation, true)
            );

            for (Pair<Rotation, BlockPos> bridge : EndCityPieces.FAT_TOWER_BRIDGES) {
               if (random.nextBoolean()) {
                  EndCityPieces.EndCityPiece bridgeStart = EndCityPieces.addHelper(
                     pieces,
                     EndCityPieces.addPiece(
                        structureTemplateManager, lastPiece, (BlockPos)bridge.getSecond(), "bridge_end", rotation.getRotated((Rotation)bridge.getFirst()), true
                     )
                  );
                  EndCityPieces.recursiveChildren(
                     structureTemplateManager, EndCityPieces.TOWER_BRIDGE_GENERATOR, genDepth + 1, bridgeStart, null, pieces, random
                  );
               }
            }
         }

         lastPiece = EndCityPieces.addHelper(
            pieces, EndCityPieces.addPiece(structureTemplateManager, lastPiece, new BlockPos(-2, 8, -2), "fat_tower_top", rotation, true)
         );
         return true;
      }
   };

   private static EndCityPieces.EndCityPiece addPiece(
      final StructureTemplateManager structureTemplateManager,
      final EndCityPieces.EndCityPiece parent,
      final BlockPos offset,
      final String templateName,
      final Rotation rotation,
      final boolean overwrite
   ) {
      EndCityPieces.EndCityPiece child = new EndCityPieces.EndCityPiece(structureTemplateManager, templateName, parent.templatePosition(), rotation, overwrite);
      BlockPos origin = parent.template().calculateConnectedPosition(parent.placeSettings(), offset, child.placeSettings(), BlockPos.ZERO);
      child.move(origin.getX(), origin.getY(), origin.getZ());
      return child;
   }

   public static void startHouseTower(
      final StructureTemplateManager structureTemplateManager,
      final BlockPos origin,
      final Rotation rotation,
      final List<StructurePiece> pieces,
      final RandomSource random
   ) {
      FAT_TOWER_GENERATOR.init();
      HOUSE_TOWER_GENERATOR.init();
      TOWER_BRIDGE_GENERATOR.init();
      TOWER_GENERATOR.init();
      EndCityPieces.EndCityPiece lastPiece = addHelper(pieces, new EndCityPieces.EndCityPiece(structureTemplateManager, "base_floor", origin, rotation, true));
      lastPiece = addHelper(pieces, addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 0, -1), "second_floor_1", rotation, false));
      lastPiece = addHelper(pieces, addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 4, -1), "third_floor_1", rotation, false));
      lastPiece = addHelper(pieces, addPiece(structureTemplateManager, lastPiece, new BlockPos(-1, 8, -1), "third_roof", rotation, true));
      recursiveChildren(structureTemplateManager, TOWER_GENERATOR, 1, lastPiece, null, pieces, random);
   }

   private static EndCityPieces.EndCityPiece addHelper(final List<StructurePiece> pieces, final EndCityPieces.EndCityPiece piece) {
      pieces.add(piece);
      return piece;
   }

   private static boolean recursiveChildren(
      final StructureTemplateManager structureTemplateManager,
      final EndCityPieces.SectionGenerator generator,
      final int genDepth,
      final EndCityPieces.EndCityPiece parent,
      final BlockPos offset,
      final List<StructurePiece> pieces,
      final RandomSource random
   ) {
      if (genDepth > 8) {
         return false;
      }

      List<StructurePiece> childPieces = new ArrayList<>();
      if (generator.generate(structureTemplateManager, genDepth, parent, offset, childPieces, random)) {
         boolean collision = false;
         int childTag = random.nextInt();

         for (StructurePiece child : childPieces) {
            child.setGenDepth(childTag);
            StructurePiece collisionPiece = StructurePiece.findCollisionPiece(pieces, child.getBoundingBox());
            if (collisionPiece != null && collisionPiece.getGenDepth() != parent.getGenDepth()) {
               collision = true;
               break;
            }
         }

         if (!collision) {
            pieces.addAll(childPieces);
            return true;
         }
      }

      return false;
   }

   public static class EndCityPiece extends TemplateStructurePiece {
      public EndCityPiece(
         final StructureTemplateManager structureTemplateManager,
         final String templateName,
         final BlockPos position,
         final Rotation rotation,
         final boolean overwrite
      ) {
         super(
            StructurePieceType.END_CITY_PIECE,
            0,
            structureTemplateManager,
            makeIdentifier(templateName),
            templateName,
            makeSettings(overwrite, rotation),
            position
         );
      }

      public EndCityPiece(final StructureTemplateManager structureTemplateManager, final CompoundTag tag) {
         super(
            StructurePieceType.END_CITY_PIECE,
            tag,
            structureTemplateManager,
            location -> makeSettings(tag.getBooleanOr("OW", false), tag.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow())
         );
      }

      private static StructurePlaceSettings makeSettings(final boolean overwrite, final Rotation rotation) {
         BlockIgnoreProcessor processor = overwrite ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
         return new StructurePlaceSettings().setIgnoreEntities(true).addProcessor(processor).setRotation(rotation);
      }

      @Override
      protected Identifier makeTemplateLocation() {
         return makeIdentifier(this.templateName);
      }

      private static Identifier makeIdentifier(final String templateName) {
         return Identifier.withDefaultNamespace("end_city/" + templateName);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
         tag.putBoolean("OW", this.placeSettings.getProcessors().get(0) == BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void handleDataMarker(
         final String markerId, final BlockPos position, final ServerLevelAccessor level, final RandomSource random, final BoundingBox chunkBB
      ) {
         if (markerId.startsWith("Chest")) {
            BlockPos chestPosition = position.below();
            if (chunkBB.isInside(chestPosition)) {
               RandomizableContainer.setBlockEntityLootTable(level, random, chestPosition, BuiltInLootTables.END_CITY_TREASURE);
            }
         } else if (chunkBB.isInside(position) && Level.isInSpawnableBounds(position)) {
            if (markerId.startsWith("Sentry")) {
               Shulker sentry = EntityTypes.SHULKER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
               if (sentry != null) {
                  sentry.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                  level.addFreshEntity(sentry);
               }
            } else if (markerId.startsWith("Elytra")) {
               ItemFrame itemFrame = new ItemFrame(level.getLevel(), position, this.placeSettings.getRotation().rotate(Direction.SOUTH));
               itemFrame.setItem(new ItemStack(Items.ELYTRA), false);
               level.addFreshEntity(itemFrame);
            }
         }
      }
   }

   private interface SectionGenerator {
      void init();

      boolean generate(
         StructureTemplateManager structureTemplateManager,
         int genDepth,
         EndCityPieces.EndCityPiece parent,
         BlockPos offset,
         List<StructurePiece> pieces,
         RandomSource random
      );
   }
}
