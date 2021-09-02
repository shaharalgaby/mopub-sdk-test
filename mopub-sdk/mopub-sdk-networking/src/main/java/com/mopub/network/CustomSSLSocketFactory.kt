// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.net.SSLCertificateSocketFactory
import android.os.Build

import com.mopub.common.VisibleForTesting

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * An [SSLSocketFactory] that supports TLS settings for the MoPub ad servers.
 */
class CustomSSLSocketFactory private constructor() : SSLSocketFactory() {
    private var certificateSocketFactory: SSLSocketFactory? = null

    @Deprecated("setCertificateSocketFactory should only be used in tests")
    @VisibleForTesting
    fun setCertificateSocketFactory(sslSocketFactory: SSLSocketFactory) {
        certificateSocketFactory = sslSocketFactory
    }

    // Forward all methods. Enable TLS 1.1 and 1.2 before returning.

    // SocketFactory overrides
    @Throws(IOException::class)
    override fun createSocket(): Socket {
        return certificateSocketFactory?.createSocket().also {
            enableTlsIfAvailable(it)
        } ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, i: Int): Socket {
        return certificateSocketFactory?.createSocket(host, i).also {
            enableTlsIfAvailable(it)
        } ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localhost: InetAddress, localPort: Int): Socket {
        return certificateSocketFactory?.createSocket(host, port, localhost, localPort).also {
            enableTlsIfAvailable(it)
        } ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int): Socket {
        return certificateSocketFactory?.createSocket(address, port)?.also {
            enableTlsIfAvailable(it)
        } ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localhost: InetAddress, localPort: Int): Socket {
        return certificateSocketFactory?.createSocket(address, port, localhost, localPort)?.also {
            enableTlsIfAvailable(it)
        } ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
    }

    // SSLSocketFactory overrides

    override fun getDefaultCipherSuites(): Array<String> {
        return certificateSocketFactory?.defaultCipherSuites ?: arrayOf()
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return certificateSocketFactory?.supportedCipherSuites ?: arrayOf()
    }

    @Throws(IOException::class)
    override fun createSocket(socketParam: Socket?, host: String, port: Int, autoClose: Boolean): Socket {
        val csf = certificateSocketFactory ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
        // There is a bug in Android before version 6.0 where SNI does not work, so we try to do
        // it manually here.
        return when {
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) -> {
                // Don't use the original socket and create a new one. This closes the original socket
                // if the autoClose flag is set.
                if (autoClose) socketParam?.close()
                csf.createSocket(InetAddressUtils.getInetAddressByName(host), port).also {
                    enableTlsIfAvailable(it)
                    doManualServerNameIdentification(it, host)
                }
            }
            else -> {
                csf.createSocket(socketParam, host, port, autoClose).also {
                    enableTlsIfAvailable(it)
                }
            }
        }
    }

    /**
     * Some versions of Android fail to do server name identification (SNI) even though they are
     * able to. This method forces SNI to happen, if possible. SNI is only used in https
     * connections, and this method will no-op for http connections. This method throws an
     * SSLHandshakeException if SNI fails. This method may also throw other socket-related
     * IOExceptions.
     *
     * @param socket The socket to do SNI on
     * @param host   The host to verify the server name
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun doManualServerNameIdentification(socket: Socket,
                                                 host: String?) {
        certificateSocketFactory?.also {
            if (socket is SSLSocket && it is SSLCertificateSocketFactory) {
                it.setHostname(socket, host)
                verifyServerName(socket, host)
            }
        } ?: throw SocketException("SSLSocketFactory was null. Unable to create socket.")
    }

    private fun enableTlsIfAvailable(socket: Socket?) {
        if (socket is SSLSocket) {
            val supportedProtocols = socket.supportedProtocols
            // Make sure all supported protocols are enabled. Android does not enable TLSv1.1 or
            // TLSv1.2 by default.
            // TODO: Should we disable SSLv3?
            socket.enabledProtocols = supportedProtocols
        }
    }

    companion object {
        @JvmStatic
        fun getDefault(handshakeTimeoutMillis: Int): CustomSSLSocketFactory {
            val factory = CustomSSLSocketFactory()
            factory.certificateSocketFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis, null)

            return factory
        }

        /**
         * This actually performs server name identification.
         */
        @JvmStatic
        @VisibleForTesting
        @Throws(IOException::class)
        fun verifyServerName(sslSocket: SSLSocket,
                             host: String?) {
            sslSocket.startHandshake()
            val hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
            if (!hostnameVerifier.verify(host, sslSocket.session)) {
                throw SSLHandshakeException("Server Name Identification failed.")
            }
        }
    }
}
