package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ManifestDeepLinkConfigurationTest {

    @Test
    fun manifest_supports_bilibili_scheme_open_with() {
        val manifest = loadManifestText()

        assertTrue(
            manifest.contains("""android:scheme="bilibili""""),
            "AndroidManifest should declare bilibili:// VIEW intent-filter so BiliPai appears in Open With"
        )
        assertTrue(
            manifest.contains("""android:scheme="bili""""),
            "AndroidManifest should declare bili:// VIEW intent-filter for broader deep link compatibility"
        )
        assertTrue(
            manifest.contains("""android.intent.category.BROWSABLE"""),
            "AndroidManifest deep links should be browsable from external apps"
        )
    }

    private fun loadManifestText(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate AndroidManifest.xml from ${File(".").absolutePath}")
        return manifestFile.readText()
    }
}
