import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Image;
import java.util.Hashtable;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;

public class MusicEditor {
    // a bunch of variables that need to be edited by outside functions, may move all components here in future
    private ArrayList<MusicView> musicViews = new ArrayList<MusicView>();
    private int currPage = 1;
    private JLabel statusLabel;
    // private JLabel mainLabel;
    private JMenuItem menuDeleteStaff;
    private JButton deleteStaff;
    private JLabel pageLabel;
    private String pageDescription;
    private String staffDescription;
    private JButton deletePage;
    private JMenuItem menuDeletePage;
    private JButton next;
    private JButton previous;
    private JMenuItem menuNext;
    private JMenuItem menuPrevious;
    private MusicView currMusicView;
    private JScrollPane mainScrollPane;

    // fields for moving notes
    private boolean selecting = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    //fields for sending note info to MusicViews
    private int noteLength = 0;
    private int noteType = 0;
    private int originalNoteLength = 0;

    // fields for playback
    private boolean playing = false;
    private boolean stopRequested = false;
    private MIDI_Player player = new MIDI_Player();
    private Thread playThread;

    private boolean penning = false;


    public MusicEditor() {
        // overall frame with border layout
        JFrame frame = new JFrame("Music Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // create menu
        JMenuBar menuBar;
        JMenu file, edit, view;

        menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        // file with exit
        file = new JMenu("File");
        menuBar.add(file);
        JMenuItem menuExit = new JMenuItem("Exit", KeyEvent.VK_Q);
        file.add(menuExit);
        
        // edit with new/delete staff, new/delete page
        edit = new JMenu("Edit");
        menuBar.add(edit);
        JMenuItem menuNewStaff = new JMenuItem("New Staff", KeyEvent.VK_S);
        edit.add(menuNewStaff);
        menuDeleteStaff = new JMenuItem("Delete Staff", KeyEvent.VK_D);
        edit.add(menuDeleteStaff);
        JMenuItem menuNewPage = new JMenuItem("New Page", KeyEvent.VK_P);
        edit.add(menuNewPage);
        menuDeletePage = new JMenuItem("Delete Page", KeyEvent.VK_L);
        edit.add(menuDeletePage);

        // view with next/previous page
        view = new JMenu("View");
        menuBar.add(view);
        menuNext = new JMenuItem("Next", KeyEvent.VK_N);
        view.add(menuNext);
        menuPrevious = new JMenuItem("Previous", KeyEvent.VK_P);
        view.add(menuPrevious);


        
        // left panel (tools) ------------------------------
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        wrapper.setMinimumSize(new Dimension(100, 200));
        JPanel toolPanel = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        toolPanel.setLayout(gbl);
        // "disabling" the scroll because not needed any longer
        JScrollPane toolScroll = new JScrollPane(wrapper, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        wrapper.add(toolPanel);
        frame.add(toolScroll, BorderLayout.WEST);

        // panel label
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=2;
        toolPanel.add(new JLabel("Tools"), c);

        // select button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=2;
        c.insets = new Insets(0, 0, 0, 5);
        ImageIcon selectIcon = createImageIcon("/images/select.png");
        JToggleButton select = new JToggleButton("Select", selectIcon);
        toolPanel.add(select, c);
        // pen button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=2;
        c.insets = new Insets(0, 5, 0, 0);
        ImageIcon penIcon = createImageIcon("/images/pen.png");
        JToggleButton pen = new JToggleButton("Pen", penIcon);
        toolPanel.add(pen, c);

        // new staff button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=4;
        c.insets = new Insets(0, 0, 0, 5);
        ImageIcon newStaffIcon = createImageIcon("/images/plus.png");
        JButton newStaff = new JButton("New Staff", newStaffIcon);
        toolPanel.add(newStaff, c);
        // delete staff button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=4;
        c.insets = new Insets(0, 5, 0, 0);
        ImageIcon deleteStaffIcon = createImageIcon("/images/remove.png");
        deleteStaff = new JButton("Delete Staff", deleteStaffIcon);
        toolPanel.add(deleteStaff, c);
        
        // play button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=6;
        c.insets = new Insets(0, 0, 0, 5);
        ImageIcon playIcon = createImageIcon("/images/play.png");
        JButton play = new JButton("Play", playIcon);
        toolPanel.add(play, c);
        // stop button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=6;
        c.insets = new Insets(0, 5, 0, 0);
        ImageIcon stopIcon = createImageIcon("/images/stop.png");
        JButton stop = new JButton("Stop", stopIcon);
        stop.setEnabled(false);
        toolPanel.add(stop, c);

        // new page button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=8;
        c.insets = new Insets(0, 5, 0, 0);
        ImageIcon newPageIcon = createImageIcon("/images/add-page.png");
        JButton newPage = new JButton("New Page", newPageIcon);
        toolPanel.add(newPage, c);
        // delete page button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=8;
        c.insets = new Insets(0, 0, 0, 5);
        ImageIcon deletePageIcon = createImageIcon("/images/delete-page.png");
        deletePage = new JButton("Delete Page", deletePageIcon);
        toolPanel.add(deletePage, c);

        // next button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=10;
        c.insets = new Insets(0, 5, 0, 0);
        ImageIcon nextIcon = createImageIcon("/images/next.png");
        next = new JButton("Next", nextIcon);
        toolPanel.add(next, c);
        // previous button
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=10;
        c.insets = new Insets(0, 0, 0, 5);
        ImageIcon previousIcon = createImageIcon("/images/previous.png");
        previous = new JButton("Previous", previousIcon);
        toolPanel.add(previous, c);

        // radio buttons
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=12;
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        // adding each button to group and paenl
        JRadioButton radioNote = new JRadioButton("Note");
        JRadioButton radioRest = new JRadioButton("Rest");
        JRadioButton radioFlat = new JRadioButton("Flat");
        JRadioButton radioSharp = new JRadioButton("Sharp");
        ButtonGroup radios = new ButtonGroup();
        radios.add(radioNote);
        radios.add(radioRest);
        radios.add(radioFlat);
        radios.add(radioSharp);
        radioPanel.add(radioNote);
        radioPanel.add(radioRest);
        radioPanel.add(radioFlat);
        radioPanel.add(radioSharp);
        toolPanel.add(radioPanel, c);
        radioNote.setSelected(true);

        // slider
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=12;
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 4, 0);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        // mapping slider values to labels (from which we can find strings)
        // useful for later when we need to find the string value of the slider
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(Integer.valueOf(0), new JLabel("Sixteenth"));
        labelTable.put(Integer.valueOf(1), new JLabel("Eight"));
        labelTable.put(Integer.valueOf(2), new JLabel("Quarter"));
        labelTable.put(Integer.valueOf(3), new JLabel("Half"));
        labelTable.put(Integer.valueOf(4), new JLabel("Whole"));
        slider.setLabelTable(labelTable);
        slider.setSnapToTicks(true); // no inbetween values
        slider.setMinimumSize(new Dimension(40, 80)); // prevent slider from getting too squished
        slider.setPreferredSize(new Dimension(40, 120));
        toolPanel.add(slider, c);

        // page count
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.gridy=13;
        c.insets = new Insets(10, 0, 0, 0);
        pageLabel = new JLabel("Page: " + musicViews.size());
        toolPanel.add(pageLabel, c);

        // separators
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=1;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 10, 0);
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(Color.GRAY);
        // for loop to fill in every other row
        for (int i = 1; i < 13; i+=2) {
            c.gridy = i;
            toolPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);
        }
        //set all columns to be same width
        int maxWidth = 0;
        for (Component component : toolPanel.getComponents()) {
            int width = (int) component.getPreferredSize().getWidth();
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        gbl.columnWidths = new int[2];
        gbl.columnWidths[0] = maxWidth;
        gbl.columnWidths[1] = maxWidth;

        
        // main panel with label and scroll ------------------------
        // JPanel mainPanel = new JPanel(new GridBagLayout());
        // mainLabel = new JLabel();
        // mainLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //for padding
        // // mainPanel.add(mainLabel);
        // MusicView musicView = new MusicView();
        // mainPanel.add(musicView);
        // //mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        // JScrollPane mainScrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        // mainScrollPane.setMinimumSize(new Dimension(200, 200));
        // //mainScrollPane.setPreferredSize(new Dimension(400, 300));
        // frame.add(mainScrollPane, BorderLayout.CENTER);
        
        // main view now has the jscrollpane with one musicview inside it
        musicViews.add(new MusicView());
        currMusicView = musicViews.get(0);
        attachNoteListener(currMusicView);
        mainScrollPane = new JScrollPane(musicViews.get(0), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        mainScrollPane.setMinimumSize(new Dimension(200, 200));
        frame.add(mainScrollPane, BorderLayout.CENTER);

        // initial calls to populate staffCounts and update the text to be correct
        PageUpdate(0);


        // bottom panel just label
        JPanel statusPanel = new JPanel();
        statusLabel = new JLabel("Status Bar");
        statusPanel.add(statusLabel);
        frame.add(statusPanel, BorderLayout.SOUTH);


        // action listeners
        // all ALs are here just for organization
        // menu ---------------------------------------
        menuExit.addActionListener(e -> {
            System.exit(0);
        });

        menuNewStaff.addActionListener(e -> {
            StaffUpdate(1);
            statusLabel.setText("Staff count updated to " + musicViews.get(currPage - 1).getStaffCount());
        });

        menuDeleteStaff.addActionListener(e -> {
            StaffUpdate(-1);
            statusLabel.setText("Staff count updated to " + musicViews.get(currPage - 1).getStaffCount());
        });

        menuNewPage.addActionListener(e -> {
            PageUpdate(1);
            statusLabel.setText("Page added");
        });

        menuDeletePage.addActionListener(e -> {
            PageUpdate(-1);
            statusLabel.setText("Page deleted");
        });

        menuNext.addActionListener(e -> {
            startNextPage();
            statusLabel.setText("Next Page");
        });

        menuPrevious.addActionListener(e -> {
            startPrevPage();
            statusLabel.setText("Previous Page");
        });

        // tools panel -----------------------------------------
        select.addActionListener(e -> {
            //selecting sets behaviors for click on the main panel, see attachNoteListener
            selecting = select.isSelected();
            pen.setSelected(false);
            if (selecting) {
                statusLabel.setText("Select mode enabled");
            } else{
                currMusicView.clearSelectedNote();
                statusLabel.setText("Select mode disabled");
            }
            pen.setSelected(false);
            penning = pen.isSelected();
        });

        pen.addActionListener(e -> {
            statusLabel.setText("Pen pressed");
            penning = pen.isSelected();
            if (penning) {
                statusLabel.setText("Pen mode enabled");
            } else {
                statusLabel.setText("Pen mode disabled");
            }
            select.setSelected(false);
            selecting = select.isSelected();
            currMusicView.clearSelectedNote();
        });

        newStaff.addActionListener(e -> {
            StaffUpdate(1);
            statusLabel.setText("New Staff pressed - staff count updated to " + musicViews.get(currPage - 1).getStaffCount());
        });

        deleteStaff.addActionListener(e -> {
            StaffUpdate(-1);
            statusLabel.setText("Delete Staff pressed - staff count updated to " + musicViews.get(currPage - 1).getStaffCount());
        });

        /*
         * play button plays notes given by the musicView compiling notes together
         * uses threads to synchronize note groups (chords) stops another embedded thread to stop each note when it ends
         */
        play.addActionListener(e -> {
            statusLabel.setText("Play pressed");
            play.setEnabled(false);
            stop.setEnabled(true);
            ArrayList<NoteGroup> toPlay = currMusicView.getNoteGroups();
            stopRequested = false;
            playing = true;
            // for (NoteGroup ng: toPlay) {
            //     System.out.println("NoteGroup start time: " + ng.getStartTime() + " duration: " + ng.getDuration());
            //     for (PlayableNote pn : ng.getNotes()) {
            //         System.out.println("\tNote in group: " + pn.getPitch() + " duration: " + pn.getDuration());
            //     }
            // }
            playThread = new Thread(() -> {
                for (NoteGroup ng : toPlay) {
                    if (stopRequested) break;
                    for (PlayableNote pn : ng.getNotes()) {
                        // System.out.println("Playing note: " + pn.getPitch());
                        new Thread(() -> {
                            player.playMidiSound(pn.getPitch());
                            try {
                                Thread.sleep(pn.getDuration());
                            } catch (InterruptedException ex) {
                                
                            }
                            player.stopMidiSound(pn.getPitch());
                        }).start();
                    }
                    try {
                        Thread.sleep(ng.getDuration());
                    } catch (InterruptedException ex) {
                        
                    }
                }
                playing = false;
                stopRequested = false;
                stop.setEnabled(false);
                play.setEnabled(true);
            });
            playThread.start();
            currMusicView.startIndicator();
        });

        // requests a stop to playback at the next possible moment
        // disables stop button and enables play button
        stop.addActionListener(e -> {
            statusLabel.setText("Stop pressed");
            stopRequested = true;
            for (NoteGroup ng : currMusicView.getNoteGroups()) {
                for (PlayableNote pn : ng.getNotes()) {
                    player.stopMidiSound(pn.getPitch());
                }
            }
            stop.setEnabled(false);
            play.setEnabled(true);
            currMusicView.stopIndicator();
        });

        radioNote.addActionListener(e -> {
            noteType = 0;
            currMusicView.setNoteType(noteType);
            statusLabel.setText("Note selected");
        });

        // for the drag function, saves the original note length so all dragging is relative
        radioNote.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                originalNoteLength = noteLength;
            }
        });
        // drag function, will check the radioNote as well as adjust length with dragging
        radioNote.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                radioNote.doClick();
                int yDiff = e.getY() - radioNote.getY() - radioNote.getHeight()/2;
                int movement = yDiff / 20;

                int newLength = originalNoteLength - movement;
                if (newLength < 0)
                    newLength = 0;
                else if (newLength > 4)
                    newLength = 4;
                noteLength = newLength;

                slider.setValue(noteLength);                
                currMusicView.setNoteLength(noteLength);
            }
            public void mouseMoved(MouseEvent e) {
                // nothing
            }
        });

        radioRest.addActionListener(e -> {
            noteType = 1;
            currMusicView.setNoteType(noteType);
            statusLabel.setText("Rest selected");
        });

        // for the drag function, saves the original note length so all dragging is relative
        radioRest.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                originalNoteLength = noteLength;
            }
        });
        // drag function, will check the radioRest as well as adjust length with dragging
        radioRest.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                radioRest.doClick();
                int yDiff = e.getY() - radioRest.getY() - radioRest.getHeight()/2;
                int movement = yDiff / 20;

                int newLength = originalNoteLength - movement;
                if (newLength < 0)
                    newLength = 0;
                else if (newLength > 4)
                    newLength = 4;
                noteLength = newLength;

                slider.setValue(noteLength);                
                currMusicView.setNoteLength(noteLength);
            }
            public void mouseMoved(MouseEvent e) {
                // nothing
            }
        });

        radioFlat.addActionListener(e -> {
            noteType = 2;
            currMusicView.setNoteType(noteType);
            statusLabel.setText("Flat selected");
        });

        radioSharp.addActionListener(e -> {
            noteType = 3;
            currMusicView.setNoteType(noteType);
            statusLabel.setText("Sharp selected");
        });

        slider.addChangeListener(e -> {
            int value = slider.getValue();
            noteLength = value;
            currMusicView.setNoteLength(noteLength);
            JLabel label = labelTable.get(value);
            String noteType = label.getText();
            statusLabel.setText(noteType + " selected");
        });

        newPage.addActionListener(e -> {
            PageUpdate(1);
            statusLabel.setText("Page added");
        });

        deletePage.addActionListener(e -> {
            PageUpdate(-1);
            statusLabel.setText("Page deleted");
        });

        next.addActionListener(e -> {
            startNextPage();
            statusLabel.setText("Next Page");
        });

        previous.addActionListener(e -> {
            startPrevPage();
            statusLabel.setText("Previous Page");
        });

        // because there is only one page at first, prevent any page changing controls at first
        next.setEnabled(false);
        previous.setEnabled(false);
        menuNext.setEnabled(false);
        menuPrevious.setEnabled(false);
        
        // pack to get small size, then make it so that it cannot be resized smaller
        frame.pack();
        frame.setMinimumSize(new Dimension(frame.getSize().width - (musicViews.get(0).getPreferredSize().width / 2), frame.getSize().height));
        frame.setVisible(true);
    }

    // updates the staffs inside each musicView based on change
    // updates certain components based on number of staves
    private void StaffUpdate(int change) {
        int ind = currPage - 1;
        if (change == 1) {
            musicViews.get(ind).addStaff();
        } else if (change == -1) {
            musicViews.get(ind).removeStaff();
        }
        int staffCount = musicViews.get(ind).getStaffCount();
        if (staffCount <= 1) { // disable deletion if we only have 1 left
            deleteStaff.setEnabled(false);
            menuDeleteStaff.setEnabled(false);
        } else if (staffCount > 1) { // enable if more
            deleteStaff.setEnabled(true);
            menuDeleteStaff.setEnabled(true);
        }
        String staffString = (staffCount == 1) ? "staff" : "staves"; //plural check
        staffDescription = "Showing " + staffCount + " " + staffString;
        MainUpdate();
    }

    // adds or removes a page based on change
    // updates certain components based on number of pages and current page
    // manages creation and deletion of MusicView objects in musicViews
    private void PageUpdate(int change) {
        if (change < 0) { //decrement current page if too low
            musicViews.remove(currPage - 1);
            //if the previous current page was the last one, we need to decrement
            if (musicViews.size() + 1 == currPage) {
                currPage += change;
            }
        } else if (change > 0) { // if we are adding pages, add a new page with default value 4
            MusicView temp = new MusicView();
            attachNoteListener(temp);
            musicViews.add(temp);
        }
        if (musicViews.size() <= 1) { // if too small, cannot delete
            deletePage.setEnabled(false);
            menuDeletePage.setEnabled(false);
            //since deleting to one page will also mean curr page is 1, we need to disable previous buttons
            previous.setEnabled(false);
            menuPrevious.setEnabled(false);
        } else if (musicViews.size() - change <= 1 && musicViews.size() > 1) { // if big enough, enable delete
            deletePage.setEnabled(true);
            menuDeletePage.setEnabled(true);
        }
        if (change >= 1 && currPage == musicViews.size() - change) { // if adding and were already at limit, enable next
            next.setEnabled(true);
            menuNext.setEnabled(true);
        } else if (change <= -1 && currPage == musicViews.size()) { // if deleting and currPage becomes max, disable next
            next.setEnabled(false);
            menuNext.setEnabled(false);
        }
        pageDescription = "Page " + currPage + "/" + musicViews.size();
        // In case if our page changed, we should update the view (mainly due to next page and delete page)
        currMusicView = musicViews.get(currPage - 1);
        mainScrollPane.setViewportView(currMusicView);
        currMusicView.setNoteLength(noteLength);
        currMusicView.setNoteType(noteType);
        StaffUpdate(0);
        MainUpdate();
    }

    // move to the next page
    // checks if next/previous buttons need to be disabled if we moved from 1 or are at the end
    // this clears the selected note
    private void NextPage(boolean prevState, boolean nextState) {
        ++currPage;
        next.setEnabled(nextState);
        menuNext.setEnabled(nextState);
        previous.setEnabled(prevState);
        menuPrevious.setEnabled(prevState);
        if (currPage == musicViews.size()) {
            next.setEnabled(false);
            menuNext.setEnabled(false);
        } 
        if (currPage >= 2) {
            previous.setEnabled(true);
            menuPrevious.setEnabled(true);
        }
        PageUpdate(0);
        currMusicView.clearSelectedNote();
        stopRequested = true; // stop any playback if we change pages
        currMusicView.stopIndicator();
    }

    // move to the previous page
    // checks if next/previous buttons need to be disabled if we moved from the end or are at 1
    // this clears the selected note
    private void PrevPage(boolean prevState, boolean nextState) {
        --currPage;
        next.setEnabled(nextState);
        menuNext.setEnabled(nextState);
        previous.setEnabled(prevState);
        menuPrevious.setEnabled(prevState);
        if (currPage == 1) {
            previous.setEnabled(false);
            menuPrevious.setEnabled(false);
        } 
        if (currPage == musicViews.size() - 1) {
            next.setEnabled(true);
            menuNext.setEnabled(true);
        }
        PageUpdate(0);
        currMusicView.clearSelectedNote();
        stopRequested = true; // stop any playback if we change pages
        currMusicView.stopIndicator();
    }

    // these next two methods are for handling the animation component of the next/previous page before actually changing the page
    private void startNextPage() {
        // get buffered images of current and next page
        BufferedImage currImg = makeOffscreenImage(currMusicView);
        musicViews.get(currPage).setSize(currMusicView.getSize()); // ensure same size for animation
        BufferedImage nextImg = makeOffscreenImage(musicViews.get(currPage));

        // disable next/prev controls during animation
        // save states of buttons to re-enable later
        boolean nextEnabled = next.isEnabled();
        boolean prevEnabled = previous.isEnabled();
        next.setEnabled(false);
        previous.setEnabled(false);
        menuNext.setEnabled(false);
        menuPrevious.setEnabled(false);

        // debugging
        // try {
        //     ImageIO.write(currImg, "PNG", new File("currPage.png"));
        //     ImageIO.write(nextImg, "PNG", new File("nextPage.png"));
        // }
        // catch (Exception e) {
        // }

        // attach listener
        currMusicView.setPageTurnListener(e -> {
            NextPage(prevEnabled, nextEnabled);
        });

        // call animation
        currMusicView.nextPage(currImg, nextImg);
    }

    private void startPrevPage() {
        // get buffered images
        BufferedImage currImg = makeOffscreenImage(currMusicView);
        musicViews.get(currPage - 2).setSize(currMusicView.getSize()); // ensure same size for animation
        BufferedImage prevImg = makeOffscreenImage(musicViews.get(currPage - 2));

        // disable controls and save
        boolean nextEnabled = next.isEnabled();
        boolean prevEnabled = previous.isEnabled();
        next.setEnabled(false);
        previous.setEnabled(false);
        menuNext.setEnabled(false);
        menuPrevious.setEnabled(false);

        // attach listener
        currMusicView.setPageTurnListener(e -> {
            PrevPage(prevEnabled, nextEnabled);
        });

        // call animation
        currMusicView.previousPage(currImg, prevImg);
    }

    // updates the main label and page label on the tool panel
    // since the mainlabe, isn't used anymore, just affects page label
    // this could technically be removed and refactored to the pageupdate instead, but I'll keep it for now
    private void MainUpdate() {
        // mainLabel.setText(staffDescription + " " + pageDescription);
        pageLabel.setText(pageDescription);
    }

    // helper for creating the image icons for the buttons
    private ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL);
            Image resized = originalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resized);
            return resizedIcon;
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    // function for attaching all necessary action listeners to a MusicView
    // called every time a new MusicView is created
    private void attachNoteListener(MusicView musicView) {
        //some code for setting the cursor to be transparent
        // BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        // Cursor defaultCursor = Cursor.getDefaultCursor();
        // note each action listener is split into different behaviors depending on the state of the select button (selecting)
        musicView.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (selecting) { // selecting press - select the note position (if one exists)
                    Note selected = musicView.selectNote(e.getX(), e.getY());
                    if (selected != null) {
                        // calculate offset so that the note doesn't snap to position
                        dragOffsetX = e.getX() - selected.getOffsetX();
                        dragOffsetY = e.getY() - selected.getOffsetY();
                        statusLabel.setText("Selected " + musicView.getSelectedNote().getTypeString() + " with pitch " + musicView.getSelectedNote().getPitch() + " and length " + musicView.getSelectedNote().getDurationString() + " on staff " + (musicView.getSelectedNote().getStaffNum() + 1));
                    } else {
                        statusLabel.setText("No note selected");
                    }
                    musicView.requestFocusInWindow(); // this helps with allowing the delete function to "hear" the keypress due to focus
                } else if (penning) { // start drawing with the pen
                    musicView.startDrawing(e.getX(), e.getY());
                }
                else { // otherwise start drawing a note at position
                    // musicView.setCursor(blankCursor);
                    musicView.drawNote(e.getX(), e.getY());
                    statusLabel.setText("Drawing a " + musicView.getNote().getTypeString());
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (selecting) { // selecting release - place note (if there is one selected)
                    Note selectedNote = musicView.getSelectedNote();
                    if (selectedNote == null) {
                        statusLabel.setText("No note selected");
                        return;
                    }
                    boolean code = musicView.placeSelectedNote(e.getX() - dragOffsetX, e.getY() - dragOffsetY);
                    if (!code) {
                        statusLabel.setText("Placed note out of bounds, deleting note");
                        return;
                    }
                    if (selectedNote.getType() == 0) {
                        statusLabel.setText("Placed " + selectedNote.getTypeString() + " with pitch " + selectedNote.getPitch() + " and length " + selectedNote.getDurationString() + " on staff " + (selectedNote.getStaffNum() + 1));
                    } else if (selectedNote.getType() == 1) {
                        statusLabel.setText("Placed " + selectedNote.getTypeString() + " with length " + selectedNote.getDurationString() + " on staff " + (selectedNote.getStaffNum() + 1));
                    }
                } else if (penning) { // stop drawing note
                    Note drawnNote = musicView.stopDrawing();
                    if (drawnNote == null) {
                        statusLabel.setText("Drawing not recognized");
                    } else if ((drawnNote.getType() == 2 || drawnNote.getType() == 3) && drawnNote.getParent() == null) {
                        statusLabel.setText("Failed to place accidental; no note underneath");
                    } else if (drawnNote.getType() == 0) {
                        statusLabel.setText("Drew " + drawnNote.getTypeString() + " with pitch " + drawnNote.getPitch() + " and length " + drawnNote.getDurationString() + " on staff " + (drawnNote.getStaffNum() + 1));
                    } else if (drawnNote.getType() == 1) {
                        statusLabel.setText("Drew " + drawnNote.getTypeString() + " with length " + drawnNote.getDurationString() + " on staff " + (drawnNote.getStaffNum() + 1));
                    } else if (drawnNote.getType() == 2 || drawnNote.getType() == 3) {
                        statusLabel.setText("Drew " + drawnNote.getTypeString() + " modifying the note to " + drawnNote.getParent().getPitch() + " on staff " + (drawnNote.getStaffNum() + 1));
                    } else if (drawnNote.getType() == -1 && drawnNote.getDuration() == -1) {
                        statusLabel.setText("Scratching out " + drawnNote.getStaffNum() + " symbols underneath drawing");
                    }
                }
                else { // otherwise place the note we've been dragging
                    // musicView.setCursor(defaultCursor);
                    Note placedNote = musicView.getNote();
                    boolean code = musicView.placeNote(e.getX(), e.getY());
                    statusLabel.setText(placedNote.getTypeString() + " placed with pitch " + placedNote.getPitch() + " and length " + placedNote.getDurationString() + " on staff " + (placedNote.getStaffNum() + 1));
                    musicView.resetNote();
                    if (!code) { // if code is false, this means we placed out of bounds
                        statusLabel.setText("Placed note out of bounds, deleting note");
                        return;
                    }
                    if (placedNote.getType() == 0) {
                        statusLabel.setText("Placed " + placedNote.getTypeString() + " with pitch " + placedNote.getPitch() + " and length " + placedNote.getDurationString() + " on staff " + (placedNote.getStaffNum() + 1));
                    } else if (placedNote.getType() == 1) {
                        statusLabel.setText("Placed " + placedNote.getTypeString() + " with length " + placedNote.getDurationString() + " on staff " + (placedNote.getStaffNum() + 1));
                    } else if (placedNote.getType() == 2 || placedNote.getType() == 3) {
                        try {
                            statusLabel.setText("Placed " + placedNote.getTypeString() + " modifying the note to " + placedNote.getParent().getPitch() + " on staff " + (placedNote.getStaffNum() + 1));
                        } catch (NullPointerException ex) {
                            statusLabel.setText("Failed to place accidental, no note to modify");
                        }
                        
                    }
                }
            }
        });
        musicView.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                if (selecting) { // selecting drag - update note position
                    musicView.moveSelectedNote(e.getX() - dragOffsetX, e.getY() - dragOffsetY);
                } else if (penning) {
                    musicView.draw(e.getX(), e.getY());
                    statusLabel.setText("Drawing with pen");
                } else { // same, but without accounting for the offset
                    musicView.drawNote(e.getX(), e.getY());
                }
            }
            public void mouseMoved(MouseEvent e) {
                // nothing
            }
        });
        musicView.addKeyListener(new KeyAdapter() {
            // for the delete note function
            public void keyPressed(KeyEvent e) {
                if (selecting && e.getKeyCode() == KeyEvent.VK_DELETE) {
                    Note parentNote = musicView.getSelectedNote().getParent();
                    if (musicView.deleteSelectedNote()) {
                        statusLabel.setText("Selected note deleted");
                        if (parentNote != null) {
                            statusLabel.setText("Selected accidental deleted, modified note to " + parentNote.getPitch());
                        }
                    } else {
                        statusLabel.setText("No note selected to delete");
                    }
                }
            }
        });
        // KeyListener requires focus to work
        musicView.setFocusable(true);
        musicView.requestFocusInWindow();
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MusicEditor app = new MusicEditor();
            }
        });
    }

    
    public BufferedImage makeOffscreenImage (JComponent source) {
        // Create our BufferedImage and get a Graphics object for it
        GraphicsConfiguration gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage offscreenImage = gfxConfig.createCompatibleImage(source.getWidth(), source.getHeight());
        Graphics2D offscreenGraphics = (Graphics2D) offscreenImage.getGraphics();
        
        // Tell the component to paint itself onto the image
        source.paint(offscreenGraphics);
        
        // return the image
        return offscreenImage;
    }
}