package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF146C60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EFE5),
    onPrimaryContainer = Color(0xFF093B34),
    secondary = Color(0xFF536453),
    secondaryContainer = Color(0xFFE3EADD),
    background = Color(0xFFF6F8F4),
    surface = Color(0xFFFCFDF9),
    surfaceVariant = Color(0xFFE6EDE6),
    outline = Color(0xFF727C74)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF83D5BF),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF125044),
    onPrimaryContainer = Color(0xFFA2F2DB),
    secondary = Color(0xFFBBCBB7),
    background = Color(0xFF101713),
    surface = Color(0xFF172019),
    surfaceVariant = Color(0xFF38463C)
)

@Composable
fun StudyDayTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
