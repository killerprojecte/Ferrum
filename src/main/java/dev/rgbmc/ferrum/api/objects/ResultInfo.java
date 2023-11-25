package dev.rgbmc.ferrum.api.objects;

public class ResultInfo {
    private final int deletions;
    private final int modifications;
    private final int additions;

    public ResultInfo(int deletions, int modifications, int additions) {
        this.deletions = deletions;
        this.modifications = modifications;
        this.additions = additions;
    }

    public int getAdditions() {
        return additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public int getModifications() {
        return modifications;
    }
}
