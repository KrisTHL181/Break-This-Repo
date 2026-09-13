package net.minecraft.client.data.models.model;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.LocalTime;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelUtils {
   public static ItemModel.Unbaked plainModel(Identifier p_456497_) {
      return new BlockModelWrapper.Unbaked(p_456497_, List.of());
   }

   public static ItemModel.Unbaked tintedModel(Identifier p_461071_, ItemTintSource... p_377261_) {
      return new BlockModelWrapper.Unbaked(p_461071_, List.of(p_377261_));
   }

   public static ItemTintSource constantTint(int p_375685_) {
      return new Constant(p_375685_);
   }

   public static ItemModel.Unbaked composite(ItemModel.Unbaked... p_376335_) {
      return new CompositeModel.Unbaked(List.of(p_376335_));
   }

   public static ItemModel.Unbaked specialModel(Identifier p_452475_, SpecialModelRenderer.Unbaked p_378654_) {
      return new SpecialModelWrapper.Unbaked(p_452475_, p_378654_);
   }

   public static RangeSelectItemModel.Entry override(ItemModel.Unbaked p_378360_, float p_376445_) {
      return new RangeSelectItemModel.Entry(p_376445_, p_378360_);
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_377347_, ItemModel.Unbaked p_378776_, RangeSelectItemModel.Entry... p_377688_) {
      return new RangeSelectItemModel.Unbaked(p_377347_, 1.0F, List.of(p_377688_), Optional.of(p_378776_));
   }

   public static ItemModel.Unbaked rangeSelect(
      RangeSelectItemModelProperty p_376332_, float p_378339_, ItemModel.Unbaked p_376666_, RangeSelectItemModel.Entry... p_376006_
   ) {
      return new RangeSelectItemModel.Unbaked(p_376332_, p_378339_, List.of(p_376006_), Optional.of(p_376666_));
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_378056_, ItemModel.Unbaked p_377985_, List<RangeSelectItemModel.Entry> p_378674_) {
      return new RangeSelectItemModel.Unbaked(p_378056_, 1.0F, p_378674_, Optional.of(p_377985_));
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_375806_, List<RangeSelectItemModel.Entry> p_376112_) {
      return new RangeSelectItemModel.Unbaked(p_375806_, 1.0F, p_376112_, Optional.empty());
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_376983_, float p_378096_, List<RangeSelectItemModel.Entry> p_378015_) {
      return new RangeSelectItemModel.Unbaked(p_376983_, p_378096_, p_378015_, Optional.empty());
   }

   public static ItemModel.Unbaked conditional(ConditionalItemModelProperty p_376924_, ItemModel.Unbaked p_375906_, ItemModel.Unbaked p_378638_) {
      return new ConditionalItemModel.Unbaked(p_376924_, p_375906_, p_378638_);
   }

   public static <T> SelectItemModel.SwitchCase<T> when(T p_375850_, ItemModel.Unbaked p_375472_) {
      return new SelectItemModel.SwitchCase<>(List.of(p_375850_), p_375472_);
   }

   public static <T> SelectItemModel.SwitchCase<T> when(List<T> p_376133_, ItemModel.Unbaked p_377675_) {
      return new SelectItemModel.SwitchCase<>(p_376133_, p_377675_);
   }

   @SafeVarargs
   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_377711_, ItemModel.Unbaked p_378198_, SelectItemModel.SwitchCase<T>... p_377556_) {
      return select(p_377711_, p_378198_, List.of(p_377556_));
   }

   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_377040_, ItemModel.Unbaked p_377564_, List<SelectItemModel.SwitchCase<T>> p_378371_) {
      return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(p_377040_, p_378371_), Optional.of(p_377564_));
   }

   @SafeVarargs
   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_376548_, SelectItemModel.SwitchCase<T>... p_376322_) {
      return select(p_376548_, List.of(p_376322_));
   }

   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_375543_, List<SelectItemModel.SwitchCase<T>> p_375408_) {
      return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(p_375543_, p_375408_), Optional.empty());
   }

   public static ConditionalItemModelProperty isUsingItem() {
      return new IsUsingItem();
   }

   public static ConditionalItemModelProperty hasComponent(DataComponentType<?> p_377139_) {
      return new HasComponent(p_377139_, false);
   }

   public static ItemModel.Unbaked inOverworld(ItemModel.Unbaked p_377039_, ItemModel.Unbaked p_377612_) {
      return select(new ContextDimension(), p_377612_, when(Level.OVERWORLD, p_377039_));
   }

   public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(
      Property<T> p_378359_, ItemModel.Unbaked p_376594_, Map<T, ItemModel.Unbaked> p_377415_
   ) {
      List<SelectItemModel.SwitchCase<String>> list = p_377415_.entrySet().stream().sorted(Entry.comparingByKey()).map(p_375487_ -> {
         String s = p_378359_.getName(p_375487_.getKey());
         return new SelectItemModel.SwitchCase<>(List.of(s), p_375487_.getValue());
      }).toList();
      return select(new ItemBlockState(p_378359_.getName()), p_376594_, list);
   }

   public static ItemModel.Unbaked isXmas(ItemModel.Unbaked p_375496_, ItemModel.Unbaked p_375877_) {
      DateTimeFormatter datetimeformatter = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT);
      List<String> list = SpecialDates.CHRISTMAS_RANGE.stream().map(datetimeformatter::format).toList();
      return select(LocalTime.create("MM-dd", "", Optional.empty()), p_375877_, List.of(when(list, p_375496_)));
   }
}
