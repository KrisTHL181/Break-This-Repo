package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

public class ChunkRenamesFix extends DataFix {
   public ChunkRenamesFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> levelFinder = chunkType.findField("Level");
      OpticFinder<?> structureFinder = levelFinder.type().findField("Structures");
      Type<?> newChunkType = this.getOutputSchema().getType(References.CHUNK);
      Type<?> newStructuresType = newChunkType.findFieldType("structures");
      return this.fixTypeEverywhereTyped("Chunk Renames; purge Level-tag", chunkType, newChunkType, chunk -> {
         Typed<?> level = chunk.getTyped(levelFinder);
         Typed<?> chunkTyped = appendChunkName(level);
         chunkTyped = chunkTyped.set(DSL.remainderFinder(), mergeRemainders(chunk, (Dynamic)level.get(DSL.remainderFinder())));
         chunkTyped = renameField(chunkTyped, "TileEntities", "block_entities");
         chunkTyped = renameField(chunkTyped, "TileTicks", "block_ticks");
         chunkTyped = renameField(chunkTyped, "Entities", "entities");
         chunkTyped = renameField(chunkTyped, "Sections", "sections");
         chunkTyped = chunkTyped.updateTyped(structureFinder, newStructuresType, structure -> renameField(structure, "Starts", "starts"));
         chunkTyped = renameField(chunkTyped, "Structures", "structures");
         return chunkTyped.update(DSL.remainderFinder(), remainder -> remainder.remove("Level"));
      });
   }

   private static Typed<?> renameField(final Typed<?> input, final String oldName, final String newName) {
      return renameFieldHelper(input, oldName, newName, input.getType().findFieldType(oldName)).update(DSL.remainderFinder(), tag -> tag.remove(oldName));
   }

   private static <A> Typed<?> renameFieldHelper(final Typed<?> input, final String oldName, final String newName, final Type<A> fieldType) {
      Type<Either<A, Unit>> oldType = DSL.optional(DSL.field(oldName, fieldType));
      Type<Either<A, Unit>> newType = DSL.optional(DSL.field(newName, fieldType));
      return input.update(oldType.finder(), newType, Function.identity());
   }

   private static <A> Typed<Pair<String, A>> appendChunkName(final Typed<A> input) {
      return new Typed(DSL.named("chunk", input.getType()), input.getOps(), Pair.of("chunk", input.getValue()));
   }

   private static <T> Dynamic<T> mergeRemainders(final Typed<?> chunk, final Dynamic<T> levelRemainder) {
      DynamicOps<T> ops = levelRemainder.getOps();
      Dynamic<T> chunkRemainder = ((Dynamic)chunk.get(DSL.remainderFinder())).convert(ops);
      DataResult<T> toMap = ops.getMap(levelRemainder.getValue()).flatMap(map -> ops.mergeToMap(chunkRemainder.getValue(), map));
      return toMap.result().map(v -> new Dynamic(ops, v)).orElse(levelRemainder);
   }
}
