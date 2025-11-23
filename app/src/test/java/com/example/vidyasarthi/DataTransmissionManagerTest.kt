package com.example.vidyasarthi

import android.content.Context
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import com.example.vidyasarthi.core.ui.VoiceUiManager
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

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
}