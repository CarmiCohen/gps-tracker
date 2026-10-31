package com.jd.core.network

import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject

/**
 * SocketFactory: Abstraction for socket creation to enable unit testing of signaling logic.
 */
interface SocketFactory {
    fun createSocket(url: String, options: IO.Options): Socket
}

class DefaultSocketFactory @Inject constructor() : SocketFactory {
    override fun createSocket(url: String, options: IO.Options): Socket {
        return IO.socket(url, options)
    }
}
