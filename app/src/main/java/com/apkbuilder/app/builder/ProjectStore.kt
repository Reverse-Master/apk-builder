package com.apkbuilder.app.builder

import android.content.Context
import com.apkbuilder.app.model.ProjectConfig
import java.io.File

/** Persists user projects as JSON files in `filesDir/projects/`. */
class ProjectStore(private val ctx: Context) {
    private val dir: File get() = File(ctx.filesDir, "projects").apply { mkdirs() }

    fun list(): List<Pair<String, ProjectConfig>> = dir.listFiles()
        ?.filter { it.extension == "json" }
        ?.map { it.nameWithoutExtension to ProjectConfig.fromJson(it.readText()) }
        ?: emptyList()

    fun save(name: String, cfg: ProjectConfig) {
        File(dir, "$name.json").writeText(cfg.toJson())
    }

    fun delete(name: String) { File(dir, "$name.json").delete() }
}
