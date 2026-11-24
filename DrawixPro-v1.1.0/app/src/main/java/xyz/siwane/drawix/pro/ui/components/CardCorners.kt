package xyz.siwane.drawix.pro.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** A heading or a standalone block ends a group; only adjoining vertical rows share corners. */
object CardCorners {
    val spacing = 8.dp
    val single = RoundedCornerShape(22.dp)
    val leading = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    val trailing = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    /** Logical start/end corners automatically mirror in RTL. */
    fun horizontal(index: Int, count: Int) = RoundedCornerShape(
        topStart = if (index == 0) 22.dp else 6.dp,
        bottomStart = if (index == 0) 22.dp else 6.dp,
        topEnd = if (index == count - 1) 22.dp else 6.dp,
        bottomEnd = if (index == count - 1) 22.dp else 6.dp
    )
    fun topRow(index: Int, count: Int) = RoundedCornerShape(
        topStart = if (index == 0) 22.dp else 6.dp,
        topEnd = if (index == count - 1) 22.dp else 6.dp,
        bottomStart = 6.dp, bottomEnd = 6.dp
    )
    fun grouped(index: Int, count: Int) = when {
        count <= 1 -> single
        index == 0 -> leading
        index == count - 1 -> trailing
        else -> RoundedCornerShape(6.dp)
    }
}
