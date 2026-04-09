import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Note{

    private int x;
    private int y;
    private int duration;
    private int type;
    private int staff;
    private BufferedImage image;
    private String pitch;
    private boolean drawBounds;
    private Note accidental;
    private Note parent;

    private final int SHARPXOFFSET = -18;
    private final int SHARPYOFFSET = -12;
    private final int FLATXOFFSET = -19;
    private final int FLATYOFFSET = -14;

    public Note(int x, int y, int duration, int type, int staff, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.duration = duration;
        this.type = type;
        this.staff = staff;
        this.image = image;
        drawBounds = false;
        accidental = null;
        parent = null;
    }

    /*
     * method to draw the note at its current x and y coords
     */
    public void drawNote(Graphics g) {
        g.drawImage(image, x, y, null);
        // if (accidental != null) {
        //     drawAccidental(g, accidental);
        // }
    }
    /*
     * method to draw the bounds of the note for select, accidentals, etc
     */
    public void drawBounds(Graphics g) {
        g.setColor(Color.RED);
        g.drawRect(x - 2, y - 2,
                    image.getWidth() + 2,
                    image.getHeight() + 2);
        g.setColor(Color.BLACK);
    }

    /*
     * method to draw the accidental of the note if it has one or one is provided (temporary drawing)
     */
    public void drawAccidental(Graphics g, Note acc) {
        if (acc == null) {
            if (accidental == null) {
                return;
            }
            acc = accidental;
        }
        if (acc.type == 2) { //flat
            g.setColor(Color.BLACK);
            g.drawImage(acc.getImage(), getOffsetX() + FLATXOFFSET, getOffsetY() + FLATYOFFSET, null);

        } else if (acc.type == 3) { //sharp
            g.setColor(Color.BLACK);
            g.drawImage(acc.getImage(), getOffsetX() + SHARPXOFFSET, getOffsetY() + SHARPYOFFSET, null);
        }
    }

    // Getters and Setters
    // the offset accounts for notes with their "center" being not on the component coordinates
    public int getOffsetX() {
        if (type == 0) {
            if (duration == 0) { // sixteenth note
                return x + 6;
            } else if (duration == 1) { // eighth note
                return x + 15;
            } else if (duration == 2) { // quarter note
                return x + 7;
            } else if (duration == 3) { // half note
                return x + 15;
            } else if (duration == 4) { // whole note
                return x + 10;
            }
        }
        return x;
    }

    // also calls setX to update accidental
    public void setOffsetX(int x) {
        if (type == 0) {
            if (duration == 0) { // sixteenth note
                setX(x - 6);
            } else if (duration == 1) { // eighth note
                setX(x - 15);
            } else if (duration == 2) { // quarter note
                setX(x - 7);
            } else if (duration == 3) { // half note
                setX(x - 15);
            } else if (duration == 4) { // whole note
                setX(x - 10);
            }
        } else{
            setX(x);
        };
    }

    public int getOffsetY() {
        if (type == 0) {
            if (duration == 0) { // sixteenth note
                return y + 35;
            } else if (duration == 1) { // eighth note
                return y + 36;
            } else if (duration == 2) { // quarter note
                return y + 35;
            } else if (duration == 3) { // half note
                return y + 34;
            } else if (duration == 4) { // whole note
                return y + 6;
            }
        }
        return y;
    }

    // also calls setY to update accidental
    public void setOffsetY(int y) {
        if (type == 0) {
            if (duration == 0) { // sixteenth note
                setY(y - 35);
            } else if (duration == 1) { // eighth note
                setY(y - 36);
            } else if (duration == 2) { // quarter note
                setY(y - 35);
            } else if (duration == 3) { // half note
                setY(y - 34);
            } else if (duration == 4) { // whole note
                setY(y - 6);
            }
        } else{
            setY(y);
        };
    }

    public int getX() {
        return x;
    }

    // sets X and also updates the accidental if it exists
    public void setX(int x) {
        this.x = x;
        if (accidental != null) {
            if (accidental.getType() == 2) {
                accidental.setOffsetX(getOffsetX() + FLATXOFFSET);
            } else if (accidental.getType() == 3) {
                accidental.setOffsetX(getOffsetX() + SHARPXOFFSET);
            }
        }
    }

    public int getY() {
        return y;
    }

    // sets Y and also updates the accidental if it exists
    public void setY(int y) {
        this.y = y;
        if (accidental != null) {
            if (accidental.getType() == 2) {
                accidental.setOffsetY(getOffsetY() + FLATYOFFSET);
            } else if (accidental.getType() == 3) {
                accidental.setOffsetY(getOffsetY() + SHARPYOFFSET);
            }
        }
    }

    public int getDuration() {
        return duration;
    }

    // returns duration represented as a readable string
    public String getDurationString() {
        switch (duration) {
            case 0:
                return "sixteenth";
            case 1:
                return "eighth";
            case 2:
                return "quarter";
            case 3:
                return "half";
            case 4:
                return "whole";
            default:
                return "unknown";
        }
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getType() {
        return type;
    }

    public String getTypeString() {
        switch (type) {
            case 0:
                return "note";
            case 1:
                return "rest";
            case 2:
                return "flat";
            case 3:
                return "sharp";
            default:
                return "unknown";
        }
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStaffNum() {
        return staff;
    }

    public void setStaffNum(int staff) {
        this.staff = staff;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public String getPitch() {
        return pitch;
    }

    // make the pitch only valid for notes
    public void setPitch(String pitch) {
        if (type == 0) {
            if (accidental != null && !pitch.substring(0, 2).equals("un")) {
                if (accidental.getType() == 2) {
                    this.pitch = pitch.substring(0, 1) + "b" + pitch.substring(1);
                } else if (accidental.getType() == 3) {
                    this.pitch = pitch.substring(0, 1) + "#" + pitch.substring(1);
                }
                // this idea was causing problems with pitch being set incorrectly
                // personally think this is easier to handle in the musicView class
                // // for the midiplayer, some constructions are invalid like e#, so we will map it correctly
                // String base = this.pitch.substring(0, 2);
                // if (base.equals("E#")) {
                //     this.pitch = "F" + this.pitch.substring(2);
                // } else if (base.equals("B#")) {
                //     this.pitch = "C" + this.pitch.substring(2);
                // } else if (base.equals("Fb")) {
                //     this.pitch = "E" + this.pitch.substring(2);
                // } else if (base.equals("Cb")) {
                //     this.pitch = "B" + this.pitch.substring(2);
                // }
            } else {
                this.pitch = pitch;
            }
        } else if (type == 1) {
            this.pitch = "rest";
        } else {
            this.pitch = "none";
        }
    }

    // returns the current accidental
    public Note getAccidental() {
        return accidental;
    }


    /*
     * sets accidental and updates pitch accordingly
     * returns the previous accidental if it existed
     */
    public Note setAccidental(Note acc) {
        int accType = acc == null ? -1 : acc.getType();
        Note currAcc = this.accidental;
        this.accidental = acc;
        // change pitch depending on accidental added and/or removed
        if (currAcc != null) {
            pitch = pitch.substring(0, 1) + pitch.substring(2);
        }
        setPitch(pitch);
        if (acc != null) {
            acc.setParent(this);  
            acc.setStaffNum(this.staff);

            // correct offsets for the accidentals need to be set
            if (accType == 2) {
                acc.setOffsetX(getOffsetX() + FLATXOFFSET);
                acc.setOffsetY(getOffsetY() + FLATYOFFSET);
            } else if (accType == 3) {
                acc.setOffsetX(getOffsetX() + SHARPXOFFSET);
                acc.setOffsetY(getOffsetY() + SHARPYOFFSET);
            }
        }
        return currAcc;
    }

    public void setParent(Note parent) {
        this.parent = parent;
    }

    public Note getParent() {
        return parent;
    }
}
