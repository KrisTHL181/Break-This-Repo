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

   public boolean isInsidePiece(final BlockPos startPos) {
      for (StructurePiece piece : this.pieces) {
         if (piece.getBoundingBox().isInside(startPos)) {
            return true;
         }
      }

      return false;
   }

   public Tag save(final StructurePieceSerializationContext context) {
      ListTag childrenTags = new ListTag();

      for (StructurePiece piece : this.pieces) {
         childrenTags.add(piece.createTag(context));
      }

      return childrenTags;
   }

   public static PiecesContainer load(final ListTag children, final StructurePieceSerializationContext context) {
      List<StructurePiece> pieces = Lists.newArrayList();

      for (int i = 0; i < children.size(); i++) {
         CompoundTag pieceTag = children.getCompoundOrEmpty(i);
         String oldId = pieceTag.getStringOr("id", "").toLowerCase(Locale.ROOT);
         Identifier oldPieceKey = Identifier.parse(oldId);
         Identifier pieceId = RENAMES.getOrDefault(oldPieceKey, oldPieceKey);
         StructurePieceType pieceType = BuiltInRegistries.STRUCTURE_PIECE.getValue(pieceId);
         if (pieceType == null) {
            LOGGER.error("Unknown structure piece id: {}", pieceId);
         } else {
            try {
               StructurePiece piece = pieceType.load(context, pieceTag);
               pieces.add(piece);
            } catch (Exception e) {
               LOGGER.error("Exception loading structure piece with id {}", pieceId, e);
            }
         }
      }

      return new PiecesContainer(pieces);
   }

   public BoundingBox calculateBoundingBox() {
      return StructurePiece.createBoundingBox(this.pieces.stream());
   }
}
