package com.example.client.ssh;

import com.jcraft.jsch.ChannelExec;

public interface SshConnection {

    public boolean isConnected();

    public ChannelExec executeCommandChannel(String command) throws SshException;

    public String executeCommand(String command) throws SshException;

    public String executeCommand(String command, int timeout) throws SshException;

    public void connect() throws SshException;

    public void disconnect();

}
