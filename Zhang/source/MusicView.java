import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import dollar.DollarRecognizer;
import dollar.Result;

public class MusicView extends JComponent{

    private ArrayList<Staff> sheet;
    private int next_x;
    private int next_y;

    private static final int INITIAL_X = 10;
    private static final int INITIAL_Y = 50;
    private static final int MEASURE_WIDTH = 240;
    private static final int STAFF_HEIGHT = 48;
    private static final int INITIAL_STAVES = 4;
    private static final int LEDGER_LINE_EXTENSION = 10;
    private static final int QUARTER_LENGTH = 400; // in ms

    private BufferedImage trebleClefImage;
    private BufferedImage commonTimeImage;
    private BufferedImage flatImage;
    private BufferedImage sharpImage;
    private BufferedImage naturalImage;
    private BufferedImage sixteenthNoteImage;
    private BufferedImage eighthNoteImage;
    private BufferedImage quarterNoteImage;
    private BufferedImage halfNoteImage;
    private BufferedImage wholeNoteImage;
    private BufferedImage sixteenthRestImage;
    private BufferedImage eightRestImage;
    private BufferedImage quarterRestImage;
    private BufferedImage halfRestImage;
    private BufferedImage wholeRestImage;

    private BufferedImage currNoteImage;
    private int noteLength; // from 0 to 4, sixteenth to whole
    private int noteType; // from 0 to 3, note, rest, flat, sharp
    private Note currNote;
    private int selectedNoteIndex;

    private boolean placingNote;
    private boolean placingAccidental;

    private ArrayList<Note> notes; // holds all notes, association with staff is through Note fields

    private ArrayList<Point2D> penPoints; // holds individual points for pen drawing
    private Point2D penStartPoint;

    private Timer playbackTimer;
    private ArrayList<NoteGroup> playbackNoteGroups;
    private int playbackIndex;
    private int playbackCurrTime;
    private int playbackInitTime;
    private boolean drawIndicator;
    private int indicatorX;
    private int indicatorY;

    private BufferedImage nextPage;
    private BufferedImage prevPage;
    private BufferedImage currPage;
    private Timer pageTurnTimer;
    private int pageTurnProgress;
    private ActionListener pageTurnListener;
    private boolean turningForward;
    private boolean turningBackward;
    private static final int PAGE_TURN_DURATION = 300;

    // Class to hold a page of music, including staves, notes, etc
    public MusicView() {
        super();
        next_x = INITIAL_X;
        next_y = INITIAL_Y;
        sheet = new ArrayList<Staff>();
        for (int i = 0; i < INITIAL_STAVES; i++) {
            addStaff();
        }
        //initial preferred size is based on 4 staves and fixed 1000 pixels (enough for one staff)
        this.setSize(new Dimension(1000, 4 * STAFF_HEIGHT * 3));
        this.setPreferredSize(this.getSize());
        
        //load images
        try {
            trebleClefImage = ImageIO.read(getClass().getResource("/images/trebleClef.png"));
            commonTimeImage = ImageIO.read(getClass().getResource("/images/commonTime.png"));
            
            flatImage = ImageIO.read(getClass().getResource("/images/flat.png"));
            sharpImage = ImageIO.read(getClass().getResource("/images/sharp.png"));
            naturalImage = ImageIO.read(getClass().getResource("/images/natural.png"));

            sixteenthNoteImage = ImageIO.read(getClass().getResource("/images/sixteenthNote.png"));
            eighthNoteImage = ImageIO.read(getClass().getResource("/images/eighthNote.png"));
            quarterNoteImage = ImageIO.read(getClass().getResource("/images/quarterNote.png"));
            halfNoteImage = ImageIO.read(getClass().getResource("/images/halfNote.png"));
            wholeNoteImage = ImageIO.read(getClass().getResource("/images/wholeNote.png"));

            sixteenthRestImage = ImageIO.read(getClass().getResource("/images/sixteenthRest.png"));
            eightRestImage = ImageIO.read(getClass().getResource("/images/eighthRest.png"));
            quarterRestImage = ImageIO.read(getClass().getResource("/images/quarterRest.png"));
            halfRestImage = ImageIO.read(getClass().getResource("/images/halfRest.png"));
            wholeRestImage = ImageIO.read(getClass().getResource("/images/wholeRest.png"));
        } catch (Exception e) {
            System.out.println("Error loading images: " + e.getMessage());
        }

        // initial setting of length and type so they a defined value
        setNoteLength(0);
        setNoteType(0);

        currNote = new Note(0, 0, noteLength, noteType, 0, currNoteImage);
        placingNote = false;
        selectedNoteIndex = -1;
        placingAccidental = false;

        notes = new ArrayList<Note>();
    }
    
    /*
     * creates a new staff on the music view
     * this will update the new end to be the new staff, update the next y to create at, and set the new size
     */
    public void addStaff() {
        if (sheet.size() > 0) {
            sheet.get(sheet.size() - 1).setEnd(false);
        }
        sheet.add(new Staff(next_x, next_y, MEASURE_WIDTH, STAFF_HEIGHT, true));
        // next_x += STAFF_WIDTH;
        // if (sheet.size() % 4 == 0) {
        //     next_x = INITIAL_X;
        //     next_y += STAFF_HEIGHT * 2;
        // } else if ((sheet.size() - 1) % 4 == 0) {
        //     this.setSize(new Dimension(this.getWidth(), next_y + STAFF_HEIGHT * 2));
        //     this.setPreferredSize(this.getSize());
        //     revalidate();
        // }
        next_y += STAFF_HEIGHT * 3;
        this.setSize(new Dimension(this.getWidth(), next_y + STAFF_HEIGHT * 3));
        this.setPreferredSize(this.getSize());
        revalidate();
        repaint();
    }
    
    /*
     * removes the last staff on the music view
     * this will update the new end to be the one at sheet.size - 1, delete all notes associated with the staff, set the new y value, and update size
     */
    public void removeStaff() {
        if (sheet.size() <= 1) return;
        sheet.remove(sheet.size() - 1);
        sheet.get(sheet.size() - 1).setEnd(true);
        // next_x -= STAFF_WIDTH;
        // if (next_x < INITIAL_X) {
        //     next_x = INITIAL_X + STAFF_WIDTH * 3;
        //     next_y -= STAFF_HEIGHT * 2;
        // }
        // if (sheet.size() % 4 == 0) {
        //     //since next_y will point to the next line, we need to subtract that difference first
        //     this.setSize(new Dimension(this.getWidth(), (next_y - STAFF_HEIGHT * 2) + STAFF_HEIGHT + 20));
        //     this.setPreferredSize(this.getSize());
        //     revalidate();
        // }
        for (int i = notes.size() - 1; i >= 0; i--) {
            Note n = notes.get(i);
            if (n.getStaffNum() >= sheet.size()) {
                notes.remove(i);
            }
        }
        next_y -= STAFF_HEIGHT * 3;
        this.setSize(new Dimension(this.getWidth(), next_y));
        this.setPreferredSize(this.getSize());
        revalidate();
        repaint();
    }

    /*
     * draws the clef and signature of the staff/staves
     * 
     */
    private void drawSignature(Graphics g) {
        // for (int i = 0; i < (sheet.size() - 1) / 4 + 1; i++) {
        //     g.drawImage(trebleClefImage, 10, i * STAFF_HEIGHT * 2, null);
        //     g.drawImage(commonTimeImage, 50, 25 + i * STAFF_HEIGHT * 2, 25, 38,null);
        // }
        for (int i = 0; i < sheet.size(); i++) {
            g.drawImage(trebleClefImage, 10, INITIAL_Y - 20 + i * STAFF_HEIGHT * 3, null);
            g.drawImage(commonTimeImage, 50, INITIAL_Y + 5 + i * STAFF_HEIGHT * 3, 25, 38,null);
        }
    }

    /*
     * function for painting the whole MusicView, including notes, staves, and other miscellaneous
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // check for page turning
        if (turningForward) {
            // interpolate between currPage and nextPage based on pageTurnProgress
            int w = currPage.getWidth();
            int h = currPage.getHeight();
            int turnPosition = (int) (w * (1 - (pageTurnProgress * 1.0 / PAGE_TURN_DURATION)));
            if (turnPosition <= 0) turnPosition = 1;
            int nextWidth = w - turnPosition;

            // these two lines fix the rendering problems with different thicknesses for staff lines
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            BufferedImage currImgPortion = currPage.getSubimage(0, 0, turnPosition, h);
            BufferedImage nextImgPortion = nextPage.getSubimage(turnPosition, 0, nextWidth, h);

            g.drawImage(currImgPortion, 0, 0, this);
            g.drawImage(nextImgPortion, turnPosition, 0, this);
            g.setColor(Color.GRAY);
            g.fillRect(turnPosition - 2, 0, 4, this.getHeight());
            return;
        } else if (turningBackward) {
            // should turn the other way (starts from left)
            int w = currPage.getWidth();
            int h = currPage.getHeight();
            int turnPosition = (int) (w * (pageTurnProgress * 1.0 / PAGE_TURN_DURATION));
            if (turnPosition <= 0) turnPosition = 1;
            else if (turnPosition >= w) turnPosition = w - 1;
            int currWidth = w - turnPosition;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            BufferedImage prevImgPortion = prevPage.getSubimage(0, 0, turnPosition, h);
            BufferedImage currImgPortion = currPage.getSubimage(turnPosition, 0, currWidth, h);

            g.drawImage(prevImgPortion, 0, 0, this);
            g.drawImage(currImgPortion, turnPosition, 0, this);
            g.setColor(Color.GRAY);
            g.fillRect(turnPosition - 2, 0, 4, this.getHeight());
            return;
        }
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        // draw signature and staff
        drawSignature(g);
        for (Staff s : sheet) {
            s.paintComponent(g);
        }
        // draw the note we are currently placing
        if (placingNote) {
            Note intersectingNote = getIntersectingNote(currNote);
            if (placingAccidental && intersectingNote != null) {
                intersectingNote.drawAccidental(g, currNote);
            } else {
                currNote.drawNote(g);
                drawLedgerLines(g, currNote);
            }
            // g.drawImage(currNoteImage, currNote.getX(), currNote.getY(), null);
        }
        // draw all notes and rests
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            // draw bounds if selecting that note or placing accidental
            if ((placingAccidental && n.getType() == 0) || i == selectedNoteIndex) {
                n.drawBounds(g);
            }
            n.drawNote(g);
            drawLedgerLines(g, n);
        }

        // draw pen strokes
        if (penPoints != null && penPoints.size() > 1) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setRenderingHints(new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            ));
            for (int i = 0; i < penPoints.size() - 1; i++) {
                Point2D p1 = penPoints.get(i);
                Point2D p2 = penPoints.get(i + 1);
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }

        // draw playback indicator
        if (drawIndicator) {
            g.setColor(Color.GREEN);
            g.fillOval(indicatorX - 5, indicatorY - 5, 10, 10);
        }
    }

    /*
     * for drawing (not selecting)
     * redraws the currNote (dragged during placement) at position x,y 
     */
    public void drawNote(int x, int y) {
        // check if we are placing a note near another note on the same staff for a chord
        snapToNearestNote(currNote, x);
        currNote.setOffsetY(y);
        placingNote = true;
        if (currNote.getType() == 2 || currNote.getType() == 3) {
            placingAccidental = true;
        }
        // int currStaff = calculateStaff(currNote.getOffsetY());
        // System.out.println(3 * STAFF_HEIGHT * currStaff + 1.5 * STAFF_HEIGHT - currNote.getOffsetY() + 2);
        repaint();
    }

    /*
     * finalize note placement, save to notes array, then create a new note to be currNote
     */
    public boolean placeNote(int x, int y) {
        // accidentals must belong to existing notes
        if (placingAccidental) {
            Note intersectingNote = getIntersectingNote(currNote);
            if (intersectingNote != null) {
                Note removed = intersectingNote.setAccidental(currNote);
                if (removed != null) {
                    notes.remove(removed);
                }
                notes.add(currNote);
            }
        } else {
            snapToNearestNote(currNote, x);
            currNote.setOffsetY(y);
            if (!inBounds(currNote)) {
                placingNote = false;
                placingAccidental = false;
                repaint();
                return false;
            }
            int staffNum = calculateStaff(currNote.getOffsetY());
            currNote.setStaffNum(staffNum);
            currNote.setPitch(sheet.get(staffNum).calculatePitch(currNote.getOffsetY()));
            notes.add(currNote);
        }

        placingNote = false;
        placingAccidental = false;

        repaint();
        return true;
    }
    
    /*
     * calculates the index of the staff for a note to belong to given its y coordinate
     * assumes staves are always vertically placed with constant spacing
     */
    public int calculateStaff(int y) {
        // Calulates the closest staff number to notes y position
        for (int i = 0; i < sheet.size(); i++) {
            if (y < (i + 1) * STAFF_HEIGHT * 3 + INITIAL_Y - 50) {
                return i;
            }
        }
        return sheet.size() - 1;
    }

    /*
     * selects the last note placed at a certain position
     * otherwise if there is no note, sets the index to -1 to indicate no selected note
     */
    public Note selectNote(int x, int y) {
        for (int i = notes.size() - 1; i >= 0; i--) {
            // we will start from the most recent notes because they are "on top"
            Note n = notes.get(i);
            if (x >= n.getX() && x <= n.getX() + n.getImage().getWidth() &&
                y >= n.getY() && y <= n.getY() + n.getImage().getHeight()) {
                selectedNoteIndex = i;
                repaint();
                return notes.get(i);
            }
        }
        selectedNoteIndex = -1;
        repaint();
        return null;
    }

    /*
     * moves the selected note to a new position without setting other values
     */
    public void moveSelectedNote(int x, int y) {
        if (selectedNoteIndex >= 0 && selectedNoteIndex < notes.size()) {
            if (notes.get(selectedNoteIndex).getType() == 2 || notes.get(selectedNoteIndex).getType() == 3) {
                return; // cannot move accidentals alone
            }
            Note n = notes.get(selectedNoteIndex);
            snapToNearestNote(n, x);
            n.setOffsetY(y);
            repaint();
        }
    }

    /*
     * finalizes placement of the selected note, setting key values
     */
    public boolean placeSelectedNote(int x, int y) {
        if (selectedNoteIndex >= 0 && selectedNoteIndex < notes.size()) {
            if (notes.get(selectedNoteIndex).getType() == 2 || notes.get(selectedNoteIndex).getType() == 3) {
                return false; // cannot move accidentals alone
            }
            Note n = notes.get(selectedNoteIndex);
            snapToNearestNote(n, x);
            n.setOffsetY(y);
            if (!inBounds(n)) {
                deleteSelectedNote();
                repaint();
                return false;
            }
            int staffNum = calculateStaff(n.getOffsetY());
            n.setStaffNum(staffNum);
            n.setPitch(sheet.get(staffNum).calculatePitch(n.getOffsetY()));
            repaint();
            return true;
        }
        return false;
    }

    /*
     * deletes the selected note and removes the selection
     */
    public boolean deleteSelectedNote() {
        if (selectedNoteIndex >= 0 && selectedNoteIndex < notes.size()) {
            // if the note has an accidental, we need to remove that first
            Note n = notes.get(selectedNoteIndex);
            if (n.getAccidental() != null) {
                notes.remove(n.getAccidental());
            } else if (n.getParent() != null) { // if we are deleting an accidental, update the parent
                n.getParent().setAccidental(null);
            }
            notes.remove(selectedNoteIndex);
            selectedNoteIndex = -1;
            repaint();
            return true;
        }
        return false;
    }

    /*
     * sets note length and image if applicable
     */
    public void setNoteLength(int length) {
        /*
         * Values are:
         * 0 - sixteenth
         * 1 - eighth
         * 2 - quarter
         * 3 - half
         * 4 - whole
         */
        this.noteLength = length;
        setNoteImage();
    }

    /*
     * sets note type and image if applicable
     */
    public void setNoteType(int type) {
        /*
         * Values are:
         * 0 - note
         * 1 - rest
         * 2 - flat
         * 3 - sharp
         */
        this.noteType = type;
        setNoteImage();
    }

    /*
     * based on type and length, set the current image used for notes to be one defined in the constructor
     */
    private void setNoteImage() {
        if (noteType == 0) { // note
            switch (noteLength) {
                case 0:
                    currNoteImage = sixteenthNoteImage;
                    break;
                case 1:
                    currNoteImage = eighthNoteImage;
                    break;
                case 2:
                    currNoteImage = quarterNoteImage;
                    break;
                case 3:
                    currNoteImage = halfNoteImage;
                    break;
                case 4:
                    currNoteImage = wholeNoteImage;
                    break;
                default:
                    currNoteImage = null;
            }
        } else if (noteType == 1) { // rest
            switch (noteLength) {
                case 0:
                    currNoteImage = sixteenthRestImage;
                    break;
                case 1:
                    currNoteImage = eightRestImage;
                    break;
                case 2:
                    currNoteImage = quarterRestImage;
                    break;
                case 3:
                    currNoteImage = halfRestImage;
                    break;
                case 4:
                    currNoteImage = wholeRestImage;
                    break;
                default:
                    currNoteImage = null;
            }
        } else if (noteType == 2) { // flat
            currNoteImage = flatImage;
        } else if (noteType == 3) { // sharp
            currNoteImage = sharpImage;
        } else {
            currNoteImage = null;
        }
        currNote = new Note(0, 0, noteLength, noteType, 0, currNoteImage); // sets the note with the specified length, type, and image
    }

    //some getters and setters
    public int getNoteDuration() {
        return noteLength;
    }

    public int getNoteType() {
        return noteType;
    }

    public int getStaffCount() {
        return sheet.size();
    }

    public Note getNote() {
        return currNote;
    }

    /*
     * wipes the current note to be the last selected length, type and image without position data
     * used after placing a note
     */
    public void resetNote() {
        currNote = new Note(0, 0, noteLength, noteType, 0, currNoteImage);
    }

    /*
     * makes the note selection invalid
     * used for changing pages, modes, etc - anywhere where we "lose focus"
     */
    public void clearSelectedNote() {
        selectedNoteIndex = -1;
        repaint();
    }

    /*
     * returns the current selection, if applicable
     */
    public Note getSelectedNote() {
        if (selectedNoteIndex >= 0 && selectedNoteIndex < notes.size()) {
            return notes.get(selectedNoteIndex);
        } else{
            return null;
        }
    }

    /*
     * Finds the highest note that is type 0 (an actual note) that intersects with the given note
     */
    private Note getIntersectingNote(Note note) {
        int x = note.getX();
        int y = note.getY();
        for (int i = notes.size() - 1; i >= 0; i--) {
            // we will start from the most recent notes because they are "on top"
            Note n = notes.get(i);
            if (x >= n.getX() && x <= n.getX() + n.getImage().getWidth() &&
                y >= n.getY() && y <= n.getY() + n.getImage().getHeight() &&
                n.getType() == 0) {
                return notes.get(i);
            }
        }
        return null;
    }

    /*
     * Finds and returns all notes that intersect with a point
     */
    private List<Integer> getIntersectingNoteIndices(int x, int y) {
        List<Integer> intersectingNoteIndices = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            if (x >= n.getX() && x <= n.getX() + n.getImage().getWidth() &&
                y >= n.getY() && y <= n.getY() + n.getImage().getHeight()) {
                intersectingNoteIndices.add(i);
            }
        }
        return intersectingNoteIndices;
    }

    /*
     * finds and returns the nearest note on the same staff that is horizontally aligned with the given note
     */
    private Note getNearestHorizontalNote(Note note) {
        int x = note.getOffsetX();
        int y = note.getOffsetY();
        int noteStaffNum = calculateStaff(y);
        Note bestNote = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Note n : notes) {
            if (n.getStaffNum() == noteStaffNum && n.getType() == 0) {
                if (n == note) continue; // skip self
                int distance = Math.abs(n.getOffsetX() - x);
                if (x >= n.getX() && x <= n.getX() + n.getImage().getWidth() && distance < bestDistance) {
                    bestDistance = distance;
                    bestNote = n;
                }
            }
        }
        return bestNote;
    }

    /*
     * Given the next x position of the note, snap the note to the nearest note if it is close enough
     * must be on the same staff and horizontally aligned
     * see getNearestHorizontalNote for more details
     */
    private void snapToNearestNote(Note note, int x) {
        note.setOffsetX(x);
        Note nearest = getNearestHorizontalNote(note);
        if (nearest != null && note.getType() == 0) {
            note.setOffsetX(nearest.getOffsetX());
        }
    }


    /*
     * draws up to two ledger lines for notes that are above/below the staff proper
     * only draws for actual notes (type 0)
     */
    private void drawLedgerLines(Graphics g, Note n) {
        if (n.getType() != 0) return;
        int staffNum = calculateStaff(n.getOffsetY());
        // calculate the y position within the staff
        int staffMiddle = (int) (3 * STAFF_HEIGHT * staffNum + 1.5 * STAFF_HEIGHT) + 2;
        int relativeY = staffMiddle - n.getOffsetY(); // relative position of the note to middle line, positive is above
        int spacing = STAFF_HEIGHT / 4;
        int extraWidth = n.getDuration() == 4 ? 3 : 0; // get extra width for whole notes
        g.setColor(Color.BLACK);
        
        // given the relative y position from the middle of the staff, we want ledger lines to show when we get close enough
        // this means that when we see ledger lines, we are placing notes based on the ledger lines (if the top lines show, we are placing at least A5)
        if (relativeY > 0) {
            if (relativeY >= 2.75 * spacing + 1) {
                g.drawLine(n.getOffsetX() - extraWidth - LEDGER_LINE_EXTENSION, staffMiddle - 3 * spacing,
                           n.getOffsetX() + extraWidth + LEDGER_LINE_EXTENSION, staffMiddle - 3 * spacing);
            }
            if (relativeY >= 3.75 * spacing + 1) {
                g.drawLine(n.getOffsetX() - extraWidth - LEDGER_LINE_EXTENSION, staffMiddle - 4 * spacing,
                           n.getOffsetX() + extraWidth + LEDGER_LINE_EXTENSION, staffMiddle - 4 * spacing);
            }
        } else {
            if (relativeY <= -2.75 * spacing - 1) {
                g.drawLine(n.getOffsetX() - extraWidth - LEDGER_LINE_EXTENSION, staffMiddle + 3 * spacing,
                           n.getOffsetX() + extraWidth + LEDGER_LINE_EXTENSION, staffMiddle + 3 * spacing);
            }
            if (relativeY <= -3.75 * spacing - 1) {
                g.drawLine(n.getOffsetX() - extraWidth - LEDGER_LINE_EXTENSION, staffMiddle + 4 * spacing,
                           n.getOffsetX() + extraWidth + LEDGER_LINE_EXTENSION, staffMiddle + 4 * spacing);
            }
        }
    }

    /*
     * returns an array of note groups for playback
     * this is derived from the positioning of the notes (from the notes arraylist) and their durations + pitch
     * note groups are sorted by start time and made of playable notes, which contain duration and pitch in format ready to play
     * notes are put in the same group if their offset x position and staff are the same
     */
    public ArrayList<NoteGroup> getNoteGroups() {
        // first we need to convert the notes in the notes array to NoteGroups
        // we can sort as we are adding, meaning a optimal O(n) insertion sort
        ArrayList<NoteGroup> noteGroups = new ArrayList<NoteGroup>();
        Map<String, Integer> durationMap = Map.of(
            "sixteenth", QUARTER_LENGTH / 4,
            "eighth", QUARTER_LENGTH / 2,
            "quarter", QUARTER_LENGTH,
            "half", QUARTER_LENGTH * 2,
            "whole", QUARTER_LENGTH * 4,
            "unknown", -1
        );
        for (Note n : notes) {
            if (n.getType() == 2 || n.getType() == 3) continue; // skip accidentals
            if (n.getType() == 0 && n.getPitch().substring(0, 2).equals("un")) continue; // skip unknown
            String basePitch = n.getPitch().substring(0, 2);
            String pitch = n.getPitch();
            // for handling notes that aren't supported by the MIDI player
            if (basePitch.equals("E#")) {
                pitch = "F" + n.getPitch().substring(2);
            } else if (basePitch.equals("B#")) {
                pitch = "C" + (Integer.parseInt(n.getPitch().substring(2)) + 1);
            } else if (basePitch.equals("Fb")) {
                pitch = "E" + n.getPitch().substring(2);
            } else if (basePitch.equals("Cb")) {
                pitch = "B" + (Integer.parseInt(n.getPitch().substring(2)) - 1);
            }
            PlayableNote pNote = new PlayableNote(pitch, durationMap.get(n.getDurationString()));

            int ordering = n.getOffsetX() + n.getStaffNum() * 10000;
            // go through existing note groups, find the one that matches ordering or create a new one if we reach higher ordering
            for (int i = 0; i < noteGroups.size(); i++) {
                NoteGroup ng = noteGroups.get(i);
                if (ng.getOrdering() == ordering) {
                    ng.addNote(pNote);
                    break;
                } else if (ng.getOrdering() > ordering) { // insert the new note group before this note group
                    noteGroups.add(i, new NoteGroup(pNote, durationMap.get(n.getDurationString()), ordering, n.getOffsetX(), calculateIndicatorY(n.getStaffNum()))); // insert at i, pushing the ith element back
                    break;
                } else if (i == noteGroups.size() - 1) { // we are at the end, and current note ordering is greater than all existing
                    noteGroups.add(new NoteGroup(pNote, durationMap.get(n.getDurationString()), ordering, n.getOffsetX(), calculateIndicatorY(n.getStaffNum()))); // add to the end
                    break;
                }
            }
            // if the size is zero, the for loop won't run, so we need to add manually
            if (noteGroups.size() == 0) {
                noteGroups.add(new NoteGroup(pNote, durationMap.get(n.getDurationString()), ordering, n.getOffsetX(), calculateIndicatorY(n.getStaffNum())));
            }
        }

        // now given the ordering of all the note groups, we need to determine when they start playing
        int currTime = 0;
        for (NoteGroup ng : noteGroups) {
            ng.setStartTime(currTime);
            currTime += ng.getDuration();
        }
        playbackNoteGroups = noteGroups;
        return noteGroups;
    }

    /*
     * checks if a note in in bounds
     * this means that it should be within the music view itself and close enough to a staff to be placed
     */
    private boolean inBounds(Note n) {
        if (n.getOffsetX() < INITIAL_X || n.getOffsetX() > this.getWidth() - 10) return false;
        if (n.getOffsetY() < 0 || n.getOffsetY() > next_y - STAFF_HEIGHT) return false;
        return true;
    }


    /*
     * three methods for pen drawing - one to start, one to add points continuously, and one to stop and analyze the notes
     */
    public void startDrawing(int x, int y) {
        penPoints = new ArrayList<Point2D>();
        penPoints.add(new Point2D.Double(x, y));
        penStartPoint = new Point2D.Double(x, y);
        repaint();
    }

    /*
     * basically just adds a new point to the list
     */
    public void draw(int x, int y) {
        penPoints.add(new Point2D.Double(x, y));
        repaint();
    }

    public Note stopDrawing() {
        // analyze points and create notes

        // check for scratchout pattern
        int numTurns = 0;
        int hDist = 0;
        int vDist = 0;
        int currDirection = 0; // 1 is right, -1 is left, 0 is start

        for (int i = 0; i < penPoints.size() - 1; i++) {
            // find horizontal difference, check if negative or positive, increment num turns if needed, then add to total distance
            Point2D p1 = penPoints.get(i);
            Point2D p2 = penPoints.get(i + 1);
            int deltaX = (int) (p2.getX() - p1.getX()); // positive is right, negative is left
            if (currDirection == 0) {
                if (deltaX > 0) {
                    currDirection = 1;
                } else if (deltaX < 0) {
                    currDirection = -1;
                }
            } else if (currDirection >= 1 && deltaX < 0) { // going right and turn left
                currDirection = -1;
                ++numTurns;
            } else if (currDirection <= -1 && deltaX > 0){ // going left and turn right
                currDirection = 1;
                ++numTurns;
            }

            int deltaY = (int) (p2.getY() - p1.getY());
            hDist += Math.abs(deltaX);
            vDist += Math.abs(deltaY);
        }

        if (vDist == 0) vDist = 1;

        float horizontalRatio = (float) hDist / (float) vDist;
        if (numTurns >= 2 && horizontalRatio >= 4.0f) { // scratch out the notes
            int count = 0;
            // go along points, check for overlapping notes
            for (Point2D p : penPoints) {
                int pointX = (int) p.getX();
                int pointY = (int) p.getY();
                List<Integer> intersectingNoteIndices = getIntersectingNoteIndices(pointX, pointY);

                for (int index : intersectingNoteIndices) {
                    Note n = notes.get(index);
                    // if the note has an accidental, we need to remove that first
                    if (n.getAccidental() != null) {
                        notes.remove(n.getAccidental());
                        ++count;
                    } else if (n.getParent() != null) { // if we are deleting an accidental, update the parent
                        n.getParent().setAccidental(null);
                    }
                    notes.remove(index);
                    ++count;
                }
            }

            penPoints = null;
            repaint();
            return new Note(-1, -1, -1, -1, count, null); // dummy note for scratchout
        }

        // pass to recognizer
        DollarRecognizer recognizer = new DollarRecognizer();
        Result result = recognizer.recognize(penPoints);
        Note createdNote = null;
        if (result.getScore() > 0.6) { // if the match is good enough, place note
            String resultName = result.getName();
            createdNote = new Note(0, 0, 0, 0, 0, null);
            switch (resultName) {
                case "circle":
                    createdNote.setDuration(4);
                    createdNote.setType(0);
                    createdNote.setImage(wholeNoteImage);
                    break;
                case "half note":
                    createdNote.setDuration(3);
                    createdNote.setType(0);
                    createdNote.setImage(halfNoteImage);
                    break;
                case "quarter note":
                    createdNote.setDuration(2);
                    createdNote.setType(0);
                    createdNote.setImage(quarterNoteImage);
                    break;
                case "eighth note":
                    createdNote.setDuration(1);
                    createdNote.setType(0);
                    createdNote.setImage(eighthNoteImage);
                    break;
                case "sixteenth note":
                    createdNote.setDuration(0);
                    createdNote.setType(0);
                    createdNote.setImage(sixteenthNoteImage);
                    break;
                case "rectangle":
                    createdNote.setDuration(4);
                    createdNote.setType(1);
                    createdNote.setImage(wholeRestImage);
                    break;
                case "half rest":
                    createdNote.setDuration(3);
                    createdNote.setType(1);
                    createdNote.setImage(halfRestImage);
                    break;
                case "right curly brace":
                    createdNote.setDuration(2);
                    createdNote.setType(1);
                    createdNote.setImage(quarterRestImage);
                    break;
                case "eighth rest":
                    createdNote.setDuration(1);
                    createdNote.setType(1);
                    createdNote.setImage(eightRestImage);
                    break;
                case "sixteenth rest":
                    createdNote.setDuration(0);
                    createdNote.setType(1);
                    createdNote.setImage(sixteenthRestImage);
                    break;
                case "flat":
                    // need to check for existing note to attach to underneath
                    createdNote.setType(2);
                    createdNote.setOffsetX((int) penStartPoint.getX());
                    createdNote.setOffsetY((int) penStartPoint.getY());
                    createdNote.setImage(flatImage);
                    Note intersectingNote = getIntersectingNote(createdNote);
                    if (intersectingNote != null) {
                        Note removed = intersectingNote.setAccidental(createdNote);
                        if (removed != null) {
                            notes.remove(removed);
                        }
                        notes.add(createdNote);
                    }
                    penPoints = null;
                    repaint();
                    return createdNote;
                case "star":
                    // need to check for existing note to attach to underneath
                    createdNote.setType(3);
                    createdNote.setOffsetX((int) penStartPoint.getX());
                    createdNote.setOffsetY((int) penStartPoint.getY());
                    createdNote.setImage(sharpImage);
                    intersectingNote = getIntersectingNote(createdNote);
                    if (intersectingNote != null) {
                        Note removed = intersectingNote.setAccidental(createdNote);
                        if (removed != null) {
                            notes.remove(removed);
                        }
                        notes.add(createdNote);
                    }
                    penPoints = null;
                    repaint();
                    return createdNote;
                default:
                    createdNote = null; // unrecognized symbol
            }
        } else {
            createdNote = null;
        }
        if (createdNote != null) {
            createdNote.setOffsetX((int) penStartPoint.getX());
            createdNote.setOffsetY((int) penStartPoint.getY());

            createdNote.setStaffNum(calculateStaff(createdNote.getOffsetY()));
            createdNote.setPitch(sheet.get(createdNote.getStaffNum()).calculatePitch(createdNote.getOffsetY()));
            notes.add(createdNote);
        }
        penPoints = null;
        repaint();
        return createdNote;
    }

    // start the playback indicator animation
    // this does get out of sync with the sound
    public void startIndicator() {
        /*
         * Find all positions with lengths of notes to be played
         * iterate through notes and animate with timer.
         * 
         * We can reuse the note groups for calculating the positions, then iterate through
         */
        // use playback note groups if exists
        if (playbackNoteGroups == null) {
            playbackNoteGroups = getNoteGroups();
        }
        if (playbackNoteGroups.size() == 0) return; // nothing to play
        drawIndicator = true;
        playbackIndex = 0;
        playbackCurrTime = playbackNoteGroups.get(0).getDuration();
        playbackInitTime = playbackCurrTime;
        playbackTimer = new Timer(10, null);
        playbackTimer.addActionListener(e -> {
            // check if we need to move to the next note group
            // check if in bounds of the array if we are moving to a new group
            if (playbackCurrTime <= 0) {
                playbackIndex++;
                if (playbackIndex < playbackNoteGroups.size()) {
                    playbackCurrTime = playbackNoteGroups.get(playbackIndex).getDuration();
                    playbackInitTime = playbackCurrTime;
                } else {
                    playbackTimer.stop();
                    drawIndicator = false;
                    repaint();
                    return;
                }
            }
            playbackCurrTime -= 10;
            // set the indicator position based on scale betwen current time and intial time
            // linear interpolation? something else maybe idk
            indicatorX = playbackNoteGroups.get(playbackIndex).getXPos();
            indicatorY = playbackNoteGroups.get(playbackIndex).getYPos();
            int verticalMovement = STAFF_HEIGHT * 2/5;
            
            // first half of the duratino move down, second half move up
            if (playbackInitTime / 2 <= playbackCurrTime) {
                int progressDown = (int) (verticalMovement * (playbackInitTime - playbackCurrTime) / (playbackInitTime / 2.0f));
                indicatorY = indicatorY + progressDown;
            } else {
                int progressUp = (int) (verticalMovement * -((playbackInitTime / 2) - playbackCurrTime) / (playbackInitTime / 2.0f));
                indicatorY = indicatorY + verticalMovement + progressUp;
            }
            repaint();
        });
        playbackTimer.start();
    }

    // stop the playback indicator
    public void stopIndicator() {
        if (playbackTimer != null) {
            playbackTimer.stop();
        }
        drawIndicator = false;
        repaint();
    }

    // given a staff number, calculate the starting y position for the playback indicator
    public int calculateIndicatorY(int staffNum) {
        return staffNum * STAFF_HEIGHT * 3 + INITIAL_Y / 5;
    }

    // animation for turning to the next page (forward direction)
    public void nextPage(BufferedImage curr, BufferedImage next) {
        currPage = curr;
        nextPage = next;
        pageTurnTimer = new Timer(10, null);
        pageTurnProgress = 0;
        turningForward = true;
        turningBackward = false;
        pageTurnTimer.addActionListener(e -> {
            // page turn animation by drawing subimages of the buffered images
            if (pageTurnProgress >= PAGE_TURN_DURATION) {
                finishPageTurn();
                repaint();
                return;
            }
            // curr image gets replaced from the right to left, next grows from the right to left
            // *this is actually handled in the paintComponent method, but parameters are updated here
            pageTurnProgress += 10;
            repaint();
        });
        pageTurnTimer.start();
    }

    // same as previous function, just the opposite direction for the animation
    public void previousPage(BufferedImage curr, BufferedImage previous) {
        currPage = curr;
        prevPage = previous;
        pageTurnTimer = new Timer(10, null);
        pageTurnProgress = 0;
        turningBackward = true;
        turningForward = false;
        pageTurnTimer.addActionListener(e -> {
            // page turn animation by drawing subimages of the buffered images
            if (pageTurnProgress >= PAGE_TURN_DURATION) {
                finishPageTurn();
                repaint();
                return;
            }
            // curr image gets replaced from the left to right, previous grows from the left to right
            // *this is actually handled in the paintComponent method, but parameters are updated here
            pageTurnProgress += 10;
            repaint();
        });
        pageTurnTimer.start();
    }

    // for the MusicEditor to use, attach listener that actually changes the page
    public void setPageTurnListener(ActionListener listener) {
        this.pageTurnListener = listener;
    }

    // finishes page turn by stopping timer, notifying listener, and resetting private variables
    private void finishPageTurn() {
        if (pageTurnTimer != null) {
            pageTurnTimer.stop();
        }
        if (pageTurnListener != null) {
            pageTurnListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "pageTurned"));
        }
        turningBackward = false;
        turningForward = false;
        pageTurnProgress = 0;
    }
}
