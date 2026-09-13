package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityUUIDFix extends AbstractUUIDFix {
    public BlockEntityUUIDFix(final Schema outputSchema) {
        super(outputSchema, References.BLOCK_ENTITY);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("BlockEntityUUIDFix", this.getInputSchema().getType(this.typeReference), input -> {
            input = this.updateNamedChoice(input, "minecraft:conduit", this::updateConduit);
            return this.updateNamedChoice(input, "minecraft:skull", this::updateSkull);
        });
    }

    private Dynamic<?> updateSkull(final Dynamic<?> tag) {
        return tag.get("Owner")
            .get()
            .map(ownerTag -> replaceUUIDString((Dynamic<?>)ownerTag, "Id", "Id").orElse((Dynamic<?>)ownerTag))
            .<Dynamic<?>>map(ownerTag -> tag.remove("Owner").set("SkullOwner", (Dynamic<?>)ownerTag))
            .result()
            .orElse(tag);
    }

    private Dynamic<?> updateConduit(final Dynamic<?> tag) {
        return replaceUUIDMLTag(tag, "target_uuid", "Target").orElse(tag);
    }
}
