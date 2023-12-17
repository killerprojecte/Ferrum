package dev.rgbmc.ferrum.api.objects;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class IncrementalIndex {
    @SerializedName("creation")
    private List<String> creation;

    @SerializedName("deletion")
    private List<String> deletion;

    @SerializedName("modification")
    private List<String> modification;

    public IncrementalIndex() {
        creation = new ArrayList<>();
        deletion = new ArrayList<>();
        modification = new ArrayList<>();
    }

    public List<String> getCreation() {
        return creation;
    }

    public List<String> getDeletion() {
        return deletion;
    }

    public List<String> getModification() {
        return modification;
    }
}
