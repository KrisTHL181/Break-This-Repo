package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_5 extends NamespacedSchema {
    public V3818_5(final int versionKey, final Schema parent) {
        super(versionKey, parent);
    }

    @Override
    public void registerTypes(
        final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
    ) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(
            true, References.ITEM_STACK, () -> DSL.optionalFields("id", References.ITEM_NAME.in(schema), "components", References.DATA_COMPONENTS.in(schema))
        );
    }
}