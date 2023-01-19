package space.taran.arklib.domain.meta

import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import java.nio.file.Path

class MetadataStorageRepo(
    private val foldersRepo: FoldersRepo
) {
    private val storageByRoot = mutableMapOf<Path, PlainMetadataStorage>()

    suspend fun provide(rootAndFav: RootAndFav): MetadataStorage {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        val shards = roots.map { root ->
            storageByRoot[root] ?: PlainMetadataStorage(root).also {
                storageByRoot[root] = it
            }
        }

        return AggregatedMetadataStorage(shards)
    }

    suspend fun provide(root: Path): MetadataStorage =
        provide(RootAndFav(root.toString(), null))
}