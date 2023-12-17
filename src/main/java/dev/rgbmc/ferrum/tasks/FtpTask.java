package dev.rgbmc.ferrum.tasks;

import dev.rgbmc.expression.functions.FastFunction;
import dev.rgbmc.expression.functions.FunctionParameter;
import dev.rgbmc.expression.functions.FunctionResult;
import dev.rgbmc.ferrum.api.Backup;
import it.sauronsoftware.ftp4j.FTPClient;

public class FtpTask implements FastFunction {
    @Override
    public FunctionResult call(FunctionParameter parameter) {
        TaskParameter taskParameter = (TaskParameter) parameter;
        try {
            String[] params = taskParameter.getString()
                    .replace(", ", ",")
                    .replace(" ,", ",")
                    .split(",");
            FTPClient client = new FTPClient();
            switch (params[4]) {
                case "ftp": {
                    client.setSecurity(FTPClient.SECURITY_FTP);
                    break;
                }
                case "ftps": {
                    client.setSecurity(FTPClient.SECURITY_FTPS);
                    break;
                }
                case "ftpes": {
                    client.setSecurity(FTPClient.SECURITY_FTPES);
                    break;
                }
            }
            client.connect(params[0], Integer.parseInt(params[1]));
            client.login(params[2], params[3]);
            Backup.logger.info("[FTP-Task] Login Successful, Start Uploading file...");
            if (params.length > 5) {
                Backup.logger.info("[FTP-Task] Switch to path: " + params[5]);
                client.changeDirectory(params[5]);
            }
            client.upload(taskParameter.getBackup().getFile());
            Backup.logger.info("[FTP-Task] Upload Finished!");
            client.disconnect(true);
        } catch (Exception e) {
            Backup.logger.error(e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }

    @Override
    public String getName() {
        return "ftp";
    }
}
