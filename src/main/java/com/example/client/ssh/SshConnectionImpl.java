package com.example.client.ssh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Note that this implementation is not thread-safe, if multiple threads access operation connect/executeCommand/disconnect
 * concurrently, there must be some bad situations happen , from connect to disconnect should be an unit operation and it must
 * be synchronized externally. So it's better that each thread gets its own connection.
 * 
 */
public class SshConnectionImpl implements SshConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshConnectionImpl.class);

    /**
     * SSH Command to open an "exec channel".
     */
    protected static final String CMD_EXEC = "exec";
    /**
     * Keep-alive interval
     */
    private static final int ALIVE_INTERVAL = 30 * 1000;
    /**
     * Time to wait for the channel to close
     */
    private static final int CLOSURE_WAIT_TIMEOUT = 200;
    /**
     * Time to wait for each check for the channel to close
     */
    private static final int CHECK_FREQUENCY = 50;
    /**
     * Timeout settings for channel connect
     */
    private static final int CHANNEL_CONNECT_TIMEOUT = 30 * 1000;
    /**
     * Timeout settings to block for a read() or skip()
     * before throwing an InterruptedIOException
     */
    private static final int CHANNEL_DEFAULT_READ_TIMEOUT = 60 * 1000;
    /**
     * Timeout settings to block for a close()
     * before throwing an InterruptedIOException
     */
    private static final int CHANNEL_CLOSE_TIMEOUT = 60 * 1000;
    /**
     * Buffer size
     */
    private static final int IO_READ_BUFFERSIZE = 4096;
    /**
     * The default ssh port
     */
    public static final int DEFAULT_SSH_PORT = 22;
    /**
     * SSH Authentication type
     */
    private static final String AUTHENTICATION_TYPE = "publickey";

    private JSch client;
    private Session connectSession;
    private String host;
    private int port;
    private SshConfig sshConfig;

    protected SshConnectionImpl(String host, int port, SshConfig sshConfig) {
        this.host = host;
        this.port = port;
        this.sshConfig = sshConfig;
    }

    /**
     * Is the connection connected.
     * 
     * @return true if it is so.
     */
    public boolean isConnected() {
        return client != null && connectSession != null && connectSession.isConnected();
    }

    /**
     * Execute an ssh command on the server. After the command is sent the used channel is disconnected.
     * 
     * @param command the command to execute.
     * @return a String containing the output from the command.
     * @throws SshException if so.
     */
    public String executeCommand(String command) throws SshException {
        return executeCommand(command, 0);
    }

    public String executeCommand(String command, int timeout) throws SshException {
        Channel channel = null;
        TimeoutInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        ByteArrayOutputStream errOut = null;
        LOGGER.info("exec cmd : {}", command);
        try {
            channel = getChannel(CMD_EXEC);

            errOut = new ByteArrayOutputStream();
            channel.setExtOutputStream(errOut);

            ((ChannelExec) channel).setCommand(command);
            inputStream = new TimeoutInputStream(channel.getInputStream(), IO_READ_BUFFERSIZE, timeout > 0 ? timeout
                    : CHANNEL_DEFAULT_READ_TIMEOUT, CHANNEL_CLOSE_TIMEOUT);
            LOGGER.debug("connecting channel.");
            channel.connect(CHANNEL_CONNECT_TIMEOUT);

            outputStream = new ByteArrayOutputStream(IO_READ_BUFFERSIZE);
            byte[] buffer = new byte[IO_READ_BUFFERSIZE];
            int count = 0;
            while ((count = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            handleChannelError(channel, errOut);
            return outputStream.toString(StandardCharsets.UTF_8.toString());
        } catch (JSchException ex) {
            throw new SshException(ex);
        } catch (IOException ex) {
            throw new SshException("IOException happens when exec ssh command: " + command, ex);
        } finally {
            IOUtils.closeQuietly(errOut);
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
            channel.disconnect();
        }
    }

    public String executeSudoCommand(String command, String userPass) throws SshException {
        Channel channel = null;
        TimeoutInputStream inputStream = null;
        ByteArrayOutputStream errOut = null;
        ByteArrayOutputStream outputStream = null;
        try {
            channel = getChannel(CMD_EXEC);

            errOut = new ByteArrayOutputStream();
            channel.setExtOutputStream(errOut);
            // man sudo
            // -S The -S (stdin) option causes sudo to read the password from the
            // standard input instead of the terminal device.
            // -p The -p (prompt) option allows you to override the default
            // password prompt and use a custom one.
            ((ChannelExec) channel).setCommand("sudo -S -p ''" + command);
            ((ChannelExec) channel).setPty(Boolean.TRUE);
            inputStream = new TimeoutInputStream(channel.getInputStream(), IO_READ_BUFFERSIZE, CHANNEL_DEFAULT_READ_TIMEOUT,
                    CHANNEL_CLOSE_TIMEOUT);
            LOGGER.debug("connecting channel.");

            OutputStream out = channel.getOutputStream();
            channel.connect(CHANNEL_CONNECT_TIMEOUT);

            out.write((userPass + System.lineSeparator()).getBytes());
            out.flush();
            outputStream = new ByteArrayOutputStream(IO_READ_BUFFERSIZE);
            byte[] buffer = new byte[IO_READ_BUFFERSIZE];
            int count = 0;
            while ((count = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            handleChannelError(channel, errOut);
            return outputStream.toString(StandardCharsets.UTF_8.toString());
        } catch (JSchException ex) {
            throw new SshException(ex);
        } catch (IOException ex) {
            throw new SshException("IOException happens when exec ssh command: " + command, ex);
        } finally {
            IOUtils.closeQuietly(errOut);
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
            channel.disconnect();
        }
    }

    private Channel getChannel(String type) throws SshException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected!");
        }
        LOGGER.debug("Opening channel");
        try {
            return connectSession.openChannel(type);
        } catch (JSchException e) {
            throw new SshException("Open channel Exception .", e);
        }
    }

    private void handleChannelError(Channel channel, ByteArrayOutputStream errOut) throws SshException {
        // Exit code is only available if channel is closed, so wait a bit
        // for it.
        waitForChannelClosure(channel);
        int exitCode = channel.getExitStatus();
        if (exitCode > 0) {
            String error = errOut.toString();
            if (error != null && error.trim().length() > 0) {
                throw new SshException(error.trim() + " (" + String.valueOf(exitCode) + ")");
            } else {
                throw new SshException(String.valueOf(exitCode));
            }
        }
    }

    /**
     * Blocks until the given channel is close or the timout is reached
     * 
     * @param channel the channel to wait for
     */
    private void waitForChannelClosure(Channel channel) {
        final long start = System.currentTimeMillis();
        final long until = start + CLOSURE_WAIT_TIMEOUT;
        try {
            while (!channel.isClosed() && System.currentTimeMillis() < until) {
                Thread.sleep(CHECK_FREQUENCY);
            }
            LOGGER.trace("Time waited for channel closure: " + (System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            LOGGER.trace("Interrupted", e);
        }
        if (!channel.isClosed()) {
            LOGGER.trace("Channel not closed in timely manner!");
        }
    };

    /**
     * This version takes a command to run, and then returns a wrapper instance that exposes all the standard state of the
     * channel (stdin, stdout, stderr, exit status, etc).
     * 
     * @param command the command to execute.
     * @return a Channel with access to all streams and the exit code.
     * @throws SshException if there are any ssh problems.
     */
    public ChannelExec executeCommandChannel(String command) throws SshException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected!");
        }
        try {
            ChannelExec channel = (ChannelExec) connectSession.openChannel(CMD_EXEC);
            channel.setCommand(command);
            channel.connect();
            return channel;
        } catch (JSchException e) {
            throw new SshException(e);
        }
    }

    public void connect() throws SshException {
        try {
            if (client == null) {
                client = new JSch();
                client.setHostKeyRepository(new BlindHostKeyRepository());
                if (sshConfig.getPrivateKeyFile() != null) {
                    if (sshConfig.getPrivateKeyPhrase() == null) {
                        client.addIdentity(sshConfig.getPrivateKeyFile().getAbsolutePath());
                    } else {
                        client.addIdentity(sshConfig.getUsername(), sshConfig.getPrivateKeyPhrase(), null, sshConfig
                                .getPrivateKeyFilePassword().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            connectSession = client.getSession(sshConfig.getUsername(), host, port);
            if (StringUtils.isNotBlank(sshConfig.getUserPassword())) {
                connectSession.setPassword(sshConfig.getUserPassword());
            }
            Hashtable<String, String> config = new Hashtable<String, String>();
            config.put("PreferredAuthentications", AUTHENTICATION_TYPE);
            connectSession.setTimeout(CHANNEL_CONNECT_TIMEOUT);
            connectSession.setConfig(config);
            connectSession.connect();
            LOGGER.debug("Connected: {} ", connectSession.isConnected());
            connectSession.setServerAliveInterval(ALIVE_INTERVAL);
        } catch (JSchException ex) {
            throw new SshException(ex);
        }

    }

    /**
     * Disconnects the connection.
     */
    public void disconnect() {
        if (connectSession != null) {
            LOGGER.debug("Disconnecting client connection.");
            connectSession.disconnect();
            connectSession = null;
        }
    }

}
