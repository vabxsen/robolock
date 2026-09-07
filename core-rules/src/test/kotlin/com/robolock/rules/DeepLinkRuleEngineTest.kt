package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepLinkRuleEngineTest {

    @Test
    fun `recognises youtube shorts urls`() {
        listOf(
            "https://youtube.com/shorts/abc123",
            "https://www.youtube.com/shorts/abc123",
            "https://m.youtube.com/shorts/abc_1-23/",
            "https://youtube.com/shorts/abc123?feature=share",
        ).forEach { url ->
            assertThat(DeepLinkRuleEngine.match(url)?.surface).isEqualTo(YouTubeShorts)
        }
    }

    @Test
    fun `recognises instagram reel urls`() {
        listOf(
            "https://instagram.com/reel/Cxyz123",
            "https://www.instagram.com/reels/Cxyz123",
            "https://instagram.com/reel/Cxyz123/",
        ).forEach { url ->
            assertThat(DeepLinkRuleEngine.match(url)?.surface).isEqualTo(InstagramReels)
        }
    }

    @Test
    fun `a normal instagram post is not a reel`() {
        // The single most important negative case: /p/ is an ordinary post. Matching it would mean
        // Robolock intercepting links it has no business claiming to understand.
        assertThat(DeepLinkRuleEngine.match("https://instagram.com/p/Cxyz123")).isNull()
    }

    @Test
    fun `a normal youtube video is not a short`() {
        assertThat(DeepLinkRuleEngine.match("https://youtube.com/watch?v=abc123")).isNull()
        assertThat(DeepLinkRuleEngine.match("https://youtu.be/abc123")).isNull()
    }

    @Test
    fun `bare hosts and profiles do not match`() {
        listOf(
            "https://instagram.com",
            "https://instagram.com/",
            "https://instagram.com/someuser",
            "https://youtube.com",
            "https://youtube.com/@channel",
        ).forEach { url ->
            assertThat(DeepLinkRuleEngine.match(url)).isNull()
        }
    }

    @Test
    fun `unrelated hosts never match`() {
        assertThat(DeepLinkRuleEngine.match("https://example.com/shorts/abc")).isNull()
        assertThat(DeepLinkRuleEngine.match("https://notyoutube.com/shorts/abc")).isNull()
    }

    @Test
    fun `a lookalike subdomain does not match`() {
        assertThat(DeepLinkRuleEngine.match("https://youtube.com.evil.test/shorts/abc")).isNull()
    }

    @Test
    fun `host matching is case insensitive`() {
        assertThat(DeepLinkRuleEngine.match("https://YouTube.com/shorts/abc")?.surface)
            .isEqualTo(YouTubeShorts)
    }

    @Test
    fun `malformed input is handled without throwing`() {
        listOf("", "not a url", "://", "https://", "shorts/abc").forEach { url ->
            assertThat(DeepLinkRuleEngine.match(url)).isNull()
        }
    }

    @Test
    fun `every pattern targets a deep-link surface`() {
        DeepLinkRuleEngine.patterns.forEach { pattern ->
            assertThat(pattern.surface.capability).isEqualTo(DetectionCapability.DEEP_LINK)
        }
    }
}
