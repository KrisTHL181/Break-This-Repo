package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ModifyContainerContents extends LootItemConditionalFunction {
   public static final MapCodec<ModifyContainerContents> CODEC = RecordCodecBuilder.mapCodec(
      p_334502_ -> commonFields(p_334502_)
         .and(
            p_334502_.group(
               ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(p_328799_ -> p_328799_.component),
               LootItemFunctions.ROOT_CODEC.fieldOf("modifier").forGetter(p_332200_ -> p_332200_.modifier)
            )
         )
         .apply(p_334502_, ModifyContainerContents::new)
   );
   private final ContainerComponentManipulator<?> component;
   private final LootItemFunction modifier;

   private ModifyContainerContents(List<LootItemCondition> p_329722_, ContainerComponentManipulator<?> p_330185_, LootItemFunction p_330905_) {
      super(p_329722_);
      this.component = p_330185_;
      this.modifier = p_330905_;
   }

   @Override
   public LootItemFunctionType<ModifyContainerContents> getType() {
      return LootItemFunctions.MODIFY_CONTENTS;
   }

   @Override
   public ItemStack run(ItemStack p_329760_, LootContext p_328367_) {
      if (p_329760_.isEmpty()) {
         return p_329760_;
      }

      this.component.modifyItems(p_329760_, p_332662_ -> this.modifier.apply(p_332662_, p_328367_));
      return p_329760_;
   }

   @Override
   public void validate(ValidationContext p_332171_) {
      super.validate(p_332171_);
      this.modifier.validate(p_332171_.forChild(new ProblemReporter.FieldPathElement("modifier")));
   }
}
