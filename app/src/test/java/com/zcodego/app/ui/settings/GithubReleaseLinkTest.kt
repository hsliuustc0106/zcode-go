package com.zcodego.app.ui.settings

import com.zcodego.app.security.LinkDecision
import com.zcodego.app.security.RemoteUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GithubReleaseLinkTest {
    @Test
    fun releasesPageStaysOutsideTheInAppPage() {
        assertEquals(
            "https://github.com/hsliuustc0106/zcode-go/releases",
            GithubReleaseLink.RELEASES_URL,
        )
        assertFalse(RemoteUrlPolicy.isAllowedNavigation(GithubReleaseLink.RELEASES_URL))
        assertFalse(LinkDecision.canSave(RemoteUrlPolicy.parseRemoteLink(GithubReleaseLink.RELEASES_URL)))
    }
}
