package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.slf4j.Logger;

public record PiecesContainer(List<StructurePiece> pieces) {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier JIGSAW_RENAME = Identifier.withDefaultNamespace("jigsaw");
   private static final Map<Identifier, Identifier> RENAMES = ImmutableMap.builder()
      .put(Identifier.withDefaultNamespace("nvi"), JIGSAW_RENAME)
      .put(Identifier.withDefaultNamespace("pcp"), JIGSAW_RENAME)
      .put(Identifier.withDefaultNamespace("bastionremnant"), JIGSAW_RENAME)
      .put(Identifier.withDefaultNamespace("runtime"), JIGSAW_RENAME)
      .build();

   public PiecesContainer(final List<StructurePiece> pieces) {
      this.pieces = List.copyOf(pieces);
   }

   public boolean isEmpty() {
      return this.pieces.isEmpty();
   }

   public boolean isInsidePiece(BlockPos p_192752_) {
      for (StructurePiece structurepiece : this.pieces) {
         if (structurepiece.getBoundingBox().isInside(p_192752_)) {
            return true;
         }
      }

      return false;
   }

   public Tag save(StructurePieceSerializationContext p_192750_) {
      ListTag listtag = new ListTag();

      for (StructurePiece structurepiece : this.pieces) {
         listtag.add(structurepiece.createTag(p_192750_));
      }

      return listtag;
   }

   public static PiecesContainer load(ListTag p_192754_, StructurePieceSerializationContext p_192755_) {
      List<StructurePiece> list = Lists.newArrayList();

      for (int i = 0; i < p_192754_.size(); i++) {
         CompoundTag compoundtag = p_192754_.getCompoundOrEmpty(i);
         String s = compoundtag.getStringOr("id", "").toLowerCase(Locale.ROOT);
         Identifier identifier = Identifier.parse(s);
         Identifier identifier1 = RENAMES.getOrDefault(identifier, identifier);
         StructurePieceType structurepiecetype = BuiltInRegistries.STRUCTURE_PIECE.getValue(identifier1);
         if (structurepiecetype == null) {
            LOGGER.error("Unknown structure piece id: {}", identifier1);
         } else {
            try {
               StructurePiece structurepiece = structurepiecetype.load(p_192755_, compoundtag);
               list.add(structurepiece);
            } catch (Exception exception) {
               LOGGER.error("Exception loading structure piece with id {}", identifier1, exception);
            }
         }
      }

      return new PiecesContainer(list);
   }

   public BoundingBox calculateBoundingBox() {
      return StructurePiece.createBoundingBox(this.pieces.stream());
   }
}
