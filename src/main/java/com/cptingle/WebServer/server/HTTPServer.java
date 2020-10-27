package com.cptingle.WebServer.server;

import com.cptingle.WebServer.exceptions.YAMLConfigurationException;
import com.cptingle.WebServer.util.DirTree;
import com.cptingle.WebServer.util.configuration.Configuration;
import com.cptingle.WebServer.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HTTPServer implements Runnable {
    private boolean debug = false;
    private boolean persistentConnections = false;
    private int persistentConnectionTimeout = 0;
    private String version = "";

    private int port;
    private String bindAddr;
    private ServerSocket servSock;
    private ThreadPoolExecutor executor;
    private Configuration config;

    private Logger logger;

    private Map<String, AbstractServlet> servletMap;


    public HTTPServer() {
        config = new Configuration();

        loadConfig();

        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        this.logger = new Logger(this);

        servletMap = new HashMap<>();
        loadServlets();

        // Open ServerSocket to listen for connections.
        try {
            servSock = new ServerSocket();
            servSock.bind(new InetSocketAddress(InetAddress.getByName(bindAddr), port));
            System.out.println("HTTP Server started " + (config.getBoolean("debug", false) ? "IN DEBUG MODE " : "") + "using " + (config.getSection("persistence").getBoolean("use-persistent-connections", true) ? "persistent connections" : "non-persistent connections"));
            System.out.println("Listening for requests on " + bindAddr + ":" + port);
            this.run();
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        } catch (YAMLConfigurationException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public boolean isDebugging() {
        return debug;
    }

    public boolean areConnectionsPersistent() {
        return persistentConnections;
    }

    public int getPersistentConnectionTimeout() {
        return persistentConnectionTimeout;
    }

    public String getVersion() {
        return version;
    }

    public Configuration getConfig() {
        return config;
    }

    public Logger getLogger() { return logger; }

    public HTTPServlet getServlet(String path) {
        return servletMap.get(path);
    }

    public void loadClasses(DirTree node, final File folder) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String filepath = "file://" + node.getPath() + "/";
        URL[] classPath = new URL[]{(new URL(filepath))};
        URLClassLoader cld = new URLClassLoader(classPath, this.getClass().getClassLoader());

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                DirTree tmp = new DirTree(fileEntry.getName(),fileEntry.getAbsolutePath(),true);
                node.addNode(tmp);
                loadClasses(tmp, fileEntry);
            } else {
                node.addNode(new DirTree(fileEntry.getName(),fileEntry.getAbsolutePath(), false));
                if (fileEntry.getName().contains(".class")) {
                    String name = fileEntry.getName().replaceAll(".class", "");
                    Class clazz = cld.loadClass(name);
                    Object obj = clazz.getConstructor(new Class[]{HTTPServer.class}).newInstance(this);
                    AbstractServlet asv = (AbstractServlet) obj;
                    for (String uri : asv.getURIs()){
                        servletMap.put(uri, asv);
                    }
                }
            }
        }
    }

    private void loadServlets() {
        try {
            loadClasses(new DirTree("servlets", Paths.get("servlets").toAbsolutePath().toString(), true), Paths.get("servlets").toFile());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the values from the configuration file into the com.cptingle.WebServer.server
     */
    private void loadConfig() {
        try {
            this.debug = config.getBoolean("debug", false);
            this.persistentConnections = config.getSection("persistence").getBoolean("use-persistent-connections", false);
            this.persistentConnectionTimeout = config.getSection("persistence").getInt("persistent-connection-timeout", 6000);
            this.bindAddr = config.getString("bind-address", "0.0.0.0");
            this.port = config.getInt("port", 80);
            this.version = config.getString("server-version");
            String webRoot = config.getSection("files").getString("web-root");
            try {
                if (!Files.exists(Paths.get(webRoot))) {
                    Files.createDirectories(Paths.get(webRoot));

                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (YAMLConfigurationException ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * Begin accepting socket requests from the clients. Upon receiving a connection from a client, create a new Server.ClientConnection object and dispatch it to the CachedThreadPool for execution
     */
    @Override
    public void run() {
        while(true) {
            try {
                executor.submit(new ClientConnection(servSock.accept(), this));
                if (debug)
                    System.out.println("Connection established with Client");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
