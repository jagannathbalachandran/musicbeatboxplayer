package com.beatbox;import javax.sound.midi.Sequencer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class TempoUpButtonListener implements ActionListener {

    private Sequencer sequencer;

    public TempoUpButtonListener(Sequencer sequencer) {

        this.sequencer = sequencer;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor((float) (tempoFactor * 1.03));
    }

}