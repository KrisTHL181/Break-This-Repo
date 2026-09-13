package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class SkipFields extends CollectToTag {
   private final Deque<FieldTree> stack = new ArrayDeque<>();

   public SkipFields(final FieldSelector... wantedFields) {
      FieldTree rootFrame = FieldTree.createRoot();

      for (FieldSelector wantedField : wantedFields) {
         rootFrame.addEntry(wantedField);
      }

      this.stack.push(rootFrame);
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(final TagType<?> type, final String id) {
      FieldTree currentFrame = this.stack.element();
      if (currentFrame.isSelected(type, id)) {
         return StreamTagVisitor.EntryResult.SKIP;
      }

      if (type == CompoundTag.TYPE) {
         FieldTree newFrame = currentFrame.fieldsToRecurse().get(id);
         if (newFrame != null) {
            this.stack.push(newFrame);
         }
      }

      return super.visitEntry(type, id);
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      if (this.depth() == this.stack.element().depth()) {
         this.stack.pop();
      }

      return super.visitContainerEnd();
   }
}
