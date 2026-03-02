package com.projectorbit.ui.contextmenu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.ui.graphics.vector.ImageVector
import com.projectorbit.domain.model.BodyType

/**
 * Defines all available context menu actions for celestial bodies.
 * Available actions vary by body type.
 */
enum class MenuAction(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false
) {
    OPEN("Open", Icons.Default.OpenInFull),
    RENAME("Rename", Icons.Default.Edit),
    PIN("Pin", Icons.Default.PushPin),
    UNPIN("Unpin", Icons.Default.PushPin),
    ADD_CHILD("Add Child", Icons.Default.Add),
    TAG("Tag", Icons.Default.Tag),
    LINK("Link", Icons.Default.Link),
    SHARE("Share", Icons.Default.Share),
    UNSHARE("Unshare", Icons.Default.Share),
    CREATE_WORMHOLE("Wormhole", Icons.Default.Star),
    DELETE("Delete", Icons.Default.Delete, isDestructive = true)
}

/**
 * Returns the available [MenuAction]s for a given [BodyType] and body state.
 */
fun actionsForBodyType(
    bodyType: BodyType,
    isPinned: Boolean,
    isShared: Boolean
): List<MenuAction> = when (bodyType) {
    BodyType.SUN -> listOf(
        MenuAction.RENAME,
        MenuAction.ADD_CHILD,
        if (isPinned) MenuAction.UNPIN else MenuAction.PIN,
        MenuAction.DELETE
    )
    BodyType.GAS_GIANT, BodyType.BINARY_STAR -> listOf(
        MenuAction.RENAME,
        MenuAction.ADD_CHILD,
        if (isPinned) MenuAction.UNPIN else MenuAction.PIN,
        MenuAction.TAG,
        MenuAction.DELETE
    )
    BodyType.PLANET -> listOf(
        MenuAction.OPEN,
        MenuAction.RENAME,
        if (isPinned) MenuAction.UNPIN else MenuAction.PIN,
        MenuAction.TAG,
        MenuAction.LINK,
        MenuAction.CREATE_WORMHOLE,
        if (isShared) MenuAction.UNSHARE else MenuAction.SHARE,
        MenuAction.DELETE
    )
    BodyType.DWARF_PLANET -> listOf(
        MenuAction.OPEN,
        MenuAction.RENAME,
        if (isPinned) MenuAction.UNPIN else MenuAction.PIN,
        MenuAction.TAG,
        MenuAction.DELETE
    )
    BodyType.MOON -> listOf(
        MenuAction.RENAME,
        MenuAction.DELETE
    )
    BodyType.ASTEROID -> listOf(
        MenuAction.RENAME,
        MenuAction.DELETE
    )
    BodyType.NEBULA -> listOf(
        MenuAction.DELETE
    )
}
