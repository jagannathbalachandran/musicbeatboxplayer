package com.beatbox;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.lang.Object;
import java.lang.Override;
import java.lang.Runnable;
import java.lang.String;
import java.lang.System;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class BeatBoxServer {


    JFrame frame;
    JPanel panel;
    JTextArea messageTextBox;
    JLabel messageLabel;
    ArrayList<ObjectOutputStream> clientOutputStreams;

    public static void main(String[] args) {
        BeatBoxServer server = new BeatBoxServer();
        server.go();
    }

    private void go() {
        setUpGui();
        try {
            ServerSocket serverSocket = new ServerSocket(4243);
            clientOutputStreams = new ArrayList<ObjectOutputStream>();
            while(true){
                Socket socket = serverSocket.accept();
                ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
                clientOutputStreams.add(writer);
                MessageReceiver messageReceiver = new MessageReceiver(socket);
                Thread newThread = new Thread(messageReceiver);
                newThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpGui() {
        frame = new JFrame("Simple Best Box Server");
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        messageTextBox = new JTextArea();
        messageLabel = new JLabel("Message");
        panel.add(messageLabel);
        panel.add(messageTextBox);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(BorderLayout.CENTER , panel);

        frame.setSize(800, 800);
        frame.pack();
        frame.setVisible(true);
    }


    private class MessageReceiver implements Runnable{
        ObjectInputStream reader;

        public MessageReceiver(Socket socket) {
            try {
                reader = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            java.lang.Object message = null;
            boolean[] beatList = null;
            try {
                while ((message = reader.readObject())!= null){
                    System.out.println("Message received from client " + message);
                    messageTextBox.setLineWrap(true);
                    messageTextBox.setWrapStyleWord(true);
                    messageTextBox.append(message + "\n");
                    beatList = (boolean[]) reader.readObject();
                    broadcast(message , beatList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


        }

        private void broadcast(Object message, boolean[] beatList) {
            for (Iterator<ObjectOutputStream> iterator = clientOutputStreams.iterator(); iterator.hasNext(); ) {
                ObjectOutputStream writer = iterator.next();
                try {
                    writer.writeObject(message);
                    writer.writeObject(beatList);
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
