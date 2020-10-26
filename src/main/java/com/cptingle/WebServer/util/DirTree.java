package com.cptingle.WebServer.util;

import java.util.ArrayList;
import java.util.List;

public class DirTree {
    private List<DirTree> nodes;
    private boolean isDir;
    private String name,path;

    public DirTree(String name, String path, boolean isDir) {
        this.name = name;
        this.path = path;
        this.isDir = isDir;
        nodes = new ArrayList<DirTree>();
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDir;
    }

    public void addNode(DirTree node) {
        this.nodes.add(node);
    }

    public void removeNode(DirTree node) {
        this.nodes.remove(node);
    }

    public List<DirTree> getNodes() {
        return nodes;
    }
}
