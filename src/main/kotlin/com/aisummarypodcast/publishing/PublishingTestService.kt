package com.aisummarypodcast.publishing

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class TestConnectionResult(
    val success: Boolean,
    val message: String,
    val quota: Map<String, Any>? = null
)

data class FtpTestCredentials(
    val host: String,
    val port: Int = 21,
    val username: String,
    val password: String? = null,
    val useTls: Boolean = true
)

@Service
class PublishingTestService(
    private val soundCloudTokenManager: SoundCloudTokenManager,
    private val soundCloudClient: SoundCloudClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun testFtp(credentials: FtpTestCredentials): TestConnectionResult {
        val ftpClient = if (credentials.useTls) FTPSClient() else FTPClient()
        return try {
            ftpClient.connectTimeout = 10_000
            ftpClient.connect(credentials.host, credentials.port)
            if (!ftpClient.login(credentials.username, credentials.password ?: "")) {
                return TestConnectionResult(success = false, message = "Authentication failed")
            }
            ftpClient.enterLocalPassiveMode()
            ftpClient.listFiles("/")
            TestConnectionResult(success = true, message = "Connected successfully")
        } catch (e: Exception) {
            log.warn("FTP connection test failed: {}", e.message)
            TestConnectionResult(success = false, message = e.message ?: "Connection failed")
        } finally {
            try {
                if (ftpClient.isConnected) ftpClient.disconnect()
            } catch (_: Exception) {}
        }
    }

    fun testSoundCloud(userId: String): TestConnectionResult {
        return try {
            val accessToken = soundCloudTokenManager.getValidAccessToken(userId)
            val me = soundCloudClient.getMe(accessToken)
            TestConnectionResult(
                success = true,
                message = "Connected as ${me.username}",
                quota = me.quota?.let {
                    mapOf(
                        "uploadSecondsUsed" to it.uploadSecondsUsed,
                        "uploadSecondsLeft" to it.uploadSecondsLeft
                    )
                }
            )
        } catch (e: IllegalStateException) {
            val message = e.message ?: "Connection failed"
            if (message.contains("No") || message.contains("not found", ignoreCase = true)) {
                TestConnectionResult(success = false, message = "No SoundCloud connection. Please authorize first.")
            } else if (message.contains("refresh failed", ignoreCase = true) || message.contains("expired", ignoreCase = true)) {
                TestConnectionResult(success = false, message = "SoundCloud authorization expired. Please re-authorize.")
            } else {
                TestConnectionResult(success = false, message = message)
            }
        } catch (e: Exception) {
            log.warn("SoundCloud connection test failed: {}", e.message)
            TestConnectionResult(success = false, message = e.message ?: "Connection failed")
        }
    }
}
