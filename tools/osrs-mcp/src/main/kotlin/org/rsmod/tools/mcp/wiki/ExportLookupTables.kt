package org.rsmod.tools.mcp.wiki

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

fun main(args: Array<String>) {
    val root = args.getOrNull(0)?.takeIf(String::isNotBlank) ?: System.getenv("RSPS_ROOT")
    val outDir =
        args.getOrNull(1)?.takeIf(String::isNotBlank)
            ?: Path.of(root ?: ".").resolve(".data").resolve("lookups").toString()
    val output = Path.of(outDir).toAbsolutePath().normalize()
    output.createDirectories()

    val gamevals = GameValTool.load(root)
    val cache = CacheTool()

    writeGamevals(output.resolve("gamevals_npcs.csv"), gamevals.allEntries("npc"))
    writeGamevals(output.resolve("gamevals_items.csv"), gamevals.allEntries("obj"))
    writeCommands(output.resolve("commands.csv"), Path.of(root ?: ".").toAbsolutePath().normalize())
    runCatching {
        writeCache(output.resolve("cache_server_npcs.csv"), cache.all(CacheKind.SERVER, CacheSearchType.Npc).matches)
        writeCache(output.resolve("cache_server_items.csv"), cache.all(CacheKind.SERVER, CacheSearchType.Item).matches)
    }.onFailure { error ->
        Files.writeString(
            output.resolve("cache_export_error.txt"),
            "Decoded cache export failed: ${error.message.orEmpty()}\n" +
                "Gameval and command exports were still written.\n",
        )
    }

    println("Exported lookup CSVs to $output")
}

private fun writeGamevals(path: Path, entries: List<GameValTool.GameValEntry>) {
    val lines = sequence {
        yield("table,full_key,key,id,source")
        entries.sortedWith(compareBy<GameValTool.GameValEntry> { it.table }.thenBy { it.fullKey })
            .forEach { yield(csv(it.table, it.fullKey, it.key, it.id, it.source)) }
    }
    Files.write(path, lines.asIterable())
}

private fun writeCache(path: Path, hits: List<CacheTool.SearchHit>) {
    val lines = sequence {
        yield("type,id,name,summary")
        hits.forEach { yield(csv(it.type, it.id, it.name, it.summary)) }
    }
    Files.write(path, lines.asIterable())
}

private fun writeCommands(path: Path, root: Path) {
    val commandDir =
        root.resolve("content").resolve("other").resolve("commands").resolve("src")
            .resolve("main").resolve("kotlin")
    val commandRegex = Regex("""onCommand\("([^"]+)",\s*"([^"]+)"""")
    val usageRegex = Regex("""invalidArgs\s*=\s*"([^"]+)"""")
    val rows = mutableListOf<List<String>>()
    if (Files.isDirectory(commandDir)) {
        Files.walk(commandDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.forEach { file ->
                val lines = Files.readAllLines(file)
                for ((index, line) in lines.withIndex()) {
                    val match = commandRegex.find(line) ?: continue
                    val block =
                        lines.drop(index).takeWhileIndexed { blockIndex, text ->
                            blockIndex == 0 || !text.contains("onCommand(")
                        }
                    val usage = block.firstNotNullOfOrNull { usageRegex.find(it)?.groupValues?.get(1) }.orEmpty()
                    rows += listOf(match.groupValues[1], match.groupValues[2], usage, root.relativize(file).toString())
                }
            }
        }
    }
    val lines = sequence {
        yield("command,description,usage,file")
        rows.sortedBy { it[0] }.forEach { yield(csv(*it.toTypedArray())) }
    }
    Files.write(path, lines.asIterable())
}

private fun csv(vararg values: Any?): String = values.joinToString(",") { value ->
    val text = value?.toString().orEmpty()
    '"' + text.replace("\"", "\"\"") + '"'
}

private inline fun <T> Iterable<T>.takeWhileIndexed(predicate: (Int, T) -> Boolean): List<T> {
    val list = mutableListOf<T>()
    for ((index, item) in withIndex()) {
        if (!predicate(index, item)) {
            break
        }
        list += item
    }
    return list
}
