package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.internal.DefaultMetadataProvider
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DefaultMetadataProviderTest {

    private val baseConfig = object : DiagnosticsConfig {
        override val appName = "TestApp"
        override val appId = "com.test.app"
        override val supportEmail = null
        override val maxStoredEntries = 100
        override val includeDeviceModelByDefault = true
        override val includeOsVersionByDefault = true
        override val redactor = DiagnosticsRedactor { it }
        override fun additionalMetadata() = emptyMap<String, String>()
    }

    private fun createProvider(
        config: DiagnosticsConfig = baseConfig,
        sessionId: String = "test-session-123",
    ): DefaultMetadataProvider {
        return DefaultMetadataProvider(
            context = RuntimeEnvironment.getApplication(),
            config = config,
            sessionId = sessionId,
        )
    }

    @Test
    fun `collect returns appName and appId from config`() {
        val metadata = createProvider().collect()

        assertThat(metadata.appName).isEqualTo("TestApp")
        assertThat(metadata.appId).isEqualTo("com.test.app")
    }

    @Test
    fun `collect returns provided session ID`() {
        val metadata = createProvider(sessionId = "my-session").collect()

        assertThat(metadata.sessionId).isEqualTo("my-session")
    }

    @Test
    fun `collect returns library version`() {
        val metadata = createProvider().collect()

        assertThat(metadata.libraryVersion).isNotEmpty()
    }

    @Test
    fun `collect returns non-zero generatedAt timestamp`() {
        val before = System.currentTimeMillis()
        val metadata = createProvider().collect()
        val after = System.currentTimeMillis()

        assertThat(metadata.generatedAt).isAtLeast(before)
        assertThat(metadata.generatedAt).isAtMost(after)
    }

    @Test
    fun `excludes OS version when includeOsVersionByDefault is false`() {
        val noOsConfig = object : DiagnosticsConfig by baseConfig {
            override val includeOsVersionByDefault = false
        }
        val metadata = createProvider(config = noOsConfig).collect()

        assertThat(metadata.androidVersion).isEmpty()
        assertThat(metadata.apiLevel).isEqualTo(0)
    }

    @Test
    fun `includes OS version when includeOsVersionByDefault is true`() {
        val metadata = createProvider().collect()

        assertThat(metadata.androidVersion).isNotEmpty()
        assertThat(metadata.apiLevel).isGreaterThan(0)
    }

    @Test
    fun `excludes device model when includeDeviceModelByDefault is false`() {
        val noDeviceConfig = object : DiagnosticsConfig by baseConfig {
            override val includeDeviceModelByDefault = false
        }
        val metadata = createProvider(config = noDeviceConfig).collect()

        assertThat(metadata.deviceManufacturer).isEmpty()
        assertThat(metadata.deviceModel).isEmpty()
    }

    @Test
    fun `includes device model when includeDeviceModelByDefault is true`() {
        val metadata = createProvider().collect()

        assertThat(metadata.deviceManufacturer).isNotEmpty()
        assertThat(metadata.deviceModel).isNotEmpty()
    }

    @Test
    fun `includes additional metadata from config`() {
        val configWithExtra = object : DiagnosticsConfig by baseConfig {
            override fun additionalMetadata() = mapOf("buildType" to "debug")
        }
        val metadata = createProvider(config = configWithExtra).collect()

        assertThat(metadata.additionalMetadata).containsEntry("buildType", "debug")
    }

    @Test
    fun `returns valid version info from package manager`() {
        val metadata = createProvider().collect()

        // Robolectric provides a default package with version info
        assertThat(metadata.versionName).isNotNull()
        assertThat(metadata.versionCode).isAtLeast(0L)
    }
}
