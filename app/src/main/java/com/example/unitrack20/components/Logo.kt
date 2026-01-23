package com.example.unitrack20.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resId = remember {
        context.resources.getIdentifier("ic_unitrack_logo", "drawable", context.packageName)
    }

    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "UniTrack logo",
            modifier = modifier.size(110.dp)
        )
    } else {
        // Fallback placeholder
        Box(
            modifier = modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0x552196F3)),
            contentAlignment = Alignment.Center
        ) {
            Text("UT", color = Color.White, fontSize = 36.sp)
        }
    }
}
