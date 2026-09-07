plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktfmt().kotlinlangStyle()
    }
    format("misc") {
        target("**/*.md", "**/*.xml", "**/*.toml", "**/*.properties", ".gitignore")
        targetExclude("**/build/**", ".gradle/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
