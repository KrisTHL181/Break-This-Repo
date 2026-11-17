// HDSL-web — the HDSL domain layer behind a web server, shipped as one jar.
//
// The domain sources under `org.jackhuang.hmcl.dsh/setting/util` are copied
// from the HDSL desktop build and de-JavaFX-ed (see docs/plan.md); their
// package names and copyright headers stay as GPLv3 requires. This build keeps
// only what a headless server needs: no JavaFX, no JFoenix, no nanohttpd.
// JNA is the single native bridge kept: it backs the PTY glue of the web
// terminal (org.jackhuang.hmcl.web.terminal.pty, see docs/web-shell.md).

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.json.JsonSlurper
import org.gradle.api.tasks.bundling.Compression
import java.security.MessageDigest

plugins {
    java
    application
    alias(libs.plugins.shadow)
}

group = "org.jackhuang.hmcl"

// A release takes its version from `-PreleaseVersion=1.2.3`; anything else is
// a development build. HDSL-web is not versioned from git tags the way the
// desktop build is — the jar name just needs to be stable.
version = (findProperty("releaseVersion") as String?)?.takeIf { it.isNotBlank() } ?: "0.1.0-dev"

application {
    mainClass = "org.jackhuang.hmcl.web.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.gson)
    implementation(libs.jetbrains.annotations)
    implementation(libs.errorprone.annotations)
    implementation(libs.kala.compress.zip)
    implementation(libs.kala.compress.tar)
    implementation(libs.kala.compress.ar)
    implementation(libs.kala.encoding.detector)
    implementation(libs.xz)
    implementation(libs.weburl)
    implementation(libs.uuid.tools)

    // Phase 1 server stack: embedded Jetty 12 (EE10 = Jakarta Servlet 6),
    // snakeyaml-engine for server.yaml, Bouncy Castle bcpkix for PEM parsing
    // and self-signed certificate generation. Phase 2 adds the WebSocket
    // modules: jakarta-server for the /ws gateway, core server + client for
    // the /i/* upgrade relay.
    implementation(libs.jetty.server)
    implementation(libs.jetty.ee10.servlet)
    implementation(libs.jetty.ee10.websocket.jakarta.server)
    implementation(libs.jetty.websocket.jetty.server)
    implementation(libs.jetty.websocket.jetty.client)
    implementation(libs.snakeyaml.engine)
    implementation(libs.bcpkix)

    // The PTY glue behind the web terminal (docs/web-shell.md): JNA is the
    // only native bridge in this build.
    implementation(libs.jna)

    // Panel chat (docs/chat.md): SQLite is the store; PDFBox and POI only
    // extract text out of uploaded documents. They never execute content.
    implementation(libs.sqlite.jdbc)
    implementation(libs.pdfbox)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)

    // Jetty speaks slf4j; this binding forwards it to java.util.logging, the
    // same sink HDSL-web's own logging story is built on. Server code itself
    // logs through org.jackhuang.hmcl.util.logging.Logger, never slf4j.
    runtimeOnly(libs.slf4j.jdk14)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // 全套用例跑在一个 fork 的 JVM 里，而 Gradle 的默认上限是 512m——本机峰值在
    // 256–384m 之间（256m 会 OOM），正好卡在默认值边上，于是 CI 上（核数不同、GC 行为
    // 不同）翻过去成了 `java.lang.OutOfMemoryError: Java heap space`。给一个有富余的界：
    // 跑得动就还是几分钟跑完，跑不动时也仍然是「堆不够」而不是随机红。
    maxHeapSize = "2g"
    testLogging {
        events("passed", "failed", "skipped")
        // Failure output carries the full exception, assertion message included: without it a
        // red test in CI logs only its line number, and the reason is unrecoverable.
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
    // A bound on every test, so a machine that cannot run one fails in minutes instead of holding
    // the runner. The name matters — `junit.jupiter.execution.timeout.default` is the key JUnit
    // reads, and `…method.default` is not a key at all, which is a bound nobody reads.
    systemProperty("junit.jupiter.execution.timeout.default", "120s")
    systemProperty("junit.jupiter.execution.timeout.threaddump.enabled", "true")
    // The instance proxy forwards `Host`, which the JDK only allows once this property is set —
    // and it freezes that allow-list at the first `HttpClient` in the JVM. Production sets it at
    // boot (`Main`); the test JVM has to do the same **before any test class** gets a client,
    // otherwise whichever class happens to run first decides whether the panel's own server
    // tests can start. (That is not hypothetical: a test that talks to a local HTTP server was
    // enough to turn every `AccountApiTest` case red.)
    systemProperty("jdk.httpclient.allowRestrictedHeaders", "host")
    // The launcher keeps its state in one per-user home, and a test that writes
    // settings must not write into the one the user is running. Tests get a home
    // of their own inside the build tree, which is also what makes them able to
    // assert on what was persisted.
    systemProperty("hdsl.home", layout.buildDirectory.dir("test-home").get().asFile.absolutePath)
    // The shared home survives an interrupted run (a killed test JVM leaves its
    // instances behind, and the next run's `crud-one` becomes `crud-one-2`).
    // Start every run from an empty one; TestSupport additionally wipes it
    // before every server boot for in-run isolation.
    doFirst {
        delete(layout.buildDirectory.dir("test-home"))
    }

    // A fake `pnpm` ahead of the real one on the test PATH (see
    // src/test/resources/fake-pnpm/pnpm): it stubs out the registry download
    // of a harness install — and only while a test has written the marker
    // file — so pack installs can run end-to-end offline. Everything else is
    // delegated to the real pnpm.
    val fakePnpmBin = layout.buildDirectory.dir("fake-pnpm-bin")
    doFirst {
        val dir = fakePnpmBin.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        // The whole fixture directory, not just the POSIX script: its Windows entry
        // point is `pnpm.cmd`, and on Windows a bare `pnpm` names nothing. Without the
        // `.cmd` the launcher's own executable resolution would skip this directory and
        // find the real pnpm further down PATH — so the tests would reach the network
        // instead of the stub, which is the one thing this fixture exists to prevent.
        project.file("src/test/resources/fake-pnpm").copyRecursively(dir, overwrite = true)
        File(dir, "pnpm").setExecutable(true)
    }
    environment("PATH", fakePnpmBin.get().asFile.absolutePath + File.pathSeparator + (System.getenv("PATH") ?: ""))
    environment("HDSL_FAKE_PNPM_MARKER", layout.buildDirectory.file("fake-pnpm.enabled").get().asFile.absolutePath)
}

// --------------------------------------------------------------- resources ---
// HMCL generates this list at build time. HDSL did the same, and HDSL-web
// keeps the task so the language list can never drift from the .properties
// files actually shipped.
val generateLanguageList by tasks.registering {
    val langDir = layout.projectDirectory.dir("src/main/resources/assets/lang")
    val outputDir = layout.buildDirectory.dir("generated/languageList")

    inputs.dir(langDir)
    outputs.dir(outputDir)

    doLast {
        val tags = sortedSetOf<String>()
        langDir.asFile.listFiles()?.forEach { file ->
            val name = file.name
            if (name.startsWith("I18N") && name.endsWith(".properties")) {
                val tag = name.removePrefix("I18N").removeSuffix(".properties").removePrefix("_")
                tags.add(if (tag.isEmpty()) "en" else tag.replace('_', '-'))
            }
        }
        val target = outputDir.get().dir("assets/lang").file("languages.json").asFile
        target.parentFile.mkdirs()
        target.writeText(tags.joinToString(prefix = "[", postfix = "]", separator = ", ") { "\"" + it + "\"" })
    }
}

sourceSets.main {
    resources.srcDir(generateLanguageList)
}

// ------------------------------------------------------------------ web UI ---
// The panel SPA lives in web/ (Vite + React). Its dist is synced into the
// resources tree before packaging so the fat jar serves it. Without npm the
// tree keeps whatever it currently holds, so Java-only builds still work.
val webDir = layout.projectDirectory.dir("web")

// Which `npm` to run is a platform question, and getting it wrong fails in a way that
// reads like a Gradle bug rather than a missing tool. A Node.js installation on Windows
// ships three files under one name: `npm` (a POSIX shell script), `npm.cmd` (the real
// shim) and `npm.ps1`. The extensionless one is a *file*, so `canExecute()` is true, but
// it is not an executable image — `CreateProcess` answers "not a valid Win32
// application" (error 193) while a bare `npm` is not found at all (error 2). The
// extension is therefore chosen by platform rather than left to the name.
val npmExecutable: String? = run {
    val names = if (File.separatorChar == '\\') listOf("npm.cmd", "npm.exe", "npm") else listOf("npm")
    System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.asSequence()
        ?.filter { it.isNotBlank() }
        ?.flatMap { dir -> names.asSequence().map { File(dir, it) } }
        ?.firstOrNull { it.isFile }
        ?.absolutePath
}
val npmAvailable = npmExecutable != null

val buildWeb by tasks.registering(Exec::class) {
    onlyIf { npmAvailable && webDir.file("package.json").asFile.isFile }
    group = "build"
    description = "Builds the panel SPA (npm run build in web/)."
    workingDir(webDir)
    commandLine(npmExecutable ?: "npm", "run", "build")

    inputs.dir(webDir.dir("src"))
    inputs.files(webDir.file("package.json"), webDir.file("index.html"))
    outputs.dir(webDir.dir("dist"))

    doFirst { logger.lifecycle("Building panel SPA with npm…") }
    doLast { if (!npmAvailable) logger.warn("npm not on PATH; serving the web tree as it stands") }
}

// buildWeb 的 onlyIf 会在没有 npm 时静默跳过；这时若连上一次的 dist 都没有，
// 打出来的 jar 必然缺前端、运行时每个页面都回 "not found"（Windows 裸部署实测踩过：
// 装了 JDK、没装 Node，shadowJar 33 秒"成功"，跑起来只剩光秃秃的 not found）。
// 所以单独设一个无输入输出的哨兵任务——这种任务永不 up-to-date、必定执行
// （doFirst 挂在 Sync 上会被 up-to-date 短路，实测不咬人）。
val checkWebDist by tasks.registering {
    doLast {
        if (!npmAvailable && !webDir.dir("dist").asFile.isDirectory) {
            throw GradleException(
                "web/ has no dist/ and npm is not on PATH: this jar would ship without the web frontend. " +
                    "Install Node.js (which provides npm) and rebuild, or download a release jar instead."
            )
        }
    }
}

val syncWebDist by tasks.registering(Sync::class) {
    dependsOn(buildWeb, checkWebDist)
    from(webDir.dir("dist"))
    into(layout.projectDirectory.dir("src/main/resources/web"))
}

// The legacy surface's server-side markdown renderer (web/scripts/render-markdown.mjs)
// is bundled next to the SPA. Its absence is not fatal the way a missing dist is:
// MarkdownSsr falls back to escaped <pre> when the bundle or Node is gone, so no
// sentinel task guards it.
// KaTeX 的样式与字体跟着 legacy 表面走：IE11 用 woff、现代浏览器用 woff2，
// 都是 jar 里的静态资源，服务端预渲染出来的公式不需要任何脚本。
val syncKatexAssets by tasks.registering(Sync::class) {
    dependsOn(buildWeb)
    val katex = webDir.dir("node_modules/katex/dist")
    from(katex.file("katex.min.css"))
    from(katex.dir("fonts")) { into("fonts") }
    into(layout.projectDirectory.dir("src/main/resources/web-legacy/katex"))
}

val syncWebLegacySsr by tasks.registering(Sync::class) {
    dependsOn(buildWeb)
    from(webDir.dir("dist-ssr"))
    into(layout.projectDirectory.dir("src/main/resources/web-legacy/ssr"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncWebDist, syncWebLegacySsr, syncKatexAssets)
    // 发行物要自带许可证与声明：GPLv3 §4/§5 要求随二进制一并传递许可证并保留声明，
    // HMCL 的附加条款（§7(b)）要求保留其版权与修改声明。依赖的 META-INF 里只有
    // 各自的 licence，没有本项目这一份，所以从仓库根显式带进 jar。
    from(layout.projectDirectory.file("LICENSE"))
    from(layout.projectDirectory.file("NOTICE"))
}

// ------------------------------------------------- first-party plugins -----
//
// Plugins the panel ships itself live under `plugins/<name>/` as ordinary npm
// packages. Each is packed into an npm-layout tarball (a `package/` directory),
// hashed and indexed, then copied into the jar's resources. The panel's
// terminal-plugin install route reads that index, materialises the tarball and
// installs it from the file system, so a first-party plugin never needs the npm
// registry — the panel works offline.
//
// Packing is a Gradle Tar task rather than `npm pack`: the build already
// tolerates a machine without npm (see buildWeb), and a plugin that could only
// be packaged with npm would leave that machine with a jar that has no plugins.
val pluginRoot = layout.projectDirectory.dir("plugins")
val pluginsOutput = layout.buildDirectory.dir("first-party-plugins")

data class FirstPartyPlugin(
    val dirName: String,
    val packageName: String,
    val version: String,
    val fileName: String,
)

fun pluginManifestField(manifest: File, field: String): String? {
    val parsed = JsonSlurper().parse(manifest) as? Map<*, *> ?: return null
    return parsed[field]?.toString()
}

fun jsonQuote(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val firstPartyPlugins: List<FirstPartyPlugin> = run {
    val dirs = pluginRoot.asFile.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
    dirs.mapNotNull { dir ->
        val manifest = File(dir, "package.json")
        if (!manifest.isFile) return@mapNotNull null
        val name = pluginManifestField(manifest, "name") ?: return@mapNotNull null
        val version = pluginManifestField(manifest, "version") ?: return@mapNotNull null
        val stem = name.removePrefix("@").replace('/', '-')
        FirstPartyPlugin(dir.name, name, version, "$stem-$version.tgz")
    }
}

// A plugin's sources import the shared terminal toolkit by a path that is right in
// this repository (`plugins/shared`), while the installed copy carries it inside the
// package (`package/shared`) — the extra `@scope/` directory an npm install adds
// means no single relative path is right in both. Staging copies the package and
// the toolkit, then rewrites just those import specifiers.
val stageFirstPartyPlugin = firstPartyPlugins.map { plugin ->
    val camel = plugin.dirName.split('-', '_').joinToString("") { part -> part.replaceFirstChar(Char::uppercase) }
    tasks.register<Sync>("stageFirstPartyPlugin$camel") {
        group = "build"
        description = "Stages the ${plugin.packageName} first-party plugin for packing."
        val stage = layout.buildDirectory.dir("staged-plugins/${plugin.dirName}")
        into(stage)
        from(pluginRoot.dir(plugin.dirName)) {
            include("package.json", "README.md", "LICENSE", "cordis.patch.yml", "bin/**", "src/**", "lib/**")
        }
        // `plugins/shared/` is the two terminals' common toolbox, not a package.
        // Copy it only into a plugin that actually imports from it: a hand-written
        // client bundle (`lib/client.js`) has no use for a terminal screen model,
        // and shipping it anyway would put reviewed-looking code in a tarball
        // that never runs it.
        val usesShared = pluginRoot.dir(plugin.dirName).asFile
            .walkTopDown()
            .any { it.isFile && it.extension in setOf("js", "mjs") &&
                (it.parentFile?.name != "test") &&
                it.readText().contains("shared/") }
        if (usesShared && pluginRoot.dir("shared").asFile.isDirectory) {
            from(pluginRoot.dir("shared")) {
                include("**/*.js")
                into("shared")
            }
        }
        inputs.dir(pluginRoot.dir(plugin.dirName))
        if (usesShared && pluginRoot.dir("shared").asFile.isDirectory) inputs.dir(pluginRoot.dir("shared"))
        outputs.dir(stage)
        doLast {
            val root = stage.get().asFile
            if (!root.isDirectory) return@doLast
            root.walkTopDown().filter { it.isFile && it.extension == "js" }.forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val depth = relative.count { it == '/' }
                val prefix = "../".repeat(depth)
                val text = file.readText()
                val rewritten = text.replace(
                    Regex("""(from\s+|import\(\s*)(['"])(?:\.\./)+shared/"""),
                ) { match -> match.groupValues[1] + match.groupValues[2] + prefix + "shared/" }
                if (rewritten != text) file.writeText(rewritten)
            }
        }
    }
}

val packFirstPartyPlugin = firstPartyPlugins.mapIndexed { index, plugin ->
    val camel = plugin.dirName.split('-', '_').joinToString("") { part -> part.replaceFirstChar(Char::uppercase) }
    tasks.register<Tar>("packFirstPartyPlugin$camel") {
        group = "build"
        description = "Packs the ${plugin.packageName} first-party plugin into an installable tarball."
        dependsOn(stageFirstPartyPlugin[index])
        archiveFileName.set(plugin.fileName)
        destinationDirectory.set(pluginsOutput)
        compression = Compression.GZIP
        // Reproducible: two builds of one commit produce one tarball.
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        into("package") {
            from(stageFirstPartyPlugin[index])
        }
        outputs.file(pluginsOutput.map { it.file(plugin.fileName) })
    }
}

val packageFirstPartyPlugins by tasks.registering {
    group = "build"
    description = "Packs and indexes the first-party plugins under plugins/."
    dependsOn(packFirstPartyPlugin)
    val outputDir = pluginsOutput.get().asFile
    outputs.files(
        firstPartyPlugins.map { File(outputDir, it.fileName) } + File(outputDir, "index.json")
    )
    doFirst {
        // A renamed or removed plugin leaves its old tarball here; without this it would
        // still be copied into the jar (and served next to the current index).
        val expected = firstPartyPlugins.map { it.fileName }.toSet() + "index.json"
        outputDir.listFiles()
            ?.filter { it.isFile && it.name !in expected }
            ?.forEach { it.delete() }
    }
    doLast {
        val entries = firstPartyPlugins.joinToString(",\n") { plugin ->
            val file = File(outputDir, plugin.fileName)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(file.readBytes())
            val sha = digest.digest().joinToString("") { value -> String.format("%02x", value.toInt() and 0xff) }
            "    {\"name\": ${jsonQuote(plugin.packageName)}, \"version\": ${jsonQuote(plugin.version)}, " +
                "\"file\": ${jsonQuote(plugin.fileName)}, \"sha256\": ${jsonQuote(sha)}}"
        }
        File(outputDir, "index.json").writeText(
            "{\n  \"plugins\": [\n" + entries + "\n  ]\n}\n"
        )
        logger.lifecycle("first-party plugins: " + firstPartyPlugins.joinToString { "${it.packageName}@${it.version}" })
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(packageFirstPartyPlugins)
    from(pluginsOutput) { into("first-party-plugins") }
}

// The plugin's own offline check and tests, run by `gradle check` where Node is
// present. A machine without Node skips them, exactly like the SPA build.
val nodeExecutable: String? = run {
    val names = if (File.separatorChar == '\\') listOf("node.exe", "node") else listOf("node")
    System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.asSequence()
        ?.filter { it.isNotBlank() }
        ?.flatMap { dir -> names.asSequence().map { File(dir, it) } }
        ?.firstOrNull { it.isFile }
        ?.absolutePath
}

firstPartyPlugins.forEach { plugin ->
    val dir = pluginRoot.dir(plugin.dirName)
    val camel = plugin.dirName.split('-', '_').joinToString("") { part -> part.replaceFirstChar(Char::uppercase) }
    if (dir.file("scripts/check.mjs").asFile.isFile) {
        val name = "checkFirstPartyPlugin$camel"
        tasks.register<Exec>(name) {
            group = "verification"
            description = "Runs the offline check of the ${plugin.packageName} first-party plugin."
            onlyIf { nodeExecutable != null }
            workingDir(dir)
            commandLine(nodeExecutable ?: "node", "scripts/check.mjs")
        }
        tasks.named("check") { dependsOn(name) }
    }
    if (dir.dir("test").asFile.isDirectory) {
        val name = "testFirstPartyPlugin$camel"
        tasks.register<Exec>(name) {
            group = "verification"
            description = "Runs the unit tests of the ${plugin.packageName} first-party plugin."
            onlyIf { nodeExecutable != null }
            workingDir(dir)
            commandLine(nodeExecutable ?: "node", "--test")
        }
        tasks.named("check") { dependsOn(name) }
    }
}

// ------------------------------------------------------------- toolchain -----
java {
    // HDSL-web requires JDK 17+. We do not pin a toolchain so the build works
    // with whichever >= 17 JDK the developer has active.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Compile against the Java 17 API even when the build runs on 21+: the jar
    // must start on a 17 JVM, and `--release` is what makes a 21-only API a
    // compile error here rather than an UnsupportedClassVersionError there.
    options.release.set(17)
    // Report every error instead of stopping at the default 100.
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

// The application plugin is used for its `run` task only. The launcher ships
// as a single executable shadow jar, so the distribution tasks are switched
// off rather than taught to agree about the jar this build disables.
tasks.named("distZip") { enabled = false }
tasks.named("distTar") { enabled = false }
tasks.named("startScripts") { enabled = false }
tasks.named("installDist") { enabled = false }

// ------------------------------------------------------------------ fat jar --
// The server ships as a single self-contained jar: `java -jar hdsl-web.jar`.
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("hdsl-web")
    archiveClassifier.set("")
    mergeServiceFiles()
    // Dependency signatures do not survive merging, and a stale one makes the
    // JVM refuse to start.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/INDEX.LIST")
    manifest {
        attributes(
            "Main-Class" to "org.jackhuang.hmcl.web.Main",
            "Implementation-Title" to "HDSL-web",
            "Implementation-Version" to project.version.toString(),
        )
    }
}
