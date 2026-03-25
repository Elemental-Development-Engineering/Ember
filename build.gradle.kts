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

subprojects {
    plugins.withId("maven-publish") {
        val isSigningConfigured = providers.gradleProperty("signing.keyId").isPresent

        if (isSigningConfigured) {
            apply(plugin = "signing")
        }

        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication> {
                pom {
                    url.set("https://github.com/Elemental-Development-Engineering/Ember")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("elemental-development")
                            name.set("Elemental Development Engineering")
                            url.set("https://github.com/Elemental-Development-Engineering")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Elemental-Development-Engineering/Ember.git")
                        developerConnection.set("scm:git:ssh://github.com/Elemental-Development-Engineering/Ember.git")
                        url.set("https://github.com/Elemental-Development-Engineering/Ember")
                    }
                }
            }

            val sonatypeUsername = providers.gradleProperty("sonatypeUsername").orNull
            val sonatypePassword = providers.gradleProperty("sonatypePassword").orNull
            if (sonatypeUsername != null && sonatypePassword != null) {
                repositories {
                    maven {
                        name = "sonatype"
                        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                        credentials {
                            username = sonatypeUsername
                            password = sonatypePassword
                        }
                    }
                    maven {
                        name = "sonatypeSnapshots"
                        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                        credentials {
                            username = sonatypeUsername
                            password = sonatypePassword
                        }
                    }
                }
            }
        }

        if (isSigningConfigured) {
            extensions.configure<SigningExtension> {
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

tasks.register("publishLibrariesToMavenLocal") {
    group = "publishing"
    description = "Publishes the release artifacts for the library modules to Maven Local."
    dependsOn(
        ":diagnostics:publishReleasePublicationToMavenLocal",
        ":diagnostics-ui:publishReleasePublicationToMavenLocal",
    )
}
