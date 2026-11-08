/* Copyright 2026 The Forbric Project. Licensed under the Apache License, Version 2.0. */
package net.forbric.kernel.transform;

import net.forbric.kernel.util.ForbricLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * A MinecraftForge-wrapped registry tells fabric-registry-sync's listeners about what it registers, as a plain registry does.
 *
 * <p>fabric-registry-sync fires {@code RegistryEntryAddedCallback} from an injection at the return of
 * {@code MappedRegistry.register}. On the merged game seventeen builtin registries — block, item, menu, … — are
 * MinecraftForge {@code NamespacedWrapper}s, whose {@code register} hands the entry to their {@code ForgeRegistry} and
 * never reaches that method, so no listener ever heard of anything registered into them. fabric-menu-api records the
 * stream codec of every {@code ExtendedMenuType} this way: its main entrypoint walks what the menu registry already
 * holds, then listens for the rest. Fabric Loader's order — by mod id, which Forbric keeps — runs fabric-menu-api-v1
 * before farmersdelight, so Farmer's Delight's cooking pot was registered after the walk, its codec was never recorded,
 * and opening the pot threw "Codec for farmersdelight:cooking_pot is not registered!" on the server every time.
 *
 * <p>The wrapper's {@code register} now hands each entry it registered to {@code KernelWrapperEntryEvents} right before
 * returning, which fires the event fabric-registry-sync gave the registry, with the raw id the wrapper assigned —
 * what Fabric's own injection does on a plain registry. Without fabric-api there is no event, and nothing happens.
 * Only {@code register}: a MinecraftForge mod's {@code RegisterEvent} and Forge's id remap both add to the
 * {@code ForgeRegistry} directly, and a remap is not an addition on Fabric either. {@code -Dforbric.wrapperEntryEvents=off}
 * leaves the method as merged.
 */
public final class WrapperEntryAddedInjector implements ClassTransformer {
	public static final String PROPERTY = "forbric.wrapperEntryEvents";
	static final String WRAPPER = "net.minecraftforge.registries.NamespacedWrapper";
	static final String REGISTER_DESC =
			"(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;";
	static final String HOOK_OWNER = "net/forbric/kernel/boot/KernelWrapperEntryEvents";
	static final String HOOK = "entryAdded";
	static final String HOOK_DESC = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V";

	static boolean enabled() {
		return !"off".equalsIgnoreCase(System.getProperty(PROPERTY, "on"));
	}

	@Override public String name() { return "forbric-wrapper-entry-events"; }

	@Override public AnchorSet anchors() {
		if (!enabled()) return AnchorSet.scanned("the wrapped registries' register explicitly left as merged with -D" + PROPERTY + "=off");
		return AnchorSet.of(new AnchorSet.Anchor(WRAPPER, AnchorSet.Severity.REQUIRED,
				"a Fabric mod's menu, block or item registered after fabric-api's own main entrypoints never reaches a "
						+ "RegistryEntryAddedCallback listener, and fabric-menu-api cannot open such a mod's menu"));
	}

	@Override public byte[] transform(String className, byte[] bytes, TransformContext context) {
		if (!enabled() || bytes == null || bytes.length == 0 || !WRAPPER.equals(className)) return bytes;
		ClassNode node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		MethodNode register = null;
		for (MethodNode method : node.methods) {
			if (method.name.equals("register") && method.desc.equals(REGISTER_DESC) && (method.access & Opcodes.ACC_STATIC) == 0) register = method;
		}
		if (register == null) return declined("NamespacedWrapper declares no register" + REGISTER_DESC, bytes);
		int returns = 0;
		for (AbstractInsnNode insn : register.instructions.toArray()) {
			if (insn instanceof MethodInsnNode call && call.owner.equals(HOOK_OWNER) && call.name.equals(HOOK)) return bytes;
			if (insn.getOpcode() != Opcodes.ARETURN) continue;
			// The holder stays on the stack underneath: register(key, value, info) hands the hook itself, key and value.
			InsnList fire = new InsnList();
			fire.add(new VarInsnNode(Opcodes.ALOAD, 0));
			fire.add(new VarInsnNode(Opcodes.ALOAD, 1));
			fire.add(new VarInsnNode(Opcodes.ALOAD, 2));
			fire.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK, HOOK_DESC, false));
			register.instructions.insertBefore(insn, fire);
			returns++;
		}
		if (returns == 0) return declined("NamespacedWrapper.register never returns a holder", bytes);
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		ForbricLog.info("[Forbric/Registries] a MinecraftForge-wrapped registry's register fires fabric-registry-sync's "
				+ "RegistryEntryAddedCallback at %d return(s), as MappedRegistry.register does — the wrapper never reached it, "
				+ "so fabric-menu-api had no codec for a menu registered after its own main entrypoint", returns);
		return writer.toByteArray();
	}

	private static byte[] declined(String reason, byte[] bytes) {
		ForbricLog.warn("[Forbric/Registries] left the wrapped registries' register as merged: %s — a Fabric mod's menu "
				+ "registered after fabric-menu-api may not open", reason);
		return bytes;
	}
}
