package com.mojang.realmsclient.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@OnlyIn(Dist.CLIENT)
public @interface Exclude {
}