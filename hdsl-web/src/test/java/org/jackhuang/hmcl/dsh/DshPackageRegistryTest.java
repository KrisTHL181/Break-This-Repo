package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// What an npm answer means.
///
/// An exporter asks the registry whether a plugin it installed from a file is published, because a
/// plugin that is published does not have to be carried: the pack names `name@version` and the install
/// fetches it. The two wrong answers are expensive in opposite directions — reading a silence as
/// "published" writes a pack that does not install, and reading it as "absent" carries files nobody
/// needed — so what a non-zero exit means is pinned here rather than guessed at the call site.
class DshPackageRegistryTest {

    @Test
    void aVersionTheRegistryPrintsIsPublished() {
        assertEquals(DshPackageRegistry.Availability.PUBLISHED,
                DshPackageRegistry.availabilityOf(0, "\"1.2.3\""));
    }

    @Test
    void aRegistryThatSaysThereIsNoSuchThingIsAnAnswer() {
        assertEquals(DshPackageRegistry.Availability.ABSENT,
                DshPackageRegistry.availabilityOf(1, """
                        npm error code E404
                        npm error 404 Not Found - GET https://registry.npmjs.org/dsh-secret - Not found
                        npm error 404  'dsh-secret@1.0.0' is not in this registry.
                        """));
    }

    @Test
    void aRegistryThatCannotBeAskedHasNotSaidAnything() {
        // A missing network is not the registry saying the package does not exist: a pack made
        // offline must carry the files rather than name a package nobody may be able to fetch.
        assertEquals(DshPackageRegistry.Availability.UNKNOWN,
                DshPackageRegistry.availabilityOf(1, """
                        npm error code ENOTFOUND
                        npm error network request to https://registry.npmjs.org/x failed
                        """));
        assertEquals(DshPackageRegistry.Availability.UNKNOWN,
                DshPackageRegistry.availabilityOf(1, ""));
        assertEquals(DshPackageRegistry.Availability.UNKNOWN,
                DshPackageRegistry.availabilityOf(1, null));
    }
}
