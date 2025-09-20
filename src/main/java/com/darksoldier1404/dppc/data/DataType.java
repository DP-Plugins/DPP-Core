package com.darksoldier1404.dppc.data;

public enum DataType {
    USER, // Yaml file but saved in 'udata' folder with UUID as filename - if path is not specified, it will be saved in the udata folder
    YAML, // Yaml file saved in custom path with custom filename - if path is not specified, it will be saved in the data folder
    CUSTOM; // Custom object saved in custom path with custom filename - if path is not specified, it will be saved in the data folder
}