package com.example.worktime.model;

public class Worker {
    private String code;
    private String name;
    private String workshop;
    private String team;

    public Worker() {}

    public Worker(String code, String name, String workshop, String team) {
        this.code = code;
        this.name = name;
        this.workshop = workshop;
        this.team = team;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWorkshop() { return workshop; }
    public void setWorkshop(String workshop) { this.workshop = workshop; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }
}
