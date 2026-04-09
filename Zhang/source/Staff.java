import javax.swing.JComponent;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

// class for a single staff
public class Staff extends JComponent{

    private int x;
    private int y;
    private boolean isEnd; // defines if it needs bars or not
    private int measureWidth; // I am using 4 measures per staff
    private int h;

    public Staff(int x, int y, int measureWidth, int h, boolean end) {
        super();
        this.x = x;
        this.y = y;
        this.isEnd = end;
        this.measureWidth = measureWidth;
        this.h = h;
        this.setBounds(x, y, measureWidth * 4, h);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        int spacing = h / 4; // 0 to h in loop
        for (int j = 0; j < 4; j++) {
            int xpos = x + j * measureWidth;
            int w = measureWidth;
            for (int i = 0; i <= 4; i++) {
                int ypos = this.y + i * spacing; // iteratively increase y for each line based on spacing
                g.drawLine(xpos, ypos, xpos + w, ypos); // from x,ypos to x+w,ypos
            }

            //vertical lines
            g.drawLine(xpos, y, xpos, y+h);

            // left line (thick or not)
            if (j == 3 && isEnd) {
                g.drawLine(xpos+w - 8, y, xpos+w - 8, y+h);
                g.fillRect(xpos+w - 4, y, 5, h);
            } else {
                g.drawLine(xpos+w, y, xpos+w, y+h);
            }
        }
    }

    /*
     * changes whether or not a staff is at the end (and thus needs the thick line)
     * used when updating number of staves
     */
    public void setEnd(boolean end) {
        this.isEnd = end;
    }

    /*
     * calculates the pitch of a note based on how far it is from the middle line of the staff (0 = B4)
     * otherwise returns a unknown pitch if it is too high or low
     */
    public String calculatePitch(int y) {
        // System.out.println("Relative offset:" + (y - this.y));
        int diff = y - this.y;
        // System.out.println("staff difference: " + diff);
        if (diff < -30) {
            // System.out.println("Too high");
            return "unknown (too high)";
        } else if (diff >= -33 && diff <= -27) {
            // System.out.println("D6");
            return "D6";
        } else if (diff >= -26 && diff <= -22) {
            // System.out.println("C6");
            return "C6";
        } else if (diff >= -21 && diff <= -15) {
            // System.out.println("B5");
            return "B5";
        } else if (diff >= -14 && diff <= -10) {
            // System.out.println("A5");
            return "A5";
        } else if (diff >= -9 && diff <= -3) { // ledger lines start above here (A5-D6)
            // System.out.println("G5");
            return "G5";
        } else if (diff >= -2 && diff <= 2) {
            // System.out.println("F5");
            return "F5";
        } else if (diff >= 3 && diff <= 9) {
            // System.out.println("E5");
            return "E5";
        } else if (diff >= 10 && diff <= 14) {
            // System.out.println("D5");
            return "D5";
        } else if (diff >= 15 && diff <= 21) {
            // System.out.println("C5");
            return "C5";
        } else if (diff >= 22 && diff <= 26) {
            // System.out.println("B4");
            return "B4";
        } else if (diff >= 27 && diff <= 33) {
            // System.out.println("A4");
            return "A4";
        } else if (diff >= 34 && diff <= 38) {
            // System.out.println("G4");
            return "G4";
        } else if (diff >= 39 && diff <= 45) {
            // System.out.println("F4");
            return "F4";
        } else if (diff >= 46 && diff <= 50) {
            // System.out.println("E4");
            return "E4";
        } else if (diff >= 51 && diff <= 57) {
            // System.out.println("D4");
            return "D4";
        } else if (diff >= 58 && diff <= 62) {
            // System.out.println("C4");
            return "C4";
        } else if (diff >= 63 && diff <= 69) {
            // System.out.println("B3");
            return "B3";
        } else if (diff >= 70 && diff <= 74) {
            // System.out.println("A3");
            return "A3";
        } else if (diff >= 75 && diff <= 81) {
            // System.out.println("G3");
            return "G3";
        }
        return "unknown (too low)";
    }
}
