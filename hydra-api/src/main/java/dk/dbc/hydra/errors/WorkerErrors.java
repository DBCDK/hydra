/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.errors;

public class WorkerErrors {

    private String worker;
    private int count;
    private String date;

    public WorkerErrors() {
    }

    public WorkerErrors(String worker, int count, String date) {
        this.worker = worker;
        this.count = count;
        this.date = date;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
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
                ", count=" + count +
                ", date='" + date + '\'' +
                '}';
    }
}
