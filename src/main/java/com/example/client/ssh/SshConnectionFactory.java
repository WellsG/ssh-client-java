package com.example.client.ssh;

public class SshConnectionFactory {

    private SshConnectionFactory() {
        throw new UnsupportedOperationException("Cannot initialize util classes.");
    }

    public static SshConnection getConnection(String host, int port, SshConfig sshConfig) {
        return new SshConnectionImpl(host, port, sshConfig);
    }
}
