
/*
 * Defines a playable note with pitch and duration.
 * Separate from Note to denote that this is specifically used for playback.
 */
public class PlayableNote {
    private String pitch;
    private int duration;

    public PlayableNote(String pitch, int duration) {
        this.pitch = pitch;
        this.duration = duration;
    }

    public String getPitch() {
        return pitch;
    }

    public int getDuration() {
        return duration;
    }
}
