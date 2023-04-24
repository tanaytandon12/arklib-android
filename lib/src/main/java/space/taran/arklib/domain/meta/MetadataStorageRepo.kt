package space.taran.arklib.domain.meta

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class MetadataStorageRepo(private val scope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, RootMetadataStorage>()

    fun provide(index: ResourceIndex): MetadataStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }

            AggregatedMetadataStorage(shards, scope)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(root: RootIndex): RootMetadataStorage =
        storageByRoot[root.path] ?: RootMetadataStorage(
            scope, root
        ).also {
            storageByRoot[root.path] = it
        }
}
