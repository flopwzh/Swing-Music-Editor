import java.util.ArrayList;

/*
 * class for a group of notes that are played at the same time (chord)
 * all notes, even if they are alone, are organized as a note group
 */
public class NoteGroup {
    private ArrayList<PlayableNote> notes; //list of notes to be played in the chord (if there are more)
    private int duration; // duration in ms
    private int ordering; // ordered based on the x value and staff number of the original notes, used for sorting
    private int startTime; // start time in ms, should only be calculated after all notes are known

    // for playback indicator
    private int xPos;
    private int yPos;

    public NoteGroup(ArrayList<PlayableNote> notes, int duration, int ordering, int xPos, int yPos) {
        this.notes = notes;
        this.duration = duration;
        this.ordering = ordering;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public NoteGroup(PlayableNote note, int duration, int ordering, int xPos, int yPos) {
        this.notes = new ArrayList<>();
        this.notes.add(note);
        this.duration = duration;
        this.ordering = ordering;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    // add a note and update the group duration if necessary
    public void addNote(PlayableNote note) {
        notes.add(note);
        if (note.getDuration() > duration) {
            duration = note.getDuration();
        }
    }

    // various getters and setters below

    public ArrayList<PlayableNote> getNotes() {
        return notes;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getXPos() {
        return xPos;
    }
    public int getYPos() {
        return yPos;
    }
}
