package gui;

import model.Movie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * คอมโพเนนต์แสดงการ์ดภาพยนตร์ 1 เรื่อง
 * ประกอบด้วย รูปโปสเตอร์ (PosterBox), ชื่อภาพยนตร์ และความยาว
 */
public class MovieCard extends JPanel {

    public MovieCard(Movie movie, int index, boolean isSelected, Runnable onSelect) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // กล่องรูปโปสเตอร์
        PosterBox posterBox = new PosterBox(movie, index, isSelected);
        posterBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(posterBox);

        add(Box.createRigidArea(new Dimension(0, 6)));

        // ชื่อภาพยนตร์
        JLabel titleLbl = new JLabel("<html><div style='text-align: center; font-family: Tahoma;'>"
                + (movie != null ? movie.title() : "") + "</div></html>", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Tahoma", isSelected ? Font.BOLD : Font.PLAIN, 11));
        titleLbl.setForeground(isSelected ? MainFrame.COLOR_PRIMARY : MainFrame.COLOR_TEXT_TITLE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLbl.setPreferredSize(new Dimension(170, 34));
        titleLbl.setMaximumSize(new Dimension(170, 34));
        add(titleLbl);

        add(Box.createRigidArea(new Dimension(0, 2)));

        // ความยาวภาพยนตร์
        String durationStr = (movie != null) ? movie.durationMinutes() + " min" : "";
        JLabel durLbl = new JLabel(durationStr, SwingConstants.CENTER);
        durLbl.setFont(MainFrame.FONT_SMALL);
        durLbl.setForeground(MainFrame.COLOR_TEXT_MUTED);
        durLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(durLbl);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelect != null) {
                    onSelect.run();
                }
            }
        });
    }
}
