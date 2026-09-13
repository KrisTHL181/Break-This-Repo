package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TrappedChestBlockEntityFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SIZE = 4096;
   private static final short SIZE_BITS = 12;

   public TrappedChestBlockEntityFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getOutputSchema().getType(References.CHUNK);
      Type<?> levelType = chunkType.findFieldType("Level");
      if (!(levelType.findFieldType("TileEntities") instanceof ListType<?> tileEntityListType)) {
         throw new IllegalStateException("Tile entity type is not a list type.");
      } else {
         OpticFinder<? extends List<?>> tileEntitiesF = DSL.fieldFinder("TileEntities", tileEntityListType);
         Type<?> chunkType1 = this.getInputSchema().getType(References.CHUNK);
         OpticFinder<?> levelFinder = chunkType1.findField("Level");
         OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
         Type<?> sectionsType = sectionsFinder.type();
         if (!(sectionsType instanceof ListType)) {
            throw new IllegalStateException("Expecting sections to be a list.");
         }

         Type<?> sectionType = ((ListType)sectionsType).getElement();
         OpticFinder<?> sectionFinder = DSL.typeFinder(sectionType);
         return TypeRewriteRule.seq(
            new AddNewChoices(this.getOutputSchema(), "AddTrappedChestFix", References.BLOCK_ENTITY).makeRule(),
            this.fixTypeEverywhereTyped(
               "Trapped Chest fix",
               chunkType1,
               chunk -> chunk.updateTyped(
                  levelFinder,
                  level -> {
                     Optional<? extends Typed<?>> sections = level.getOptionalTyped(sectionsFinder);
                     if (sections.isEmpty()) {
                        return level;
                     }

                     List<? extends Typed<?>> sectionList = sections.get().getAllTyped(sectionFinder);
                     IntSet chestLocations = new IntOpenHashSet();

                     for (Typed<?> section : sectionList) {
                        TrappedChestBlockEntityFix.TrappedChestSection trappedChestSection = new TrappedChestBlockEntityFix.TrappedChestSection(
                           section, this.getInputSchema()
                        );
                        if (!trappedChestSection.isSkippable()) {
                           for (int i = 0; i < 4096; i++) {
                              int block = trappedChestSection.getBlock(i);
                              if (trappedChestSection.isTrappedChest(block)) {
                                 chestLocations.add(trappedChestSection.getIndex() << 12 | i);
                              }
                           }
                        }
                     }

                     Dynamic<?> levelTag = (Dynamic<?>)level.get(DSL.remainderFinder());
                     int chunkX = levelTag.get("xPos").asInt(0);
                     int chunkZ = levelTag.get("zPos").asInt(0);
                     TaggedChoiceType<String> tileEntityChoiceType = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
                     return level.updateTyped(
                        tileEntitiesF,
                        tileEntities -> tileEntities.updateTyped(
                           tileEntityChoiceType.finder(),
                           tileEntity -> {
                              Dynamic<?> tag = (Dynamic<?>)tileEntity.getOrCreate(DSL.remainderFinder());
                              int x = tag.get("x").asInt(0) - (chunkX << 4);
                              int y = tag.get("y").asInt(0);
                              int z = tag.get("z").asInt(0) - (chunkZ << 4);
                              return chestLocations.contains(LeavesFix.getIndex(x, y, z))
                                 ? tileEntity.update(tileEntityChoiceType.finder(), stringPair -> stringPair.mapFirst(s -> {
                                    if (!Objects.equals(s, "minecraft:chest")) {
                                       LOGGER.warn("Block Entity was expected to be a chest");
                                    }

                                    return "minecraft:trapped_chest";
                                 }))
                                 : tileEntity;
                           }
                        )
                     );
                  }
               )
            )
         );
      }
   }

   public static final class TrappedChestSection extends LeavesFix.Section {
      private @Nullable IntSet chestIds;

      public TrappedChestSection(final Typed<?> section, final Schema inputSchema) {
         super(section, inputSchema);
      }

      @Override
      protected boolean skippable() {
         this.chestIds = new IntOpenHashSet();

         for (int i = 0; i < this.palette.size(); i++) {
            Dynamic<?> paletteTag = this.palette.get(i);
            String blockName = paletteTag.get("Name").asString("");
            if (Objects.equals(blockName, "minecraft:trapped_chest")) {
               this.chestIds.add(i);
            }
         }

         return this.chestIds.isEmpty();
      }

      public boolean isTrappedChest(final int block) {
         return this.chestIds.contains(block);
      }
   }
}
