package space.taran.arklib.domain.index

import space.taran.arklib.ResourceId

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

interface ResourceIndex {

    val roots: Set<RootIndex>

    val updates: Flow<ResourceUpdates>

    suspend fun updateAll()

    suspend fun allResources(): Set<Resource>

    suspend fun getResource(id: ResourceId): Resource?

    suspend fun getPath(id: ResourceId): Path?

    suspend fun allIds(): Set<ResourceId> =
        allResources().map { it.id }.toSet()
}


class ResourceUpdates(
    val deleted: Map<ResourceId, LostResource>,
    val added: Map<ResourceId, NewResource>
)

data class LostResource(val path: Path, val resource: Resource)

data class NewResource(val path: Path, val resource: Resource)

internal const val LOG_PREFIX: String = "[index]"