package io.github.lamba92.openaqua.engine.host.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.readNdjsonFromStdin
import kotlinx.coroutines.runBlocking

class StreamFansCommand(
    private val controller: HostHardwareController,
) : CliktCommand(
        name = "stream-fans",
    ) {
    override fun help(context: Context): String = "Continuously consume fan duty cycle updates via NDJSON on stdin and apply to hardware"

    override fun run() {
        val requestsFlow = readNdjsonFromStdin<HostFanControlRequest>()
        runBlocking {
            try {
                controller.streamFanControl(requestsFlow)
            } finally {
                controller.restoreFanDefaults()
            }
        }
    }
}
