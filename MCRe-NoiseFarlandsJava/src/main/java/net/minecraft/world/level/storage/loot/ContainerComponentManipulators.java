package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;

public interface ContainerComponentManipulators {
    ContainerComponentManipulator<ItemContainerContents> CONTAINER = new ContainerComponentManipulator<ItemContainerContents>() {
        @Override
        public DataComponentType<ItemContainerContents> type() {
            return DataComponents.CONTAINER;
        }

        public Stream<ItemStack> getContents(final ItemContainerContents component) {
            return component.allItemsCopyStream();
        }

        public ItemContainerContents empty() {
            return ItemContainerContents.EMPTY;
        }

        public ItemContainerContents setContents(final ItemContainerContents component, final Stream<ItemStack> newContents) {
            return ItemContainerContents.fromItems(newContents.toList());
        }
    };
    ContainerComponentManipulator<BundleContents> BUNDLE_CONTENTS = new ContainerComponentManipulator<BundleContents>() {
        @Override
        public DataComponentType<BundleContents> type() {
            return DataComponents.BUNDLE_CONTENTS;
        }

        public BundleContents empty() {
            return BundleContents.EMPTY;
        }

        public Stream<ItemStack> getContents(final BundleContents component) {
            return component.itemCopyStream();
        }

        public BundleContents setContents(final BundleContents component, final Stream<ItemStack> newContents) {
            BundleContents.Mutable builder = new BundleContents.Mutable(component).clearItems();
            newContents.forEach(builder::tryInsert);
            return builder.toImmutable();
        }
    };
    ContainerComponentManipulator<ChargedProjectiles> CHARGED_PROJECTILES = new ContainerComponentManipulator<ChargedProjectiles>() {
        @Override
        public DataComponentType<ChargedProjectiles> type() {
            return DataComponents.CHARGED_PROJECTILES;
        }

        public ChargedProjectiles empty() {
            return ChargedProjectiles.EMPTY;
        }

        public Stream<ItemStack> getContents(final ChargedProjectiles component) {
            return component.itemCopies().stream();
        }

        public ChargedProjectiles setContents(final ChargedProjectiles component, final Stream<ItemStack> newContents) {
            return ChargedProjectiles.ofNonEmpty(newContents.filter(s -> !s.isEmpty()).toList());
        }
    };
    Map<DataComponentType<?>, ContainerComponentManipulator<?>> ALL_MANIPULATORS = Stream.of(CONTAINER, BUNDLE_CONTENTS, CHARGED_PROJECTILES)
        .collect(Collectors.toMap(ContainerComponentManipulator::type, e -> (ContainerComponentManipulator<?>)e));
    Codec<ContainerComponentManipulator<?>> CODEC = BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec().comapFlatMap(type -> {
        ContainerComponentManipulator<?> manipulator = ALL_MANIPULATORS.get(type);
        return manipulator != null ? DataResult.success(manipulator) : DataResult.error(() -> "No items in component");
    }, ContainerComponentManipulator::type);
}