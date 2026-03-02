package com.projectorbit.data.repository

import com.projectorbit.data.db.dao.NebulaFragmentDao
import com.projectorbit.data.db.entity.NebulaFragmentEntity
import com.projectorbit.domain.model.NebulaFragment
import com.projectorbit.domain.repository.NebulaFragmentRepository
import com.projectorbit.util.Vec2
import javax.inject.Inject

class NebulaFragmentRepositoryImpl @Inject constructor(
    private val dao: NebulaFragmentDao
) : NebulaFragmentRepository {

    override suspend fun search(query: String): List<NebulaFragment> =
        dao.search(query).map { it.toDomain() }

    override suspend fun insert(fragment: NebulaFragment) =
        dao.insert(fragment.toEntity())

    override suspend fun updateFade(id: String, fadeFactor: Double) =
        dao.updateFade(id, fadeFactor)

    override suspend fun deleteFullyFaded() =
        dao.deleteFullyFaded()

    private fun NebulaFragmentEntity.toDomain(): NebulaFragment = NebulaFragment(
        id = id,
        originalBodyId = originalBodyId,
        textFragment = textFragment,
        position = Vec2(positionX, positionY),
        createdAt = createdAt,
        fadeFactor = fadeFactor
    )

    private fun NebulaFragment.toEntity(): NebulaFragmentEntity = NebulaFragmentEntity(
        id = id,
        originalBodyId = originalBodyId,
        textFragment = textFragment,
        positionX = position.x,
        positionY = position.y,
        createdAt = createdAt,
        fadeFactor = fadeFactor
    )
}
