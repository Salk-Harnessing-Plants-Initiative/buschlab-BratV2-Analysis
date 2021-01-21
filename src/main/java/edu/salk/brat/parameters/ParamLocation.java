package edu.salk.brat.parameters;

import java.util.prefs.Preferences;

public enum ParamLocation {
    basic ("edu/salk/brat/bratanalysis"),
    advanced("edu/salk/brat/bratanalysis/advanced"),
    layout("edu/salk/brat/bratanalysis/layout");

    private final Preferences prefs;

    ParamLocation (String location) {
        this.prefs = Preferences.userRoot().node(location);
    }

    public Preferences getPrefs() {
        return prefs;
    }
}
