package org.cloudbus.agent.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigLoader {
    private static final Gson gson = new GsonBuilder().create();

    public static AgentsConfig getFromFile(String filename) throws FileNotFoundException {
        return gson.fromJson(new FileReader(filename), AgentsConfig.class);
    }
}
