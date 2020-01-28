/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.errors;

public class WorkerErrorTypes {

    private String worker;
    private String error;
    private int count;
    private String date;

    public WorkerErrorTypes() {
    }

    public WorkerErrorTypes(String worker, String error, int count, String date) {
        this.worker = worker;
        this.error = error;
        this.count = count;
        this.date = date;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "WorkerErrors{" +
                "worker='" + worker + '\'' +
                ", error='" + error + '\'' +
                ", count=" + count +
                ", date='" + date + '\'' +
                '}';
    }
}
