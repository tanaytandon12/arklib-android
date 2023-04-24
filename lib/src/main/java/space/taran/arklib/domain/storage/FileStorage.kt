package space.taran.arklib.domain.storage

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arklib.ResourceId
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.attribute.FileTime

abstract class FileStorage<V>(
    private val scope: CoroutineScope,
    private val storageFile: Path,
    monoid: Monoid<V>,
    logLabel: String) : Storage<V>(monoid, logLabel) {

    /* The file will be filled with a table,
     * one mapping entry per line of the table.
     * We only need to read and write values into strings for this. */
    protected abstract fun valueToString(value: V): String
    protected abstract fun valueFromString(raw: String): V

    private var timestamp: FileTime = FileTime.fromMillis(0L)

    final override fun erase() {
        scope.launch(Dispatchers.IO) {
            Files.delete(storageFile)
        }
    }

    final override fun exists(): Boolean {
        val result = Files.exists(storageFile)

        val not = if (result) "" else " not"
        Log.d(LOG_PREFIX, "folder $storageFile does$not exist")
        return result
    }

    // returns all values from new file,
    // we don't have more granular timestamping
    override fun readFromDisk(handle: (Map<ResourceId, V>) -> Unit) {
        val newTimestamp = Files.getLastModifiedTime(storageFile)
        Log.d(LOG_PREFIX, "timestamp of storage file $storageFile is $timestamp")

        if (timestamp >= newTimestamp) {
            return
        }
        Log.d(LOG_PREFIX, "the file was modified externally, merging")

        val lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8)

        verifyVersion(lines.removeAt(0))

        val valueById = lines.associate {
            val parts = it.split(KEY_VALUE_SEPARATOR)
            val id = ResourceId.fromString(parts[0])
            val value = valueFromString(parts[1])

            check(value)

            id to value
        }

        if (valueById.isEmpty()) {
            Log.w(LOG_PREFIX, "Storage is empty")
        }

        Log.d(LOG_PREFIX, "${valueById.size} entries have been read")

        handle(valueById)
        timestamp = newTimestamp
    }

    override fun writeToDisk(valueById: Map<ResourceId, V>) {
        val lines = mutableListOf<String>()
        lines.add("$STORAGE_VERSION_PREFIX$STORAGE_VERSION")

        lines.addAll(
            valueById.map { (id, value: V) ->
                if (check(value)) {
                    throw IllegalStateException("Storage is excessive")
                }
                "$id$KEY_VALUE_SEPARATOR${valueToString(value)}"
            }
        )

        Files.write(storageFile, lines, StandardCharsets.UTF_8)

        val newTimestamp = Files.getLastModifiedTime(storageFile)
        if (newTimestamp == timestamp) {
            throw IllegalStateException("Timestamp didn't update")
        }
        timestamp = newTimestamp

        Log.d(LOG_PREFIX, "${valueById.size} entries has been written")
    }

    companion object {
        private const val STORAGE_VERSION = 2
        private const val STORAGE_VERSION_PREFIX = "version "

        const val KEY_VALUE_SEPARATOR = ':'

        private fun verifyVersion(header: String) {
            if (!header.startsWith(STORAGE_VERSION_PREFIX)) {
                throw IllegalStateException("Unknown storage version")
            }
            val version = header.removePrefix(STORAGE_VERSION_PREFIX).toInt()

            if (version > STORAGE_VERSION) {
                throw IllegalStateException("Storage format is newer than the app")
            }
            if (version < STORAGE_VERSION) {
                throw IllegalStateException("Storage format is older than the app")
            }
        }
    }
}

private const val LOG_PREFIX: String = "[file-storage]"