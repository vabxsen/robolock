package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The guarantees that matter most: Robolock must never claim to have seen something Android
 * never told it about.
 *
 * Most of this is enforced by the type system — [AppForegroundEvidence] takes an
 * [AppLevelSurface] and [DeepLinkEvidence] takes a [DeepLinkSurface], so there is simply no
 * constructor that accepts an [UnsupportedSurface], and no way to reach [ObservedIntervention]
 * except through evidence. These tests pin the parts a compiler cannot check.
 */
class HonestyTest {

    @Test
    fun `whatsapp status is modelled as undetectable`() {
        assertThat(WhatsAppStatus.capability).isEqualTo(DetectionCapability.UNSUPPORTED)
        assertThat(WhatsAppStatus).isInstanceOf(UnsupportedSurface::class.java)
    }

    @Test
    fun `an unsupported surface is neither app-level nor deep-link detectable`() {
        // If either of these ever became true, evidence could be built for it and fabricated
        // statistics would follow.
        assertThat(WhatsAppStatus).isNotInstanceOf(AppLevelSurface::class.java)
        assertThat(WhatsAppStatus).isNotInstanceOf(DeepLinkSurface::class.java)
    }

    @Test
    fun `reels and shorts are only claimable via a link`() {
        listOf(InstagramReels, YouTubeShorts).forEach { surface ->
            assertThat(surface.capability).isEqualTo(DetectionCapability.DEEP_LINK)
            assertThat(surface).isNotInstanceOf(AppLevelSurface::class.java)
        }
    }

    @Test
    fun `short labels read naturally in a sentence`() {
        // "This is a Short." rather than "This is a YouTube Shorts."
        assertThat(YouTubeShorts.shortLabel).isEqualTo("Short")
        assertThat(InstagramReels.shortLabel).isEqualTo("Reel")
        // Anything without an explicit singular falls back to its display name.
        assertThat(WhatsAppStatus.shortLabel).isEqualTo("WhatsApp Status")
    }

    @Test
    fun `whole-app surfaces are app-level and nothing more`() {
        listOf(InstagramApp, YouTubeApp, WhatsAppApp).forEach { surface ->
            assertThat(surface.capability).isEqualTo(DetectionCapability.APP_LEVEL)
            assertThat(surface).isNotInstanceOf(DeepLinkSurface::class.java)
        }
    }

    @Test
    fun `no shipped surface relies on the reserved network hint capability`() {
        assertThat(SurfaceCatalog.all.none { it.capability == DetectionCapability.NETWORK_HINT }).isTrue()
    }

    @Test
    fun `surface ids are unique and stable storage keys`() {
        val ids = SurfaceCatalog.all.map { it.id }
        assertThat(ids).containsNoDuplicates()
        assertThat(SurfaceCatalog.byId("whatsapp_status")).isEqualTo(WhatsAppStatus)
    }

    @Test
    fun `an app surface exists for any package, including ones not built in`() {
        // UsageStats reports foreground transitions for every app, so app-level evidence for one
        // outside the built-in list is still something Robolock genuinely observed.
        val standIn = SurfaceCatalog.appSurfaceFor("com.android.chrome", "Chrome")
        assertThat(standIn.capability).isEqualTo(DetectionCapability.APP_LEVEL)
        assertThat(standIn.id).isEqualTo("app:com.android.chrome")

        // And it can produce evidence, which is what makes stand-in testing exercise the real path.
        val recorded = AppForegroundEvidence(standIn, 1L, 0L)
            .toIntervention(InterventionReason.SESSION_LIMIT, InterventionAction.EXITED)
        assertThat(recorded.packageName).isEqualTo("com.android.chrome")
    }

    @Test
    fun `built-in app surfaces are reused rather than duplicated`() {
        assertThat(SurfaceCatalog.appSurfaceFor(SupportedPackages.INSTAGRAM)).isEqualTo(InstagramApp)
    }

    @Test
    fun `an app surface id round trips through the catalog`() {
        val surface = SurfaceCatalog.appSurfaceFor("com.example.app")
        assertThat(SurfaceCatalog.byId(surface.id)?.appPackage).isEqualTo("com.example.app")
    }

    @Test
    fun `only named surfaces are user-selectable`() {
        // The whole-app entries are not choices on the Distractions screen.
        assertThat(SurfaceCatalog.selectable).containsExactly(
            InstagramReels, YouTubeShorts, WhatsAppStatus,
        )
    }

    @Test
    fun `evidence carries its surface through to the recorded intervention`() {
        val evidence = AppForegroundEvidence(InstagramApp, atUtcMillis = 42L, sessionMillisBefore = 900L)
        val recorded = evidence.toIntervention(InterventionReason.SESSION_LIMIT, InterventionAction.EXITED)

        assertThat(recorded.surfaceId).isEqualTo(InstagramApp.id)
        assertThat(recorded.packageName).isEqualTo(SupportedPackages.INSTAGRAM)
        assertThat(recorded.timestampUtc).isEqualTo(42L)
        assertThat(recorded.sessionMillisBefore).isEqualTo(900L)
    }

    @Test
    fun `a deep link intervention records the link surface`() {
        val evidence = DeepLinkEvidence(YouTubeShorts, atUtcMillis = 7L, uri = "https://youtube.com/shorts/abc")
        val recorded = evidence.toIntervention(InterventionReason.DEEP_LINK, InterventionAction.CONTINUED, 60_000L)

        assertThat(recorded.surfaceId).isEqualTo("youtube_shorts")
        assertThat(recorded.bypassMillis).isEqualTo(60_000L)
    }

    @Test
    fun `no data is distinct from a zero count`() {
        val noData: SurfaceStats = SurfaceStats.NoData(WhatsAppStatus)
        val zero: SurfaceStats = SurfaceStats.Counted(InstagramApp, interventions = 0, exits = 0)

        assertThat(noData).isNotEqualTo(zero)
        assertThat(noData).isInstanceOf(SurfaceStats.NoData::class.java)
    }
}
