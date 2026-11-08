import { getMajorMinecraftVersion, getMinorMinecraftVersion, getPatchMinecraftVersion } from "./minecraft";

export interface JavaVersion {
	compatibility: string,
	mixin: string,
	release: number
}

const JAVA_8 : JavaVersion = {
	compatibility: "1_8",
	mixin: "JAVA_8",
	release: 8
}

const JAVA_16 : JavaVersion = {
	compatibility: "16",
	mixin: "JAVA_16",
	release: 16
}

const JAVA_17 : JavaVersion = {
	compatibility: "17",
	mixin: "JAVA_17",
	release: 17
}

const JAVA_21 : JavaVersion = {
	compatibility: "21",
	mixin: "JAVA_21",
	release: 21
}

const JAVA_25 : JavaVersion = {
	compatibility: "25",
	mixin: "JAVA_25",
	release: 25
}

export function getJavaVersion(minecraftVersion: string): JavaVersion {
	const major = getMajorMinecraftVersion(minecraftVersion);
	const minor = getMinorMinecraftVersion(minecraftVersion);

	if (major >= 26) {
		return JAVA_25;
	} else if (minor <= 16) {
		// Minecraft 1.16 and below use Java 8
		return JAVA_8;
	} else if (minor == 17) {
		// Minecraft 1.17 uses Java 16
		return JAVA_16;
	} else if (minor <= 19) {
		// Minecraft 1.18 and 1.19 use all Java 17
		return JAVA_17;
	} else if (minor == 20) {
		const patch = getPatchMinecraftVersion(minecraftVersion);

		if (patch <= 4) {
			// Minecraft 1.20.0 -> 1.20.4 use Java 17
			return JAVA_17;
		}
	}

	// Minecraft 1.20.5 and above use Java 21
	return JAVA_21;
}

const JAVA_PACKAGE_REGEX = /^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)*$/;
const JAVA_RESERVED_WORDS = `
	abstract continue for new switch assert default goto package synchronized
	boolean do if private this break double implements protected throw byte else
	import public throws case enum instanceof return transient catch extends int
	short try char final interface static void class finally long strictfp
	volatile const float native super while _ true false null
`.trim().split(/\s+/);
const RESERVED_PACKAGE_PREFIXES = ["net.minecraft.", "com.mojang.", "net.fabricmc.", "java."];

export function computePackageNameErrors(packageName: string): string[] {
	let errorList : string[] = [];

	if (!JAVA_PACKAGE_REGEX.test(packageName.toLowerCase())) {
		errorList.push("Package name is not a valid Java package name!");
	}

	const reservedWordsFound = packageName.split('.').filter(c => JAVA_RESERVED_WORDS.includes(c));
	if (reservedWordsFound.length != 0) {
		errorList.push(`Package name contains illegal component: '${reservedWordsFound[0]}'`);
	}

	for (let prefix of RESERVED_PACKAGE_PREFIXES) {
		if (packageName.toLowerCase().startsWith(prefix)) {
			errorList.push(`Package name starts with '${prefix}', which is reserved!`);
		} else if (packageName.toLowerCase() + "." == prefix) {
			errorList.push(`Package name is '${prefix}', which is reserved!`);
		}
	}

	return errorList;
}

const NUMERALS = [
	'zero',
	'one',
	'two',
	'three',
	'four',
	'five',
	'six',
	'seven',
	'eight',
	'nine'
] as const;

function convertNumericPrefix(s: string): string[] {
	const match = s.match(/^\d+/);
	if(!(match && match[0]))
		return [s];
	const numericPrefix = match[0];
	const rest = s.substring(numericPrefix.length);
	return [...numericPrefix.split('').map(x => NUMERALS[parseInt(x)]), rest];
}

function toPascalCase(parts: string[]): string {
	return parts
		.filter(Boolean)
		.map(s => s[0].toUpperCase() + s.slice(1))
		.join('');
}

export function formatClassname(projectName: string): string {
	const s = toPascalCase(projectName.split(/\b/).map(s => s.replaceAll(/\W/g, '')));
	return toPascalCase(convertNumericPrefix(s));
}

export function formatPackageName(packageName: string): string {
	return packageName
		.toLowerCase()
		.replaceAll(/[\s/]+/g, '.')
		.replaceAll(/[^a-z0-9_\.]/g, "");
}

export function generatePackageName(packageName: string): string | undefined {
	const formatted = convertNumericPrefix(formatPackageName(packageName))
		.join('');

	if(computePackageNameErrors(formatted).length !== 0)
		return;

	return formatted;
}

