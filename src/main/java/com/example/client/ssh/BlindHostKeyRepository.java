package com.example.client.ssh;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

/**
 * A KnownHosts repository that blindly accept any host fingerprint as OK.
 */
public class BlindHostKeyRepository implements HostKeyRepository {

    private static final HostKey[] EMPTY = new HostKey[0];

    public int check(String host, byte[] key) {
        return 0;
    }

    public void add(HostKey hostkey, UserInfo ui) {

    }

    public void remove(String host, String type) {

    }

    public void remove(String host, String type, byte[] key) {

    }

    public String getKnownHostsRepositoryID() {
        return "";
    }

    public HostKey[] getHostKey() {
        return EMPTY;
    }

    public HostKey[] getHostKey(String host, String type) {
        return EMPTY;
    }

}
