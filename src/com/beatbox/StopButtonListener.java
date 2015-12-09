package com.beatbox;import javax.sound.midi.Sequencer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created by jbalacha on 01/12/15.
*/
class StopButtonListener implements ActionListener {

    private Sequencer sequencer;

    public StopButtonListener(Sequencer sequencer) {

        this.sequencer = sequencer;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        sequencer.stop();
    }

}
