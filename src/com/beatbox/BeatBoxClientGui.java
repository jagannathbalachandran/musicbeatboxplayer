package com.beatbox;

import javax.sound.midi.InvalidMidiDataException;import javax.sound.midi.MidiEvent;import javax.sound.midi.MidiSystem;import javax.sound.midi.MidiUnavailableException;import javax.sound.midi.Sequence;import javax.sound.midi.Sequencer;import javax.sound.midi.ShortMessage;import javax.sound.midi.Track;
import javax.swing.BoxLayout;import javax.swing.JButton;import javax.swing.JCheckBox;import javax.swing.JFileChooser;import javax.swing.JFrame;import javax.swing.JLabel;import javax.swing.JList;import javax.swing.JPanel;import javax.swing.JScrollPane;import javax.swing.JTextField;import javax.swing.ListSelectionModel;import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;import java.awt.GridLayout;import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;import java.io.FileInputStream;import java.io.FileOutputStream;import java.io.IOException;import java.io.ObjectInputStream;import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;import java.lang.Exception;import java.lang.Object;import java.lang.Override;import java.lang.Runnable;import java.lang.String;import java.lang.System;import java.lang.Thread;import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class BeatBoxClientGui {

    JButton startButton;
    JButton stopButton;
    JButton tempoUpButton;
    JButton tempoDownButton;
    JButton saveButton;
    JButton restoreButton;
    JButton clearButton;
    JButton sendButton;
    Sequence sequence;
    Sequencer sequencer;
    JLabel chatWindowLabel, messageLabel;
    JTextField messageTextBox;
    boolean[] beatList;
    Vector<String> listVector = new Vector<String>();
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    Track track;
    ArrayList<JCheckBox> checkBoxList;
    JFrame frame;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Bat",
            "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maxacas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Aqoqo",
            "0pen Hi Conga"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 59, 47, 67, 63}; //key for each instrument that needs to be passed
    private Socket socket;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private String name;
    private JList incomingList;

    public BeatBoxClientGui(String name) {

        this.name = name;
    }

    public static void main(String[] args) {
        BeatBoxClientGui gui = new BeatBoxClientGui("Jaggu");
        gui.go();
    }

    public void go() {
        frame = new JFrame("Cyber beat box : " + name);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        tempoUpButton = new JButton("Tempo Up");
        tempoDownButton = new JButton("Tempo Down");
        saveButton = new JButton("Save");
        restoreButton = new JButton("Restore");
        clearButton = new JButton("Clear");
        sendButton = new JButton("Send");
        chatWindowLabel = new JLabel("Chat Window");
        messageLabel = new JLabel("Message");
        messageTextBox = new JTextField();
        incomingList = new JList();
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        incomingList.setListData(listVector);


        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(tempoUpButton);
        buttonPanel.add(tempoDownButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(restoreButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(messageLabel);
        buttonPanel.add(messageTextBox);
        buttonPanel.add(sendButton);
        buttonPanel.add(chatWindowLabel);
        buttonPanel.add(theList);

        setUpMidi();

        startButton.addActionListener(new StartButtonListener(new TrackBuilder()));
        stopButton.addActionListener(new StopButtonListener(sequencer));
        tempoDownButton.addActionListener(new TempoUpButtonListener(sequencer));
        tempoDownButton.addActionListener(new TempoDownButtonListener(sequencer));
        saveButton.addActionListener(new SaveButtonListener());
        restoreButton.addActionListener(new RestoreButtonListener());
        clearButton.addActionListener(new ClearButtonListener());
        sendButton.addActionListener(new SendButtonListener());
        incomingList.addListSelectionListener(new MyListSelectionListener());


        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(BorderLayout.EAST, buttonPanel);

        JPanel mainPanel; //= new JPanel();
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);

//        mainPanel.setLayout(new BoxLayout(mainPanel , BoxLayout.Y_AXIS));
        checkBoxList = new ArrayList<JCheckBox>();

        mainPanel = new JPanel(grid);
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }
        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);

        createInstrumentsPanel(frame);

        frame.setBounds(50, 50, 300, 300);
        frame.pack();
        frame.setVisible(true);

        try {
            socket = new Socket("127.0.0.1", 4243);
            reader = new ObjectInputStream(socket.getInputStream());
            writer = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        MessageReceiver messageReceiver = new MessageReceiver();
        Thread newThread = new Thread(messageReceiver);
        newThread.start();

    }

    private void createInstrumentsPanel(JFrame frame) {
        JPanel instrumentsPanel = new JPanel();
        instrumentsPanel.setLayout(new BoxLayout(instrumentsPanel, BoxLayout.Y_AXIS));

        for (int i = 0; i < 16; i++) {
            instrumentsPanel.add(new JLabel(instrumentNames[i]));
        }

        frame.getContentPane().add(BorderLayout.WEST, instrumentsPanel);
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    class StartButtonListener implements ActionListener {

        private TrackBuilder trackBuilder;

        public StartButtonListener(TrackBuilder trackBuilder) {

            this.trackBuilder = trackBuilder;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            trackBuilder.buildTrackAndStart();
        }
    }


    public static MidiEvent makeEvent(int cmd, int chn, int one, int two, int tick) {
        ShortMessage a = new ShortMessage();
        MidiEvent event = null;
        try {
            a.setMessage(cmd, chn, one, two);
            event = new MidiEvent(a, tick);
        } catch (InvalidMidiDataException e) {
            return event;
        }
        return event;
    }

    /**
     * Created by jbalacha on 01/12/15.
     */
    class TrackBuilder {

        public void buildTrackAndStart() {
            int[] beatListForCurrInstr = null;
            sequence.deleteTrack(track);
            track = sequence.createTrack();

            for (int instrumentIndex = 0; instrumentIndex < 16; instrumentIndex++) {
                beatListForCurrInstr = new int[16];
                createBeatListForCurrentInstrument(beatListForCurrInstr, instrumentIndex);
                makeTracks(beatListForCurrInstr);
                track.add(makeEvent(176, 1, 127, 0, 16));
            }

            track.add(makeEvent(192, 9, 1, 0, 15));
            try {
                sequencer.setSequence(sequence);
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                sequencer.start();
                sequencer.setTempoInBPM(120);

            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        private void createBeatListForCurrentInstrument(int[] beatListForCurrInstr, int instrumentIndex) {

            int key = instruments[instrumentIndex];

            for (int beatIndexForCurrInstr = 0; beatIndexForCurrInstr < 16; beatIndexForCurrInstr++) {
                JCheckBox checkBox = checkBoxList.get((instrumentIndex * 16) + beatIndexForCurrInstr);
                if (checkBox.isSelected())
                    beatListForCurrInstr[beatIndexForCurrInstr] = key;
                else
                    beatListForCurrInstr[beatIndexForCurrInstr] = 0;
            }
        }

        public void makeTracks(int[] beatListForCurrInst) {
            for (int i = 0; i < 16; i++) {
                int key = beatListForCurrInst[i];
                if (key != 0) {
                    track.add(makeEvent(144, 9, key, 100, i));
                    track.add(makeEvent(128, 9, key, 100, i + 1));
                }
            }
        }
    }

    private class SaveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            beatList = createBeatList();
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(frame);
            java.lang.System.out.println(fileSave.getSelectedFile());
            saveTrack(fileSave.getSelectedFile());

        }

        private void saveTrack(File savedFile) {
            try {
                ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(savedFile.getAbsoluteFile()));
                os.writeObject(beatList);
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean[] createBeatList() {
        boolean[] beatList = new boolean[256];
        int i = 0;
        for (Iterator<JCheckBox> iterator = checkBoxList.iterator(); iterator.hasNext(); ) {
            JCheckBox checkBox = iterator.next();
            if (checkBox.isSelected()) beatList[i] = true;
            else beatList[i] = false;
            i++;
        }
        return beatList;
    }

    private class RestoreButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(frame);
            System.out.println(fileSave.getSelectedFile());
            loadTrack(fileSave.getSelectedFile());
        }

        private void loadTrack(File selectedFile) {

            clearTrack();
            boolean[] beatList = null;
            try {
                ObjectInputStream os = new ObjectInputStream(new FileInputStream(selectedFile.getAbsoluteFile()));
                Object o = os.readObject();
                beatList = (boolean[]) o;
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            initializeCheckBoxs(beatList);
        }


    }

    private void initializeCheckBoxs(boolean[] beatList) {
        for (int index = 0; index < 256; index++) {
            if (beatList[index]) checkBoxList.get(index).setSelected(true);
            else
                checkBoxList.get(index).setSelected(false);
        }
    }

    private class ClearButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            clearTrack();
        }
    }

    private void clearTrack() {
        for (Iterator<JCheckBox> iterator = checkBoxList.iterator(); iterator.hasNext(); ) {
            JCheckBox checkBox = iterator.next();
            checkBox.setSelected(false);
        }
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            Object message = null;
            boolean[] beatList = null;
            try {
                while ((message = reader.readObject()) != null) {

                    System.out.println("Read message from server " + message);
                    beatList = (boolean[]) reader.readObject();
                    System.out.println("Track from server " + beatList);
                    otherSeqsMap.put((String) message, beatList) ;
                    listVector .add((String) message) ;
                    incomingList.setListData(listVector) ;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
    }


    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            boolean[] beatList = createBeatList();
            CustomMessage message = new CustomMessage(name + " : " + messageTextBox.getText(), beatList);
            try {
                System.out.println("Message sent to server " + name + " : " + messageTextBox.getText());
                System.out.println("Message sent to server " + beatList);
                writer.writeObject(name + " : " + messageTextBox.getText());
                writer.writeObject(beatList);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            messageTextBox.setText("");
            messageTextBox.requestFocus();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null) {
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                }
            }
        }

    }

    private void changeSequence(boolean[] selectedState) {
        clearTrack();
        initializeCheckBoxs(selectedState);
    }
}
