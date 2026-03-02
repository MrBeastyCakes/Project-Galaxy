package com.projectorbit.domain.usecase

import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.model.NebulaFragment
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.NebulaFragmentRepository
import com.projectorbit.domain.repository.NoteContentRepository
import javax.inject.Inject

/**
 * Full-text search across all note content and nebula fragments.
 * Powers the Telescope view (plan Section 7.4).
 *
 * Returns matching body IDs (for live bodies) and nebula fragments (for deleted bodies).
 */
class SearchNotesUseCase @Inject constructor(
    private val noteContentRepository: NoteContentRepository,
    private val bodyRepository: CelestialBodyRepository,
    private val nebulaFragmentRepository: NebulaFragmentRepository
) {
    data class SearchResult(
        val matchingBodyIds: List<String>,
        val matchingBodies: List<CelestialBody>,
        val nebulaFragments: List<NebulaFragment>
    )

    suspend operator fun invoke(query: String): SearchResult {
        if (query.isBlank()) return SearchResult(emptyList(), emptyList(), emptyList())

        // FTS search on note content (returns body IDs)
        val matchingIds = noteContentRepository.searchPlainText(query)

        // Resolve to domain models (filter out deleted)
        val matchingBodies = matchingIds.mapNotNull { id ->
            bodyRepository.getById(id)?.takeIf { !it.isDeleted }
        }

        // Also search nebula fragments (deleted body remnants)
        val fragments = nebulaFragmentRepository.search(query)

        return SearchResult(
            matchingBodyIds = matchingBodies.map { it.id },
            matchingBodies = matchingBodies,
            nebulaFragments = fragments
        )
    }
}
