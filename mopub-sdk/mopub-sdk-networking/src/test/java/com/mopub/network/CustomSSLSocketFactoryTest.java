// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import androidx.annotation.Nullable;

import com.mopub.test.support.NetworkingTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.annotation.Config;

import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(NetworkingTestRunner.class)
@PrepareForTest(InetAddressUtils.class)
public class CustomSSLSocketFactoryTest {

    private CustomSSLSocketFactory subject;
    private SSLCertificateSocketFactory mockSSLCertificateSocketFactory;
    private SSLSocketWithSetHostname mockSSLSocket;

    @SuppressLint("SSLCertificateSocketFactoryCreateSocket")
    @Before
    public void setUp() throws Exception {
        mockSSLCertificateSocketFactory = mock(SSLCertificateSocketFactory.class);
        mockSSLSocket = mock(SSLSocketWithSetHostname.class);
        when(mockSSLCertificateSocketFactory.createSocket(any(InetAddress.class),
                anyInt())).thenReturn(mockSSLSocket);
        InetAddress mockInetAddress = mock(InetAddress.class);
        InetAddressUtils.setMockInetAddress(mockInetAddress);
        subject = CustomSSLSocketFactory.getDefault(0);
        subject.setCertificateSocketFactory(mockSSLCertificateSocketFactory);
    }

    @After
    public void tearDown() {
        InetAddressUtils.setMockInetAddress(null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void createSocket_withSocketParam_withAutoCloseTrue_shouldCloseOriginalSocket() throws Exception {
        final Socket mockSocket = mock(Socket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(true);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        subject.createSocket(mockSocket, "hostname", 443, true);

        verify(mockSocket).close();
        verify(mockSSLSocket).getSupportedProtocols();
        verify(mockSSLSocket).setEnabledProtocols(any(String[].class));
        verify(mockSSLSocket).startHandshake();
        verify(mockSSLSocket).getSession();
        verify(mockSSLCertificateSocketFactory).setHostname(any(Socket.class), eq("hostname"));
        verifyNoMoreInteractions(mockSocket);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void createSocket_withSocketParam_withAutoCloseFalse_shouldNotCloseOriginalSocket_shouldCallSetHostname() throws Exception {
        final Socket mockSocket = mock(Socket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(true);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        subject.createSocket(mockSocket, "hostname", 443, false);

        verify(mockSocket, never()).close();
        verify(mockSSLSocket).getSupportedProtocols();
        verify(mockSSLSocket).setEnabledProtocols(any(String[].class));
        verify(mockSSLSocket).startHandshake();
        verify(mockSSLSocket).getSession();
        verify(mockSSLCertificateSocketFactory).setHostname(any(Socket.class), eq("hostname"));
        verifyNoMoreInteractions(mockSSLSocket);
    }

    @Test
    public void verifyServerName_withValidServerNameIdentification_shouldNotThrowSSLHandshakeException() throws Exception {
        final SSLSocket mockSslSocket = mock(SSLSocket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(true);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        CustomSSLSocketFactory.verifyServerName(mockSslSocket, "hostname");
    }

    @Test(expected = SSLHandshakeException.class)
    public void verifyServerName_withInvalidServerNameIdentification_shouldThrowSSLHandshakeException() throws Exception {
        final SSLSocket mockSslSocket = mock(SSLSocket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(false);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        CustomSSLSocketFactory.verifyServerName(mockSslSocket, "hostname");
    }

    /**
     * This class has the setHostname() method that CustomSSLSocketFactory#setHostnameOnSocket uses
     * via reflection. This exists so we can Mockito.verify the method setHostname().
     */
    private abstract static class SSLSocketWithSetHostname extends SSLSocket {
        public void setHostname(@Nullable final String hostname) {}
    }
}
