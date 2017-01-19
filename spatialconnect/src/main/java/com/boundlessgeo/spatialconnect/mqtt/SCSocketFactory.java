/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SCSocketFactory extends SSLSocketFactory {

  private TrustManagerFactory tmf;
  private SSLSocketFactory factory;
  private static final String[] TLS_VERSIONS = new String[] { "TLSv1.2" };

  /**
   * Accepts an InputStream of the CA's X.509 certificate and creates an SSLSocketFactory that
   * trusts certificates signed by the CA.
   *
   * @param caCrtInputStream
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   * @throws CertificateException
   * @throws KeyManagementException
   */
  public SCSocketFactory(InputStream caCrtInputStream)
      throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException,
      KeyManagementException {
    tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    caKeyStore.load(null, null);
    CertificateFactory caCF = CertificateFactory.getInstance("X.509");
    Certificate ca = caCF.generateCertificate(caCrtInputStream);
    caKeyStore.setCertificateEntry("spatialconnect", ca);
    tmf.init(caKeyStore);
    SSLContext context = SSLContext.getInstance("TLSv1.2");
    context.init(null, tmf.getTrustManagers(), null);
    factory = context.getSocketFactory();
  }

  @Override public String[] getDefaultCipherSuites() {
    return factory.getDefaultCipherSuites();
  }

  @Override public String[] getSupportedCipherSuites() {
    return factory.getSupportedCipherSuites();
  }

  @Override public Socket createSocket() throws IOException {
    SSLSocket socket = (SSLSocket) factory.createSocket();
    socket.setEnabledProtocols(TLS_VERSIONS);
    return socket;
  }

  @Override public Socket createSocket(Socket s, String host, int port, boolean autoClose)
      throws IOException {
    SSLSocket socket = (SSLSocket) factory.createSocket(s, host, port, autoClose);
    socket.setEnabledProtocols(TLS_VERSIONS);
    return socket;
  }

  @Override public Socket createSocket(String host, int port) throws IOException {
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.setEnabledProtocols(TLS_VERSIONS);
    return socket;
  }

  @Override public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
      throws IOException {
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port, localHost, localPort);
    socket.setEnabledProtocols(TLS_VERSIONS);
    return socket;
  }

  @Override public Socket createSocket(InetAddress host, int port) throws IOException {
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.setEnabledProtocols(TLS_VERSIONS);
    return socket;
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
      throws IOException {
    SSLSocket socket = (SSLSocket) factory.createSocket(address, port, localAddress, localPort);
    socket.setEnabledProtocols(TLS_VERSIONS);
    return socket;
  }

}