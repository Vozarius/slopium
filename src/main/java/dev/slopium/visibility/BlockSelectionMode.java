package dev.slopium.visibility;
public enum BlockSelectionMode {
    ALL, WHITELIST, BLACKLIST;
    public boolean allows(boolean listed){return this==ALL||(this==WHITELIST)==listed;}
}
