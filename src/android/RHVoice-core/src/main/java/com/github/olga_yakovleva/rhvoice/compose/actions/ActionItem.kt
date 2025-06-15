package com.github.olga_yakovleva.rhvoice.compose.actions

import androidx.compose.ui.graphics.painter.Painter

enum class ActionType(val type: String) {
    NEWS("news"),
    LISTEN_BOOK("listen_book"),
    QUIZ("quiz")
}

data class ActionItem(
    val title: String,
    val description: String,
    val image: Painter,
    val prompt: String,
    val actionType: ActionType
)
