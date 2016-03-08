# ssh-client-java
        String hostName = "";
        String port = "";
        String username = ""; 
        String privateKeyFile = ""; 
        String privateKeyFilePassword = "";

        String command = "";
        SshConfig sshConfig = new SshConfig(); 
        shConfig.setUsername(username);
        if (StringUtils.isNotBlank(privateKeyFile)) { 
              shConfig.setPrivateKeyFile(new File(privateKeyFile)); 
              sshConfig.setPrivateKeyFilePassword(privateKeyFilePassword); 
        } SshConnection client = null; 
        try { 
            client = getSshConnection(hostName, port, sshConfig); 
            String result = client.executeCommand(command, DEFAULT_SSH_CMD_TIMEOUT); 
        } catch (Throwable e) { 
            logger.error(e); 
        } finally { client.disconnect(); }

        private SshConnection getSshConnection(String hostName, String port, SshConfig sshConfig) throws Exception { 
              SshConnection client = SshConnectionFactory.getConnection(hostName, Integer.parseInt(port), sshConfig); 
              client.connect(); return client; 
        }
