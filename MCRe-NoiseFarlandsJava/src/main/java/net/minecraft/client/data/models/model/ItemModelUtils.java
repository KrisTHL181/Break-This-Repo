package net.minecraft.client.data.models.model;

import com.mojang.math.Transformation;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
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
    public static ItemModel.Unbaked plainModel(final Identifier id) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of());
    }

    public static ItemModel.Unbaked plainModel(final Identifier id, final Transformation transformation) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.of(transformation), List.of());
    }

    public static ItemModel.Unbaked tintedModel(final Identifier id, final ItemTintSource... tints) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of(tints));
    }

    public static ItemTintSource constantTint(final int color) {
        return new Constant(color);
    }

    public static ItemModel.Unbaked composite(final ItemModel.Unbaked... models) {
        return new CompositeModel.Unbaked(List.of(models), Optional.empty());
    }

    public static ItemModel.Unbaked specialModel(final Identifier base, final SpecialModelRenderer.Unbaked<?> model) {
        return specialModel(base, Optional.empty(), model);
    }

    public static ItemModel.Unbaked specialModel(final Identifier base, final Transformation transformation, final SpecialModelRenderer.Unbaked<?> model) {
        return specialModel(base, Optional.of(transformation), model);
    }

    public static ItemModel.Unbaked specialModel(
        final Identifier base, final Optional<Transformation> transformation, final SpecialModelRenderer.Unbaked<?> model
    ) {
        return new SpecialModelWrapper.Unbaked(base, transformation, model);
    }

    public static RangeSelectItemModel.Entry override(final ItemModel.Unbaked model, final float value) {
        return new RangeSelectItemModel.Entry(value, model);
    }

    public static ItemModel.Unbaked rangeSelect(
        final RangeSelectItemModelProperty property, final ItemModel.Unbaked fallback, final RangeSelectItemModel.Entry... entries
    ) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, 1.0F, List.of(entries), Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(
        final RangeSelectItemModelProperty property, final float scale, final ItemModel.Unbaked fallback, final RangeSelectItemModel.Entry... entries
    ) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, scale, List.of(entries), Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(
        final RangeSelectItemModelProperty property, final ItemModel.Unbaked fallback, final List<RangeSelectItemModel.Entry> entries
    ) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, 1.0F, entries, Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(final RangeSelectItemModelProperty property, final List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, 1.0F, entries, Optional.empty());
    }

    public static ItemModel.Unbaked rangeSelect(final RangeSelectItemModelProperty property, final float scale, final List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, scale, entries, Optional.empty());
    }

    public static ItemModel.Unbaked conditional(final ConditionalItemModelProperty property, final ItemModel.Unbaked onTrue, final ItemModel.Unbaked onFalse) {
        return conditional(Optional.empty(), property, onTrue, onFalse);
    }

    public static ItemModel.Unbaked conditional(
        final Transformation transformation, final ConditionalItemModelProperty property, final ItemModel.Unbaked onTrue, final ItemModel.Unbaked onFalse
    ) {
        return conditional(Optional.of(transformation), property, onTrue, onFalse);
    }

    public static ItemModel.Unbaked conditional(
        final Optional<Transformation> transformation,
        final ConditionalItemModelProperty property,
        final ItemModel.Unbaked onTrue,
        final ItemModel.Unbaked onFalse
    ) {
        return new ConditionalItemModel.Unbaked(transformation, property, onTrue, onFalse);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(final T value, final ItemModel.Unbaked model) {
        return new SelectItemModel.SwitchCase<>(List.of(value), model);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(final List<T> values, final ItemModel.Unbaked model) {
        return new SelectItemModel.SwitchCase<>(values, model);
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(
        final SelectItemModelProperty<T> property, final ItemModel.Unbaked fallback, final SelectItemModel.SwitchCase<T>... cases
    ) {
        return select(property, fallback, List.of(cases));
    }

    public static <T> ItemModel.Unbaked select(
        final SelectItemModelProperty<T> property, final ItemModel.Unbaked fallback, final List<SelectItemModel.SwitchCase<T>> cases
    ) {
        return new SelectItemModel.Unbaked(Optional.empty(), new SelectItemModel.UnbakedSwitch<>(property, cases), Optional.of(fallback));
    }

    public static <T> ItemModel.Unbaked select(
        final Transformation transformation,
        final SelectItemModelProperty<T> property,
        final ItemModel.Unbaked fallback,
        final List<SelectItemModel.SwitchCase<T>> cases
    ) {
        return new SelectItemModel.Unbaked(Optional.of(transformation), new SelectItemModel.UnbakedSwitch<>(property, cases), Optional.of(fallback));
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(final SelectItemModelProperty<T> property, final SelectItemModel.SwitchCase<T>... cases) {
        return select(property, List.of(cases));
    }

    public static <T> ItemModel.Unbaked select(final SelectItemModelProperty<T> property, final List<SelectItemModel.SwitchCase<T>> cases) {
        return new SelectItemModel.Unbaked(Optional.empty(), new SelectItemModel.UnbakedSwitch<>(property, cases), Optional.empty());
    }

    public static ConditionalItemModelProperty isUsingItem() {
        return new IsUsingItem();
    }

    public static ConditionalItemModelProperty hasComponent(final DataComponentType<?> component) {
        return new HasComponent(component, false);
    }

    public static ItemModel.Unbaked inOverworld(final ItemModel.Unbaked ifTrue, final ItemModel.Unbaked ifFalse) {
        return select(new ContextDimension(), ifFalse, when(Level.OVERWORLD, ifTrue));
    }

    private static <T extends Comparable<T>> List<SelectItemModel.SwitchCase<String>> createBlockPropertySwitchCases(
        final Property<T> property, final Map<T, ItemModel.Unbaked> cases
    ) {
        return cases.entrySet().stream().sorted(Entry.comparingByKey()).map(e -> {
            String valueName = property.getName(e.getKey());
            return new SelectItemModel.SwitchCase<>(List.of(valueName), e.getValue());
        }).toList();
    }

    public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(
        final Property<T> property, final ItemModel.Unbaked fallback, final Map<T, ItemModel.Unbaked> cases
    ) {
        return select(new ItemBlockState(property.getName()), fallback, createBlockPropertySwitchCases(property, cases));
    }

    public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(
        final Transformation transformation, final Property<T> property, final ItemModel.Unbaked fallback, final Map<T, ItemModel.Unbaked> cases
    ) {
        return select(transformation, new ItemBlockState(property.getName()), fallback, createBlockPropertySwitchCases(property, cases));
    }

    public static ItemModel.Unbaked isXmas(final ItemModel.Unbaked onTrue, final ItemModel.Unbaked onFalse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT);
        List<String> days = SpecialDates.CHRISTMAS_RANGE.stream().map(formatter::format).toList();
        return select(LocalTime.create("MM-dd", "", Optional.empty()), onFalse, List.of(when(days, onTrue)));
    }
}