package de.zkinqjustin.jobsystem.jobs;

public class Job {
    private final String name;
    private final String description;

    public Job(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

