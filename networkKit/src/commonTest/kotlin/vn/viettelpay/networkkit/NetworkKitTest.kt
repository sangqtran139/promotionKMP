package vn.viettelpay.networkkit

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkKitTest {

    @Test
    fun versionPlaceholderIsSet() {
        assertEquals("0.1.0", NetworkKit.VERSION)
    }
}
