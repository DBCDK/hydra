/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.common;

public class ApplicationConstants {

    private ApplicationConstants() {

    }

    public static final String API_QUEUE = "/queue";

    public static final String API_HYDRA = "/hydra";
    public static final String API_HYDRA_STATUS = "status";
    public static final String API_HYDRA_INSTANCE_NAME = "instance";

    public static final String API_STATS = "/stats";
    public static final String API_STATS_QUEUE_AGENCIES = "/queueByAgency";
    public static final String API_STATS_QUEUE_WORKERS = "/queueByWorker";

    public static final String API_ERRORS = "/errors";
    public static final String API_ERRORS_WORKERS = "/byWorker";
    public static final String API_ERRORS_TYPE = "/byType";

}
