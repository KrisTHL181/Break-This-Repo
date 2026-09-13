package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveBlockEntityTagFix extends DataFix {
   private final Set<String> blockEntityIdsToDrop;
   private final boolean useLegacyDataStructure;

   public RemoveBlockEntityTagFix(final Schema outputSchema, final Set<String> blockEntityIdsToDrop) {
      this(outputSchema, false, blockEntityIdsToDrop);
   }

   public RemoveBlockEntityTagFix(final Schema outputSchema, final boolean useLegacyDataStructure, final Set<String> blockEntityIdsToDrop) {
      super(outputSchema, true);
      this.blockEntityIdsToDrop = blockEntityIdsToDrop;
      this.useLegacyDataStructure = useLegacyDataStructure;
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<String> blockEntityIdF = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      return this.useLegacyDataStructure
         ? TypeRewriteRule.seq(
            this.createItemBlockEntityRemover(blockEntityIdF, "tag", "BlockEntityTag"),
            new TypeRewriteRule[]{
               this.createFallingBlockBlockEntityRemover(blockEntityIdF),
               this.createStructureBlockEntityRemover(blockEntityIdF),
               this.createUncheckedConverterHack()
            }
         )
         : TypeRewriteRule.seq(
            this.createItemBlockEntityRemover(blockEntityIdF, "components", "minecraft:block_entity_data"),
            new TypeRewriteRule[]{
               this.createFallingBlockBlockEntityRemover(blockEntityIdF),
               this.createStructureBlockEntityRemover(blockEntityIdF),
               this.createChunkBlockEntityRemover(blockEntityIdF),
               this.createUncheckedConverterHack()
            }
         );
   }

   private TypeRewriteRule createItemBlockEntityRemover(
      final OpticFinder<String> blockEntityIdF, final String itemTagOrComponentKey, final String itemBlockEntityDataKey
   ) {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> itemTagF = itemStackType.findField(itemTagOrComponentKey);
      OpticFinder<?> itemBlockEntityF = itemTagF.type().findField(itemBlockEntityDataKey);
      return this.fixTypeEverywhereTyped(
         "ItemRemoveBlockEntityTagFix" + this.getOutputSchema().getVersionKey(),
         itemStackType,
         input -> input.updateTyped(itemTagF, tag -> this.removeBlockEntity(tag, itemBlockEntityF, blockEntityIdF, itemBlockEntityDataKey))
      );
   }

   private TypeRewriteRule createFallingBlockBlockEntityRemover(final OpticFinder<String> blockEntityIdF) {
      Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
      OpticFinder<?> fallingBlockF = DSL.namedChoice(
         "minecraft:falling_block", this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:falling_block")
      );
      OpticFinder<?> fallingBlockEntityTagF = fallingBlockF.type().findField("TileEntityData");
      return this.fixTypeEverywhereTyped(
         "FallingBlockEntityRemoveBlockEntityTagFix" + this.getOutputSchema().getVersionKey(),
         entityType,
         input -> input.updateTyped(fallingBlockF, tag -> this.removeBlockEntity(tag, fallingBlockEntityTagF, blockEntityIdF, "TileEntityData"))
      );
   }

   private TypeRewriteRule createStructureBlockEntityRemover(final OpticFinder<String> blockEntityIdF) {
      Type<?> structureType = this.getInputSchema().getType(References.STRUCTURE);
      OpticFinder<?> blocksF = structureType.findField("blocks");
      OpticFinder<?> blockTypeF = DSL.typeFinder(((ListType)blocksF.type()).getElement());
      OpticFinder<?> blockNbtF = blockTypeF.type().findField("nbt");
      return this.fixTypeEverywhereTyped(
         "StructureRemoveBlockEntityTagFix" + this.getOutputSchema().getVersionKey(),
         structureType,
         input -> input.updateTyped(blocksF, tag -> tag.updateTyped(blockTypeF, blockTag -> this.removeBlockEntity(blockTag, blockNbtF, blockEntityIdF, "nbt")))
      );
   }

   private TypeRewriteRule createChunkBlockEntityRemover(final OpticFinder<String> blockEntityIdF) {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<? extends List<?>> blockEntitiesF = chunkType.findField("block_entities");
      Type<?> blockEntityElementsType = ((ListType)blockEntitiesF.type()).getElement();
      OpticFinder<?> blockEntityTypeFinder = this.getInputSchema().getType(References.BLOCK_ENTITY).finder();
      Type<?> chunkTypeOut = this.getOutputSchema().getType(References.CHUNK);
      Type<List<?>> blockEntitiesTypeOut = chunkType.findField("block_entities").type();
      return this.fixTypeEverywhereTyped(
         "BlockEntityChunkRemover" + this.getOutputSchema().getVersionKey(),
         chunkType,
         chunkTypeOut,
         input -> input.update(blockEntitiesF, blockEntitiesTypeOut, listTag -> {
            ArrayList<Object> keptBlockEntities = new ArrayList<>();

            for (Object untypedBlockEntity : listTag) {
               Typed<?> typedBlockEntity = ExtraDataFixUtils.cast(blockEntityElementsType, untypedBlockEntity, input.getOps());
               Typed<?> typedBlockEntityUnwrapped = typedBlockEntity.getOrCreateTyped(blockEntityTypeFinder);
               String blockEntityId = typedBlockEntityUnwrapped.getOptional(blockEntityIdF).orElse("");
               if (!this.blockEntityIdsToDrop.contains(blockEntityId)) {
                  keptBlockEntities.add(untypedBlockEntity);
               }
            }

            return List.copyOf(keptBlockEntities);
         })
      );
   }

   private TypeRewriteRule createUncheckedConverterHack() {
      return this.convertUnchecked(
         "ItemRemoveBlockEntityTagFix - update block entity type" + this.getOutputSchema().getVersionKey(),
         this.getInputSchema().getType(References.BLOCK_ENTITY),
         this.getOutputSchema().getType(References.BLOCK_ENTITY)
      );
   }

   private Typed<?> removeBlockEntity(
      final Typed<?> tag, final OpticFinder<?> blockEntityF, final OpticFinder<String> blockEntityIdF, final String blockEntityFieldName
   ) {
      Optional<? extends Typed<?>> maybeBlockEntity = tag.getOptionalTyped(blockEntityF);
      if (maybeBlockEntity.isEmpty()) {
         return tag;
      }

      String blockEntityId = maybeBlockEntity.get().getOptional(blockEntityIdF).orElse("");
      return !this.blockEntityIdsToDrop.contains(blockEntityId)
         ? tag
         : Util.writeAndReadTypedOrThrow(tag, tag.getType(), tagData -> tagData.remove(blockEntityFieldName));
   }
}
