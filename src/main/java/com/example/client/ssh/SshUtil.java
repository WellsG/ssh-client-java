package com.example.client.ssh;

import java.io.File;

public class SshUtil {

    public static final String PROPERTY_USERNAME = "user.name";
    public static final String PROPERTY_USER_HOME = "user.home";
    /**
     * The default dir to store key-file
     */
    public static final String DEFAULT_PRIVATE_KEY_DIR = ".ssh";
    public static final String DEFAULT_PRIVATE_KEY = "id_rsa";
    /**
     * Find private key file under /home/{userName}/.ssh/{privateKeyFileName}, if the parameter privateKeyFileName is not
     * specified then it will be the default one id_rsa.
     * 
     * @param userName
     * @param privateKeyFileName
     * @return
     */
    public static File getPrivateKeyFile(String userName, String privateKeyFileName) {
        System.setProperty(PROPERTY_USERNAME, userName);
        File privateKeyFile = new File(new File(System.getProperty(PROPERTY_USER_HOME),
                DEFAULT_PRIVATE_KEY_DIR), privateKeyFileName == null ? DEFAULT_PRIVATE_KEY
                : privateKeyFileName);
        return privateKeyFile;
    }
}
