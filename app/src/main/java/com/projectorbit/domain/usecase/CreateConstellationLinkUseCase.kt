package com.projectorbit.domain.usecase

import com.projectorbit.domain.model.Link
import com.projectorbit.domain.model.LinkType
import com.projectorbit.domain.repository.LinkRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a constellation (backlink) between two celestial bodies in different systems.
 * Constellation links are rendered as glowing lines at galaxy zoom (plan Section 7.1).
 */
class CreateConstellationLinkUseCase @Inject constructor(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(sourceId: String, targetId: String): Link {
        val link = Link(
            id = UUID.randomUUID().toString(),
            sourceId = sourceId,
            targetId = targetId,
            linkType = LinkType.CONSTELLATION,
            createdAt = System.currentTimeMillis()
        )
        linkRepository.upsert(link)
        return link
    }

    suspend fun remove(sourceId: String, targetId: String) {
        linkRepository.deleteByEndpoints(sourceId, targetId, LinkType.CONSTELLATION)
    }
}
