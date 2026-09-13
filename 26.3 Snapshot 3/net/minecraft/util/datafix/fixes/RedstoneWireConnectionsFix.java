package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class RedstoneWireConnectionsFix extends DataFix {
   public RedstoneWireConnectionsFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Schema inputSchema = this.getInputSchema();
      return this.fixTypeEverywhereTyped(
         "RedstoneConnectionsFix", inputSchema.getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), this::updateRedstoneConnections)
      );
   }

   private <T> Dynamic<T> updateRedstoneConnections(final Dynamic<T> state) {
      boolean isRedstone = state.get("Name").asString().result().filter("minecraft:redstone_wire"::equals).isPresent();
      return !isRedstone
         ? state
         : state.update(
            "Properties",
            props -> {
               String east = props.get("east").asString("none");
               String west = props.get("west").asString("none");
               String north = props.get("north").asString("none");
               String south = props.get("south").asString("none");
               boolean eastwest = isConnected(east) || isConnected(west);
               boolean northsouth = isConnected(north) || isConnected(south);
               String newEast = !isConnected(east) && !northsouth ? "side" : east;
               String newWest = !isConnected(west) && !northsouth ? "side" : west;
               String newNorth = !isConnected(north) && !eastwest ? "side" : north;
               String newSouth = !isConnected(south) && !eastwest ? "side" : south;
               return props.update("east", value -> value.createString(newEast))
                  .update("west", value -> value.createString(newWest))
                  .update("north", value -> value.createString(newNorth))
                  .update("south", value -> value.createString(newSouth));
            }
         );
   }

   private static boolean isConnected(final String connectionType) {
      return !"none".equals(connectionType);
   }
}
