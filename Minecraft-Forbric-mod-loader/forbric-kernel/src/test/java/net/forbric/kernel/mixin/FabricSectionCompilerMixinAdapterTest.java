package net.forbric.kernel.mixin;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.forbric.kernel.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class FabricSectionCompilerMixinAdapterTest {
	private ClassNode mixin() throws Exception {
		return StagedFabricMixinFixture.mixin("fabric-renderer-api-v1", FabricSectionCompilerMixinAdapter.MIXIN);
	}

	private ClassNode target(boolean vanilla) throws Exception {
		var path = vanilla ? TestFixtures.vanillaJar()
				: java.nio.file.Path.of(System.getProperty("forbric.stagedRoot"), "merged-base/patched-mc-merged-26.2.jar");
		org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(path));
		try (var zip = new java.util.zip.ZipFile(path.toFile())) {
			ClassNode node = new ClassNode();
			new org.objectweb.asm.ClassReader(zip.getInputStream(zip.getEntry(FabricSectionCompilerMixinAdapter.TARGET + ".class"))).accept(node, 0);
			return node;
		}
	}

	@Test void movesBothHalvesWithTheirCapturedLayerMapAndSharedRendererIntact() throws Exception {
		ClassNode mixin = mixin();
		ClassNode target = target(false);
		byte[] before = StagedFabricMixinFixture.bytes(target);
		assertEquals(2, FabricSectionCompilerMixinAdapter.adapt(mixin, n -> target));
		MethodNode setup = StagedFabricMixinFixture.method(mixin, "beforeLoopCompile");
		MethodNode draw = StagedFabricMixinFixture.method(mixin, "tesselateBlockProxy");
		for (MethodNode handler : List.of(setup, draw))
			assertEquals(List.of("compile" + FabricSectionCompilerMixinAdapter.LIVE), MixinFit.value(MixinFit.injectorOf(handler), "method"));
		assertEquals(List.of("startedLayers"), MixinFit.value(setup.invisibleParameterAnnotations[6].getFirst(), "name"));
		assertEquals("altBlockRenderer", MixinFit.value(setup.invisibleParameterAnnotations[7].getFirst(), "value"));
		assertEquals("altQuadOutput", MixinFit.value(setup.invisibleParameterAnnotations[8].getFirst(), "value"));
		assertEquals(List.of(0, 1, 2, 3, 4, 6, 7, 8, 9), java.util.stream.StreamSupport.stream(setup.instructions.spliterator(), false)
				.filter(i -> i instanceof VarInsnNode).map(i -> ((VarInsnNode)i).var).toList());
		assertNull(MixinFit.injectorOf(StagedFabricMixinFixture.method(mixin, "beforeLoopCompile$forbricOriginal")));
		new Analyzer<>(new BasicVerifier()).analyze(mixin.name, setup);
		assertArrayEquals(before, StagedFabricMixinFixture.bytes(target), "NeoForge's extra geometry and native compile body stay intact");
		assertEquals(0, FabricSectionCompilerMixinAdapter.adapt(mixin, n -> target));
	}

	@Test void aMissingDrawAnchorRefusesTheEntireSharedStatePair() throws Exception {
		ClassNode mixin = mixin();
		byte[] before = StagedFabricMixinFixture.bytes(mixin);
		ClassNode target = target(false);
		for (MethodNode m : target.methods) for (var i : m.instructions)
			if (i instanceof MethodInsnNode c && c.name.equals("tesselateBlock")) c.name = "changedRenderer";
		assertEquals(0, FabricSectionCompilerMixinAdapter.adapt(mixin, n -> target));
		assertArrayEquals(before, StagedFabricMixinFixture.bytes(mixin));
	}

	@Test void changedShareContractIsNotPartiallyRebound() throws Exception {
		ClassNode mixin = mixin();
		MethodNode draw = StagedFabricMixinFixture.method(mixin, "tesselateBlockProxy");
		draw.invisibleParameterAnnotations[10].getFirst().values.set(1, "anotherRenderer");
		byte[] before = StagedFabricMixinFixture.bytes(mixin);
		ClassNode target = target(false);
		assertEquals(0, FabricSectionCompilerMixinAdapter.adapt(mixin, n -> target));
		assertArrayEquals(before, StagedFabricMixinFixture.bytes(mixin));
	}

	@Test void vanillaAndTheDisabledPassLeaveTheOriginalContractAlone() throws Exception {
		ClassNode vanilla = target(true);
		assertEquals(0, FabricSectionCompilerMixinAdapter.adapt(mixin(), n -> vanilla));
		String old = System.setProperty(FabricSectionCompilerMixinAdapter.PROPERTY, "off");
		try {
			ClassNode merged = target(false);
			assertEquals(0, FabricSectionCompilerMixinAdapter.adapt(mixin(), n -> merged));
		} finally {
			if (old == null) System.clearProperty(FabricSectionCompilerMixinAdapter.PROPERTY);
			else System.setProperty(FabricSectionCompilerMixinAdapter.PROPERTY, old);
		}
	}
}
