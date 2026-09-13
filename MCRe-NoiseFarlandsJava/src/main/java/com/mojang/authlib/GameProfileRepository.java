package com.mojang.authlib;

import com.mojang.authlib.yggdrasil.response.NameAndId;

import java.util.Optional;

public interface GameProfileRepository {
    /**
     * Find UUID and canonical name for muliple accounts
     * Note 1: returned names might differ in casing from requested one
     * Note 2: might be slower than {@link #findProfileByName(String)}
     */
    void findProfilesByNames(String[] names, ProfileLookupCallback callback);

    /**
     * Find UUID and canonical name for a single account.
     * Note: returned name might differ in casing from requested one
     */
    Optional<NameAndId> findProfileByName(String name);
}
