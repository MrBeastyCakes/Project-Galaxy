package com.projectorbit.domain.usecase

import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.model.NoteContent
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.NoteContentRepository
import javax.inject.Inject

/**
 * Accretion use case: merges an asteroid into a target planet.
 *
 * On merge:
 *  1. Append asteroid's text content to the planet's note content
 *  2. Recalculate planet mass/radius from combined word count
 *  3. Soft-delete the asteroid from Room
 *  4. Remove asteroid from PhysicsWorld
 *
 * Undo: caller should capture the pre-merge planet content and asteroid entity
 * before invoking, then use the returned [MergeResult] for the 30s undo window.
 */
class MergeAsteroidUseCase @Inject constructor(
    private val bodyRepository: CelestialBodyRepository,
    private val noteRepository: NoteContentRepository,
    private val physicsWorld: PhysicsWorld
) {
    data class MergeResult(
        val asteroidId: String,
        val planetId: String,
        val preMergePlanetContent: NoteContent?,
        val asteroidContent: NoteContent?
    )

    suspend operator fun invoke(asteroidId: String, planetId: String): MergeResult {
        val now = System.currentTimeMillis()

        val asteroidContent = noteRepository.getForBody(asteroidId)
        val planetContent = noteRepository.getForBody(planetId)
        val planet = bodyRepository.getById(planetId)

        // Append asteroid text to planet content
        if (asteroidContent != null && asteroidContent.plainText.isNotBlank()) {
            val separator = if (planetContent?.plainText?.isNotBlank() == true) "\n\n" else ""
            val mergedPlain = (planetContent?.plainText ?: "") + separator + asteroidContent.plainText
            val mergedRich = mergeRichText(planetContent?.richTextJson, asteroidContent.richTextJson)

            noteRepository.upsert(
                NoteContent(
                    bodyId = planetId,
                    richTextJson = mergedRich,
                    plainText = mergedPlain,
                    updatedAt = now
                )
            )

            // Update planet mass/radius from new word count
            val newWordCount = mergedPlain.split("\\s+".toRegex()).count { it.isNotEmpty() }
            val newMass = CelestialBody.computeMass(
                wordCount = newWordCount,
                accessCount = planet?.accessCount ?: 0,
                isPinned = planet?.isPinned ?: false
            )
            val newRadius = CelestialBody.computeRadius(newMass)
            bodyRepository.updateMassAndRadius(planetId, newMass, newRadius)
            physicsWorld.setMass(planetId, newMass)
        }

        // Soft-delete the asteroid
        bodyRepository.softDelete(asteroidId, now)
        physicsWorld.removeBody(asteroidId)

        return MergeResult(
            asteroidId = asteroidId,
            planetId = planetId,
            preMergePlanetContent = planetContent,
            asteroidContent = asteroidContent
        )
    }

    private fun mergeRichText(existingJson: String?, appendJson: String?): String {
        // Simple concatenation strategy -- the rich text editor layer handles
        // proper JSON merging. Here we produce a valid fallback.
        if (existingJson.isNullOrBlank()) return appendJson ?: ""
        if (appendJson.isNullOrBlank()) return existingJson
        // Wrap both as a combined JSON array for the rich text engine to parse
        return """{"merged":true,"parts":[$existingJson,$appendJson]}"""
    }
}
