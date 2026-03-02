package com.projectorbit.domain.repository

import com.projectorbit.domain.model.NebulaFragment

interface NebulaFragmentRepository {
    suspend fun search(query: String): List<NebulaFragment>
    suspend fun insert(fragment: NebulaFragment)
    suspend fun updateFade(id: String, fadeFactor: Double)
    suspend fun deleteFullyFaded()
}
