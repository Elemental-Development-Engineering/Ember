plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    group = providers.gradleProperty("GROUP").orElse("com.elementaldevelopment").get()
    version = providers.gradleProperty("VERSION_NAME").orElse("0.1.0").get()
}

tasks.register("publishLibrariesToMavenLocal") {
    group = "publishing"
    description = "Publishes the release artifacts for the library modules to Maven Local."
    dependsOn(
        ":diagnostics:publishReleasePublicationToMavenLocal",
        ":diagnostics-ui:publishReleasePublicationToMavenLocal",
    )
}
