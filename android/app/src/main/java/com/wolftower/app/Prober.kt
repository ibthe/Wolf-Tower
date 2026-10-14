package com.wolftower.app

import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** Проверка доступности сайта через локальный SOCKS5 ByeDPI. */
object Prober {
    /**
     * Успех = TLS-рукопожатие прошло (с проверкой сертификата) и сервер ответил HTTP-строкой.
     * Имя домена передаётся прокси без локального DNS, как это делает приложение через туннель.
     */
    fun probe(host: String, socksPort: Int, timeoutMs: Int = 5000): Boolean {
        var raw: Socket? = null
        return try {
            raw = Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort)))
            raw.connect(InetSocketAddress.createUnresolved(host, 443), timeoutMs)
            raw.soTimeout = timeoutMs
            val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(raw, host, 443, true) as SSLSocket
            val params = ssl.sslParameters
            params.endpointIdentificationAlgorithm = "HTTPS"
            ssl.sslParameters = params
            ssl.soTimeout = timeoutMs
            ssl.startHandshake()
            val out = ssl.outputStream
            out.write(
                ("HEAD / HTTP/1.1\r\nHost: $host\r\nUser-Agent: Mozilla/5.0\r\n" +
                    "Connection: close\r\n\r\n").toByteArray()
            )
            out.flush()
            val line = ssl.inputStream.bufferedReader().readLine()
            line != null && line.startsWith("HTTP/")
        } catch (_: Exception) {
            false
        } finally {
            try {
                raw?.close()
            } catch (_: Exception) {
            }
        }
    }
}
