package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpDevicePresentationPolicyTest {

    @Test
    fun `cling devices require avtransport to be considered castable`() {
        assertTrue(shouldIncludeClingDevice(hasAvTransport = true))
        assertTrue(!shouldIncludeClingDevice(hasAvTransport = false))
    }

    @Test
    fun `ssdp fallback devices stay hidden when cling devices already exist`() {
        val visible = resolveVisibleSsdpDevices(
            clingDevices = listOf(
                CastDeviceInfo(
                    udn = "renderer-1",
                    name = "Living Room TV",
                    description = "MediaRenderer",
                    location = "http://192.168.31.8:8899/rootDesc.xml"
                )
            ),
            ssdpDevices = listOf(
                SsdpDiscovery.SsdpDevice(
                    location = "http://192.168.31.9:8899/rootDesc.xml",
                    server = "Linux/3.10 DLNADOC/1.50",
                    usn = "uuid:renderer-2",
                    st = "urn:schemas-upnp-org:device:MediaRenderer:1"
                )
            ),
            profiles = mapOf(
                "http://192.168.31.9:8899/rootDesc.xml" to SsdpCastClient.SsdpDeviceProfile(
                    friendlyName = "Bedroom TV",
                    modelName = "Xiaomi",
                    avTransportEndpoint = SsdpCastClient.AvTransportEndpoint(
                        controlUrl = "http://192.168.31.9:8899/control",
                        serviceType = "urn:schemas-upnp-org:service:AVTransport:1"
                    )
                )
            )
        )

        assertTrue(visible.isEmpty())
    }

    @Test
    fun `ssdp fallback devices require avtransport and use friendly name`() {
        val visible = resolveVisibleSsdpDevices(
            clingDevices = emptyList(),
            ssdpDevices = listOf(
                SsdpDiscovery.SsdpDevice(
                    location = "http://192.168.31.9:8899/rootDesc.xml",
                    server = "Linux/3.10 DLNADOC/1.50",
                    usn = "uuid:renderer-2",
                    st = "urn:schemas-upnp-org:device:MediaRenderer:1"
                ),
                SsdpDiscovery.SsdpDevice(
                    location = "http://192.168.31.10:8899/rootDesc.xml",
                    server = "Random NAS",
                    usn = "uuid:nas-1",
                    st = "upnp:rootdevice"
                )
            ),
            profiles = mapOf(
                "http://192.168.31.9:8899/rootDesc.xml" to SsdpCastClient.SsdpDeviceProfile(
                    friendlyName = "Bedroom TV",
                    modelName = "Xiaomi",
                    avTransportEndpoint = SsdpCastClient.AvTransportEndpoint(
                        controlUrl = "http://192.168.31.9:8899/control",
                        serviceType = "urn:schemas-upnp-org:service:AVTransport:1"
                    )
                ),
                "http://192.168.31.10:8899/rootDesc.xml" to SsdpCastClient.SsdpDeviceProfile(
                    friendlyName = "NAS",
                    modelName = "Storage",
                    avTransportEndpoint = null
                )
            )
        )

        assertEquals(1, visible.size)
        assertEquals("Bedroom TV", visible.first().title)
        assertEquals("Xiaomi", visible.first().subtitle)
    }
}
