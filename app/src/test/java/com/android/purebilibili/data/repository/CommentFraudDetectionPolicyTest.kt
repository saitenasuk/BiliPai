package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.CommentFraudStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentFraudDetectionPolicyTest {

    @Test
    fun `reply status should be normal when guest probe found`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.NORMAL, status)
    }

    @Test
    fun `reply status should be shadow banned when only auth probe found`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.SHADOW_BANNED, status)
    }

    @Test
    fun `reply status should be deleted when auth probe reports deleted hint`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authProbe = CommentPresenceProbe(
                requestSucceeded = true,
                found = false,
                deletedHint = true
            ),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.DELETED, status)
    }

    @Test
    fun `reply status should be unknown when guest probe failed`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = false, found = false),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNKNOWN, status)
    }

    @Test
    fun `root status should be under review when auth found and guest reply page visible`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            guestReplyPageVisible = true,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNDER_REVIEW, status)
    }

    @Test
    fun `root status should be shadow banned when auth found and guest reply page deleted`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            guestReplyPageVisible = false,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.SHADOW_BANNED, status)
    }

    @Test
    fun `root status should be deleted when auth seek probe has deleted hint`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(
                requestSucceeded = true,
                found = false,
                deletedHint = true
            ),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.DELETED, status)
    }

    @Test
    fun `root status should stay unknown when probes are not conclusive`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNKNOWN, status)
    }
}
