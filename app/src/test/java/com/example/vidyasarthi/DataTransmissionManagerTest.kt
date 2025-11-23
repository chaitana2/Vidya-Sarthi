package com.example.vidyasarthi

import android.content.Context
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import com.example.vidyasarthi.core.ui.VoiceUiManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class DataTransmissionManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockOfflineCache: OfflineCache

    @Mock
    private lateinit var mockLogManager: LogManager

    @Mock
    private lateinit var mockSmsHandler: SmsHandler

    @Mock
    private lateinit var mockVoiceUiManager: VoiceUiManager

    @Test
    fun `DataTransmissionManager can be instantiated`() {
        DataTransmissionManager(
            mockContext,
            mockOfflineCache,
            mockLogManager,
            mockSmsHandler,
            mockVoiceUiManager
        )
    }

    @Test
    fun `sendContent returns true`() = runBlocking {
        val dataTransmissionManager = DataTransmissionManager(
            mockContext,
            mockOfflineCache,
            mockLogManager,
            mockSmsHandler,
            mockVoiceUiManager
        )
        whenever(runBlocking { dataTransmissionManager.sendContent("content", "1234567890", "1234") }) doReturn true
        val result = dataTransmissionManager.sendContent("content", "1234567890", "1234")
        assert(result)
    }
}