/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.rest;

import dk.dbc.hydra.ErrorsAPI;
import dk.dbc.hydra.HydraAPI;
import dk.dbc.hydra.LibraryAPI;
import dk.dbc.hydra.QueueAPI;
import dk.dbc.hydra.StatisticsAPI;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application
 * by having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/api")
public class HydraApplication extends Application {
    private static final Set<Class<?>> classes = new HashSet<>(Arrays.asList(
            QueueAPI.class, LibraryAPI.class, HydraAPI.class, StatisticsAPI.class, ErrorsAPI.class));

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}