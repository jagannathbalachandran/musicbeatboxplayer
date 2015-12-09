package com.beatbox;import javax.sound.midi.Sequencer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created by jbalacha on 01/12/15.
*/
class TempoDownButtonListener implements ActionListener {

    private Sequencer sequencer;

    public TempoDownButtonListener(Sequencer sequencer) {

        this.sequencer = sequencer;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor((float) (tempoFactor * .97));
    }

}
