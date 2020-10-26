package com.cptingle.WebServer.util.logging;

import com.cptingle.WebServer.exceptions.YAMLConfigurationException;
import com.cptingle.WebServer.server.HTTPServer;
import com.cptingle.WebServer.util.configuration.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class is used to log http requests into the specified log file
 */
public class Logger {
    private HTTPServer server;
    private SimpleDateFormat dateFormatter;
    private Configuration configSection;

    private boolean enabled;

    private String httpLogFilePath;
    private String httpLogFileBasePath;
    private String httpLogFileName;
    private int httpLogFileMaxLength;
    private File httpLogFile;
    private PrintWriter httpLogFileWriter;

    private String errorLogFilePath;
    private String errorLogFileBasePath;
    private String errorLogFileName;
    private int errorLogFileMaxLength;
    private File errorLogFile;
    private PrintWriter errorLogFileWriter;
    private LogLevel errorLogMinLogLevel;


    public Logger(HTTPServer server) {
        this.server = server;
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        try {
            this.configSection = server.getConfig().getSection("logging");
            this.enabled = configSection.getBoolean("enabled", true);

            this.httpLogFileBasePath = configSection.getString("http-log-file-path", ".") + "/";
            this.httpLogFileName = configSection.getString("http-log-file-name", "http_log");
            this.httpLogFileMaxLength = configSection.getInt("http-log-file-max-length", 150);
            this.httpLogFilePath = httpLogFileBasePath + httpLogFileName;
            this.httpLogFile = new File(httpLogFilePath);
            try {
                if (!Files.exists(Paths.get(httpLogFilePath))) {
                    Files.createDirectories(Paths.get(httpLogFileBasePath));
                    Files.createFile(Paths.get(httpLogFilePath));
                    this.httpLogFileWriter = new PrintWriter(new FileWriter(httpLogFilePath, true));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.httpLogFileWriter = new PrintWriter(new FileWriter(httpLogFilePath, true));

            this.errorLogFileBasePath = configSection.getString("error-log-file-path", ".") + "/";
            this.errorLogFileName = configSection.getString("error-log-file-name", "error_log");
            this.errorLogFileMaxLength = configSection.getInt("error-log-file-max-length", 150);
            this.errorLogFilePath = errorLogFileBasePath + errorLogFileName;
            this.errorLogFile = new File(errorLogFilePath);
            try {
                if (!Files.exists(Paths.get(errorLogFilePath))) {
                    Files.createDirectories(Paths.get(errorLogFileBasePath));
                    Files.createFile(Paths.get(errorLogFilePath));
                    this.errorLogFileWriter = new PrintWriter(new FileWriter(errorLogFilePath, true));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.errorLogFileWriter = new PrintWriter(new FileWriter(errorLogFilePath, true));
            this.errorLogMinLogLevel = LogLevel.valueOf(configSection.getString("error-log-minlevel", "WARN"));
        } catch (YAMLConfigurationException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logRequest(String ip, String hostname, String request, String status) {
        if (enabled) {
            httpLogFileWriter.printf("%-27s %-40s %-23s %-60s %7s %n", ip, hostname, dateFormatter.format(new Date()), request, status);
            httpLogFileWriter.flush();
            checkHttpLogFileLength();
        }
    }

    public void errorLog(String ip, LogLevel level, String message) {
        if (enabled && (level.val >= errorLogMinLogLevel.val || server.isDebugging())) {
            if (level == LogLevel.DEBUG || server.isDebugging()) {
                System.out.println("[" + dateFormatter.format(new Date()) + "] [" + level.text + "] [client " + ip + "] : " + message);

                if (level.val >= errorLogMinLogLevel.val) {
                    errorLogFileWriter.println("[" + dateFormatter.format(new Date()) + "] [" + level.text + "] [client " + ip + "] : " + message);
                    errorLogFileWriter.flush();
                    checkErrorLogFileLength();
                }
            } else {
                if (level.val >= LogLevel.SEVERE.val) {
                    System.err.println("[" + dateFormatter.format(new Date()) + "] [" + level.text + "] [client " + ip + "] : " + message);
                }

                errorLogFileWriter.println("[" + dateFormatter.format(new Date()) + "] [" + level.text + "] [client " + ip + "] : " + message);
                errorLogFileWriter.flush();
                checkErrorLogFileLength();
            }
        }
    }

    private void checkHttpLogFileLength() {
        try {
            Path filepath = Paths.get(httpLogFilePath);
            if (Files.exists(filepath) && Files.readAllLines(filepath, Charset.defaultCharset()).size() >= httpLogFileMaxLength) {
                int i = 1;
                while(Files.exists(Paths.get(httpLogFilePath + "_" + i))) {
                    i++;
                }
                Files.copy(filepath, Paths.get(httpLogFilePath + "_" + i));
                Files.write(filepath, new byte[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the error log file length and if it is longer than the max length,
     * copy the current file into the sequentially highest file and create a new error_log file
     */
    private void checkErrorLogFileLength() {
        try {
            Path filepath = Paths.get(errorLogFilePath);
            if (Files.exists(filepath) && Files.readAllLines(filepath, Charset.defaultCharset()).size() >= errorLogFileMaxLength) {
                int i = 1;
                while(Files.exists(Paths.get(errorLogFilePath + "_" + i))) {
                    i++;
                }
                Files.copy(filepath, Paths.get(errorLogFilePath + "_" + i));
                Files.write(filepath, new byte[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
