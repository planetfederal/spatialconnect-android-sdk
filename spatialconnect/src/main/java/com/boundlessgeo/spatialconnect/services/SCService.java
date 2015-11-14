package com.boundlessgeo.spatialconnect.services;

import java.util.UUID;

public class SCService
{
    private String id;
    private SCServiceStatus status;

    public SCService()
    {
        this.id = UUID.randomUUID().toString();
        this.status = SCServiceStatus.SC_SERVICE_STOPPED;
    }

    public void start()
    {
        this.status = SCServiceStatus.SC_SERVICE_RUNNING;
    }

    public void stop()
    {
        //TODO
    }

    public void resume()
    {
        this.status = SCServiceStatus.SC_SERVICE_RUNNING;
    }

    public void pause()
    {
        this.status = SCServiceStatus.SC_SERVICE_PAUSED;
    }

    public String getId()
    {
        return id;
    }

    public SCServiceStatus getStatus()
    {
        return status;
    }
}
