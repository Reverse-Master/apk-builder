package com.apkbuilder.app.builder

import android.content.Context
import com.android.apksig.ApkSigner
import com.apkbuilder.app.model.AbiTarget
import com.apkbuilder.app.model.ProjectConfig
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Builds the final APK on-device by patching a pre-compiled template APK with
 * the user's [ProjectConfig] and re-signing it.
 *
 * Pipeline (all on-device, no server, no DEX compilation needed):
 *
 *   1. Copy `assets/templates/<id>/template.apk` to a working file.
 *   2. Re-pack the zip, replacing user-customisable entries:
 *        - assets/config.json   : user config (read by template at runtime)
 *        - assets/branding/...  : icon, splash, audio
 *        - resources.arsc       : left untouched (template handles theming via config)
 *      and stripping native libraries that don't match the chosen [AbiTarget].
 *   3. Sign the result with [KeystoreManager]'s key (v1 + v2 schemes so it
 *      installs on every Android version from 5.0 upwards).
 *
 * The output file is written into the app's private files dir under
 * `builds/` and a [File] handle returned to the caller.
 */
class ApkAssembler(private val ctx: Context) {

    fun build(
        config: ProjectConfig,
        abi: AbiTarget,
        progress: (String) -> Unit = {}
    ): File {
        progress("Preparing workspace...")
        val buildsDir = File(ctx.filesDir, "builds").apply { mkdirs() }
        val safeName = config.appName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val staging = File(ctx.cacheDir, "stage-${System.currentTimeMillis()}.apk")
        val unsigned = File(ctx.cacheDir, "unsigned-${System.currentTimeMillis()}.apk")
        val output = File(buildsDir, "${safeName}-${abi.name.lowercase()}.apk")

        progress("Loading template '${config.templateId}'...")
        copyTemplate(config.templateId, staging)

        progress("Injecting your customisations...")
        patchApk(staging, unsigned, config, abi)

        progress("Signing APK...")
        sign(unsigned, output)

        staging.delete(); unsigned.delete()
        progress("Done: ${output.name}")
        return output
    }

    private fun copyTemplate(templateId: String, dest: File) {
        ctx.assets.open("templates/$templateId/template.apk").use { input ->
            FileOutputStream(dest).use { input.copyTo(it) }
        }
    }

    /**
     * Streams entries from [src] into [dst], rewriting customisable assets
     * and dropping native libraries for ABIs the user did not select.
     */
    private fun patchApk(src: File, dst: File, cfg: ProjectConfig, abi: AbiTarget) {
        ZipFile(src).use { zf ->
            ZipOutputStream(FileOutputStream(dst)).use { zos ->
                val keepAbis = abi.abis.toSet()

                zf.entries().asSequence().forEach { entry ->
                    val name = entry.name
                    // Skip the existing signature - we'll re-sign at the end.
                    if (name.startsWith("META-INF/") &&
                        (name.endsWith(".SF") || name.endsWith(".RSA") ||
                         name.endsWith(".DSA") || name.endsWith(".EC") ||
                         name == "META-INF/MANIFEST.MF")) return@forEach

                    // Filter native libs by ABI.
                    if (name.startsWith("lib/")) {
                        val abiDir = name.removePrefix("lib/").substringBefore('/')
                        if (abiDir !in keepAbis) return@forEach
                    }

                    // Skip files we're about to overwrite.
                    if (name == "assets/config.json") return@forEach
                    if (cfg.iconPath != null && name == "assets/branding/icon.png") return@forEach
                    if (cfg.splashImagePath != null && name == "assets/branding/splash.png") return@forEach
                    if (cfg.audioPath != null && name == "assets/branding/audio.mp3") return@forEach

                    copyEntry(zf, entry, zos)
                }

                // Inject the user's configuration as JSON.
                writeEntry(zos, "assets/config.json", cfg.toJson().toByteArray())

                // Optional binary assets the template will pick up at runtime.
                cfg.iconPath?.let { writeEntry(zos, "assets/branding/icon.png", File(it).readBytes()) }
                cfg.splashImagePath?.let { writeEntry(zos, "assets/branding/splash.png", File(it).readBytes()) }
                cfg.audioPath?.let { writeEntry(zos, "assets/branding/audio.mp3", File(it).readBytes()) }
            }
        }
    }

    /**
     * Copies a zip entry preserving its compression method. STORED entries
     * MUST have crc, size, and compressedSize set before [ZipOutputStream]
     * accepts them — otherwise we get "STORED entry missing size, compressed
     * size, or crc-32" at runtime.
     */
    private fun copyEntry(zf: ZipFile, entry: ZipEntry, zos: ZipOutputStream) {
        val data = zf.getInputStream(entry).use { it.readBytes() }
        val out = ZipEntry(entry.name)
        out.method = entry.method
        if (entry.method == ZipEntry.STORED) {
            val crc = CRC32().apply { update(data) }
            out.size = data.size.toLong()
            out.compressedSize = data.size.toLong()
            out.crc = crc.value
        }
        zos.putNextEntry(out)
        zos.write(data)
        zos.closeEntry()
    }

    private fun writeEntry(zos: ZipOutputStream, name: String, bytes: ByteArray) {
        // Default DEFLATED — no need to set crc/size manually.
        val out = ZipEntry(name).apply { method = ZipEntry.DEFLATED }
        zos.putNextEntry(out)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun sign(input: File, output: File) {
        val key = KeystoreManager.loadOrCreate(ctx)
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "apkbuilder", key.privateKey, listOf(key.certificate)
        ).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false) // v3 needs API 28+; v1+v2 is broadest compatible.
            .build()
            .sign()
    }
}
