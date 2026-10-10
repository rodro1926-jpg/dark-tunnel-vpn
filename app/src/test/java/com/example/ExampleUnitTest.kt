package com.example

import com.example.model.InjectionMode
import com.example.model.TunnelProtocol
import com.example.model.VpnConfig
import com.example.tunnel.ConfigParser
import com.example.tunnel.PayloadFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun payloadFormatter_macroExpansion_isCorrect() {
        val template = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf][crlf]"
        val chunks = PayloadFormatter.format(template, "sg01.darktunnel.net", 443)

        assertEquals(1, chunks.size)
        val result = chunks[0]
        assertTrue(result.contains("CONNECT sg01.darktunnel.net:443 HTTP/1.1\r\n"))
        assertTrue(result.contains("Host: sg01.darktunnel.net\r\n"))
    }

    @Test
    fun payloadFormatter_split_isCorrect() {
        val template = "GET http://[host]/ [protocol][crlf][split]CONNECT [host_port] [protocol][crlf][crlf]"
        val chunks = PayloadFormatter.format(template, "cloudflare.com", 80)

        assertEquals(2, chunks.size)
        assertTrue(chunks[0].contains("GET http://cloudflare.com/ HTTP/1.1"))
        assertTrue(chunks[1].contains("CONNECT cloudflare.com:80 HTTP/1.1"))
    }

    @Test
    fun configParser_vlessUrl_isCorrect() {
        val url = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@us01.darktunnel.net:443?type=ws&security=tls&sni=cloudflare.com#US-VLESS"
        val result = ConfigParser.parse(url)

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()
        assertEquals("US-VLESS", config.name)
        assertEquals("us01.darktunnel.net", config.serverHost)
        assertEquals(443, config.serverPort)
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", config.uuid)
        assertEquals(TunnelProtocol.VLESS, config.protocol)
        assertEquals("cloudflare.com", config.sniHost)
    }

    @Test
    fun configParser_trojanUrl_isCorrect() {
        val url = "trojan://secretpassword123@sg01.darktunnel.net:443?security=tls&sni=sg01.darktunnel.net#SG-Trojan"
        val result = ConfigParser.parse(url)

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()
        assertEquals("SG-Trojan", config.name)
        assertEquals("sg01.darktunnel.net", config.serverHost)
        assertEquals("secretpassword123", config.password)
        assertEquals(TunnelProtocol.TROJAN, config.protocol)
    }

    @Test
    fun configParser_encryptedDarkConfig_roundTrip() {
        val config = VpnConfig(
            name = "Locked Server",
            serverHost = "192.168.1.100",
            serverPort = 22,
            username = "admin",
            password = "supersecretpassword",
            protocol = TunnelProtocol.SSH_SSL,
            isLocked = true,
            providerName = "Dark Tunnel Official"
        )

        val encrypted = ConfigParser.encryptDarkConfig(config, "pass123")
        assertTrue(encrypted.startsWith("DARKTUNNEL:"))

        val decrypted = ConfigParser.parse(encrypted, "pass123")
        assertTrue(decrypted.isSuccess)
        val decConfig = decrypted.getOrThrow()
        assertEquals(config.name, decConfig.name)
        assertEquals(config.serverHost, decConfig.serverHost)
        assertEquals(config.password, decConfig.password)
        assertTrue(decConfig.isLocked)
        assertEquals("Dark Tunnel Official", decConfig.providerName)
    }

    @Test
    fun configParser_validation_detectsInvalidPort() {
        val invalidConfig = VpnConfig(
            name = "Bad Port Config",
            serverHost = "example.com",
            serverPort = 99999
        )
        val err = ConfigParser.validateConfig(invalidConfig)
        assertNotNull(err)
        assertTrue(err!!.contains("Invalid server port"))
    }
}
