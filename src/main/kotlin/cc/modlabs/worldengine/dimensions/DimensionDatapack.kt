package cc.modlabs.worldengine.dimensions

import cc.modlabs.worldengine.world.WorldOperations
import cc.modlabs.worldengine.world.isValidWorldName
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.papermc.paper.datapack.DatapackRegistrar
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import net.kyori.adventure.text.Component
import rufus.lzstring4java.LZString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.CompletableFuture

object DimensionDatapack {
    private const val PACK_DIRECTORY = "dimension-datapack"
    private const val SHARE_HOST = "misode.github.io"
    private const val SNIPPET_HOST = "snippets.misode.workers.dev"
    private const val MINECRAFT_VERSION = "26.2"
    private const val MAX_RESPONSE_BYTES = 32 * 1024
    private const val MAX_JSON_CHARS = 64 * 1024
    private val shareIdPattern = Regex("[A-Za-z0-9_-]{6,64}")
    private val worldKeyPattern = Regex("[a-z0-9._-]{1,64}")
    private val resourceLocationPattern = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun discover(context: BootstrapContext, registrar: DatapackRegistrar) {
        val pack = packDirectory(context.dataDirectory)
        if (!Files.isRegularFile(pack.resolve("pack.mcmeta"))) return
        registrar.discoverPack(pack, "dimensions") { configurer ->
            configurer.title(Component.text("WorldEngine dimensions"))
                .autoEnableOnServerStart(true)
        }
        context.logger.info("Discovered WorldEngine dimension datapack")
    }

    fun importVoidDimension(dataDirectory: Path, worldName: String, shareLink: String): CompletableFuture<Path> {
        val key: String
        val shareId: String
        try {
            key = requireWorldKey(worldName)
            shareId = extractShareId(shareLink)
        } catch (failure: IllegalArgumentException) {
            return CompletableFuture.failedFuture(failure)
        }
        val endpoint = URI.create("https://$SNIPPET_HOST/$shareId")
        val request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("User-Agent", "WorldEngine/$MINECRAFT_VERSION")
            .GET()
            .build()

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply { response ->
            response.body().use { body ->
                require(response.statusCode() == 200) { "Misode returned HTTP ${response.statusCode()}" }
                val bytes = body.readNBytes(MAX_RESPONSE_BYTES + 1)
                require(bytes.size <= MAX_RESPONSE_BYTES) { "Misode response is too large" }
                val dimensionType = decodeSnippet(String(bytes, StandardCharsets.UTF_8), shareId)
                stage(dataDirectory, key, dimensionType)
            }
        }
    }

    internal fun extractShareId(link: String): String {
        val uri = runCatching { URI(link.trim()) }
            .getOrElse { throw IllegalArgumentException("Invalid Misode share link") }
        require(uri.scheme.equals("https", ignoreCase = true)) { "Misode share link must use HTTPS" }
        require(uri.host.equals(SHARE_HOST, ignoreCase = true)) { "Only $SHARE_HOST share links are accepted" }
        require(uri.port == -1 || uri.port == 443) { "Unexpected port in Misode share link" }
        require(uri.userInfo == null && uri.fragment == null) { "Invalid Misode share link" }
        require(uri.path.trimEnd('/') == "/dimension-type") { "Link must point to the Misode Dimension Type creator" }
        val shareValues = uri.rawQuery.orEmpty().split('&')
            .filter { it.startsWith("share=") }
            .map { it.removePrefix("share=") }
        require(shareValues.size == 1 && shareIdPattern.matches(shareValues.single())) {
            "Misode share link has no valid share id"
        }
        return shareValues.single()
    }

    internal fun decodeSnippet(responseBody: String, expectedId: String): JsonObject {
        val snippet = JsonParser.parseString(responseBody).asJsonObject
        require(snippet["id"]?.asString == expectedId) { "Misode returned the wrong share id" }
        require(snippet["type"]?.asString == "dimension_type") { "Misode share is not a dimension type" }
        require(snippet["version"]?.asString == MINECRAFT_VERSION) {
            "Misode share must target Minecraft $MINECRAFT_VERSION"
        }
        val compressed = snippet["data"]?.asString ?: throw IllegalArgumentException("Misode share has no data")
        require(compressed.length <= MAX_RESPONSE_BYTES) { "Compressed dimension type is too large" }
        val decoded = LZString.decompressFromBase64(compressed)
            ?: throw IllegalArgumentException("Could not decode Misode dimension type")
        require(decoded.length <= MAX_JSON_CHARS) { "Dimension type is too large" }
        val dimensionType = runCatching { JsonParser.parseString(decoded).asJsonObject }
            .getOrElse { throw IllegalArgumentException("Misode dimension type is not valid JSON", it) }
        return inlineCustomSounds(dimensionType)
    }

    private fun inlineCustomSounds(dimensionType: JsonObject): JsonObject {
        val copy = dimensionType.deepCopy()

        fun visit(element: JsonElement) {
            when {
                element.isJsonArray -> element.asJsonArray.forEach(::visit)
                element.isJsonObject -> element.asJsonObject.entrySet().toList().forEach { (name, value) ->
                    if (name == "sound" && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        val soundId = value.asString
                        require(resourceLocationPattern.matches(soundId)) { "Invalid sound id '$soundId'" }
                        if (!soundId.startsWith("minecraft:")) {
                            element.asJsonObject.add(name, JsonObject().apply { addProperty("sound_id", soundId) })
                        }
                    } else {
                        visit(value)
                    }
                }
            }
        }

        copy["attributes"]?.let(::visit)
        return copy
    }

    @Synchronized
    internal fun stage(dataDirectory: Path, worldKey: String, dimensionType: JsonObject): Path {
        require(worldKeyPattern.matches(worldKey)) { "Invalid dimension key" }
        val pack = packDirectory(dataDirectory)
        val typeFile = pack.resolve("data/worldengine/dimension_type/$worldKey.json")
        val dimensionFile = pack.resolve("data/worldengine/dimension/$worldKey.json")
        require(!Files.exists(dimensionFile)) { "Dimension '$worldKey' is already staged" }

        writeIfMissing(pack.resolve("pack.mcmeta"), PACK_META)
        writeAtomically(typeFile, gson.toJson(dimensionType), replace = true)
        writeAtomically(dimensionFile, gson.toJson(voidDimension(worldKey)), replace = false)
        return dimensionFile
    }

    internal fun hasDimension(dataDirectory: Path, worldKey: String): Boolean {
        if (!worldKeyPattern.matches(worldKey)) return false
        return Files.isRegularFile(packDirectory(dataDirectory).resolve("data/worldengine/dimension/$worldKey.json"))
    }

    private fun requireWorldKey(worldName: String): String {
        require(isValidWorldName(worldName)) { "Invalid world name '$worldName'" }
        return WorldOperations.sanitizeWorldKey(worldName)
    }

    private fun packDirectory(dataDirectory: Path): Path = dataDirectory.resolve(PACK_DIRECTORY)

    private fun voidDimension(worldKey: String): JsonObject = JsonObject().apply {
        addProperty("type", "worldengine:$worldKey")
        add("generator", JsonObject().apply {
            addProperty("type", "minecraft:flat")
            add("settings", JsonObject().apply {
                addProperty("biome", "minecraft:the_void")
                addProperty("features", true)
                addProperty("lakes", false)
                add("layers", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("block", "minecraft:air")
                        addProperty("height", 1)
                    })
                })
                add("structure_overrides", JsonArray())
            })
        })
    }

    private fun writeIfMissing(path: Path, content: String) {
        if (Files.exists(path)) return
        writeAtomically(path, content, replace = false)
    }

    private fun writeAtomically(path: Path, content: String, replace: Boolean) {
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, ".${path.fileName}.", ".tmp")
        try {
            Files.writeString(temporary, content + System.lineSeparator(), StandardCharsets.UTF_8)
            val options = if (replace) {
                arrayOf(StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } else {
                arrayOf(StandardCopyOption.ATOMIC_MOVE)
            }
            try {
                Files.move(temporary, path, *options)
            } catch (_: AtomicMoveNotSupportedException) {
                if (replace) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
                } else {
                    Files.move(temporary, path)
                }
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private val PACK_META = """
        {
          "pack": {
            "description": "WorldEngine managed dimensions for Minecraft Java 26.2",
            "pack_format": 107,
            "min_format": [107, 1],
            "max_format": [107, 1]
          }
        }
    """.trimIndent()
}
