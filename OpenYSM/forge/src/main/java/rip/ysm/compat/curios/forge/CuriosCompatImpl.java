package rip.ysm.compat.curios.forge;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.List;
import com.elfmcys.yesstevemodel.client.compat.curios.CuriosCompat;

public final class CuriosCompatImpl {

    private CuriosCompatImpl() {
    }

    public static boolean isLoaded() {
        return CuriosCompat.isLoaded();
    }

    public static boolean hasItemInSlot(LivingEntity livingEntity, String str, ReferenceOpenHashSet<Item> set) {
        return CuriosCompat.hasItemInSlot(livingEntity, str, set);
    }

    public static boolean hasTaggedItemInSlot(LivingEntity livingEntity, String str, List<TagKey<Item>> list) {
        return CuriosCompat.hasTaggedItemInSlot(livingEntity, str, list);
    }

    public static boolean hasNoTaggedItemInSlot(LivingEntity entity, String str, List<TagKey<Item>> list) {
        return CuriosCompat.hasNoTaggedItemInSlot(entity, str, list);
    }

    public static void registerCuriosItems(ContextBinding binding) {
        CuriosCompat.registerCuriosItems(binding);
    }
}
