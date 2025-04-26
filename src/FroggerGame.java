import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class FroggerGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int frogX = 250, frogY = 550;
    private final int frogSize = 40;
    private final int panelWidth = 600, panelHeight = 600;

    private final ArrayList<Car> cars = new ArrayList<>();
    private final ArrayList<LilyPad> lilyPads = new ArrayList<>();

    // Section heights
    private final int roadHeight = 250;
    private final int waterHeight = 100;
    private final int landHeight = 100;

    private boolean gameWon = false;
    private boolean gameOver = false;
    private String message = "";

    private final int laneHeight = 40;
    private final int laneGap = 15;

    public FroggerGame() {
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(30, this);
        timer.start();

        Random rand = new Random();
        for (int i = 0; i < 4; i++) {
            int y = landHeight + waterHeight + (laneHeight + laneGap) * i;
            boolean leftToRight = i % 2 == 0;
            for (int j = 0; j < 2; j++) {
                int startX = leftToRight ? -j * 200 : panelWidth + j * 200;
                int speed = rand.nextInt(3) + 2 + rand.nextInt(2);
                int actualSpeed = speed * (leftToRight ? 1 : -1);
                cars.add(new Car(startX, y, 80, laneHeight, actualSpeed));
            }
        }

        for (int i = 0; i < 2; i++) {
            int y = landHeight + (laneHeight + laneGap) * i;
            boolean leftToRight = i % 2 == 0;
            for (int j = 0; j < 2; j++) {
                int startX = leftToRight ? -j * 200 : panelWidth + j * 200;
                int speed = rand.nextInt(2) + 1;
                lilyPads.add(new LilyPad(startX, y, 100, laneHeight, speed * (leftToRight ? 1 : -1)));
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.green);
        g.fillRect(0, 0, panelWidth, landHeight);

        g.setColor(Color.blue);
        g.fillRect(0, landHeight, panelWidth, waterHeight);

        g.setColor(new Color(34, 139, 34));
        for (LilyPad pad : lilyPads) {
            if (pad.y + pad.height <= landHeight + waterHeight && pad.y >= landHeight) {
                g.fillOval(pad.x, pad.y, pad.width, pad.height);
            }
        }

        g.setColor(Color.darkGray);
        g.fillRect(0, landHeight + waterHeight, panelWidth, roadHeight);

        g.setColor(Color.red);
        for (Car car : cars) {
            g.fillRect(car.x, car.y, car.width, car.height);
        }

        g.setColor(Color.white);
        for (int i = 0; i < 4; i++) {
            int y = landHeight + waterHeight + (laneHeight + laneGap) * i + laneHeight / 2;
            drawDashedLine(g, 0, y, panelWidth, y);
        }

        g.setColor(Color.yellow);
        g.fillRect(frogX, frogY, frogSize, frogSize);

        if (gameOver) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(message, 180, 300);

            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press ENTER to play again", 180, 340);
        }
    }

    private void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2) {
        float dash[] = {10.0f};
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
        g2d.drawLine(x1, y1, x2, y2);
    }

    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        for (Car car : cars) {
            car.move(panelWidth);
            if (car.intersects(frogX, frogY, frogSize, frogSize)) {
                message = "YOU LOSE!";
                gameOver = true;
                timer.stop();
            }
        }

        for (LilyPad pad : lilyPads) {
            pad.move(panelWidth);
        }

        if (frogY < landHeight + waterHeight && frogY >= landHeight) {
            boolean onLilyPad = false;
            for (LilyPad pad : lilyPads) {
                if (pad.x < frogX + frogSize && pad.x + pad.width > frogX &&
                        pad.y < frogY + frogSize && pad.y + pad.height > frogY) {
                    onLilyPad = true;
                    break;
                }
            }

            if (!onLilyPad) {
                message = "YOU DROWNED!";
                gameOver = true;
                timer.stop();
            }
        }

        if (frogY < landHeight) {
            message = "YOU WIN!";
            gameOver = true;
            timer.stop();
        }

        repaint();
    }

    private void resetFrog() {
        frogX = 250;
        frogY = 550;
    }

    private void restartGame() {
        resetFrog();
        gameWon = false;
        gameOver = false;
        message = "";
        timer.start();
    }

    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                restartGame();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> frogX = Math.max(frogX - 50, 0);
            case KeyEvent.VK_RIGHT -> frogX = Math.min(frogX + 50, panelWidth - frogSize);
            case KeyEvent.VK_UP -> frogY = Math.max(frogY - 50, 0);
            case KeyEvent.VK_DOWN -> frogY = Math.min(frogY + 50, panelHeight - frogSize);
        }
        repaint();
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    private static class Car {
        int x, y, width, height, speed;

        public Car(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }

        public void move(int panelWidth) {
            x += speed;
            if (speed > 0 && x > panelWidth) x = -width;
            if (speed < 0 && x + width < 0) x = panelWidth;
        }

        public boolean intersects(int fx, int fy, int fSizeX, int fSizeY) {
            return fx < x + width && fx + fSizeX > x &&
                    fy < y + height && fy + fSizeY > y;
        }
    }

    private static class LilyPad {
        int x, y, width, height, speed;

        public LilyPad(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }

        public void move(int panelWidth) {
            x += speed;
            if (x + width < 0) x = panelWidth;
            if (x > panelWidth) x = -width;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Frogger Game");
        FroggerGame game = new FroggerGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setVisible(true);
    }
}












