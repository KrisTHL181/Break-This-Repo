package com.mojang.blocklist;

import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public interface BlockListSupplier {
   @Nullable
   Predicate<String> createBlockList();
}
