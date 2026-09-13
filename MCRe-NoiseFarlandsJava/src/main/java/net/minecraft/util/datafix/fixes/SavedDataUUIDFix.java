package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;

public class SavedDataUUIDFix extends AbstractUUIDFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public SavedDataUUIDFix(final Schema outputSchema) {
        super(outputSchema, References.SAVED_DATA_RAIDS);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "SavedDataUUIDFix",
            this.getInputSchema().getType(this.typeReference),
            input -> input.update(
                DSL.remainderFinder(),
                container -> container.update(
                    "data",
                    tag -> tag.update(
                        "Raids",
                        raids -> raids.createList(
                            raids.asStream()
                                .map(
                                    raid -> raid.update(
                                        "HeroesOfTheVillage",
                                        heros -> heros.createList(
                                            heros.asStream().map(hero -> createUUIDFromLongs((Dynamic<?>)hero, "UUIDMost", "UUIDLeast").orElseGet(() -> {
                                                LOGGER.warn("HeroesOfTheVillage contained invalid UUIDs.");
                                                return hero;
                                            }))
                                        )
                                    )
                                )
                        )
                    )
                )
            )
        );
    }
}