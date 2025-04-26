import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

public class FroggerGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int frogX = 250, frogY = 550;
    private int targetX = 250, targetY = 550;
    private final int frogSize = 40;
    private final int panelWidth = 600;
    private static final int panelHeight = 600;
    private boolean isMoving = false;
    private int hopFrame = 0;
    private final int hopDuration = 10;
    private final int hopHeight = 10;

    private final ArrayList<Car> cars = new ArrayList<>();
    private final ArrayList<LilyPad> lilyPads = new ArrayList<>();
    private final ArrayList<Frog> frogs = new ArrayList<>();

    private final int roadHeight = 250;
    private final int waterHeight = 100;
    private static final int landHeight = 100;

    private boolean gameWon = false;
    private boolean gameOver = false;
    private String message = "";

    private static final int laneHeight = 40;
    private static final int laneGap = 15;

    private long lastSinkingTime = 0;
    private static final int SINK_INTERVAL = 5000;

    private int frogsCollected = 0;

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

        for (int i = 0; i < 5; i++) {
            int x = (i + 1) * (panelWidth / 6);
            int y = landHeight - frogSize;
            frogs.add(new Frog(x, y, frogSize, frogSize, getRandomColor()));
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
            if (pad.y + pad.height <= landHeight + waterHeight && pad.y >= landHeight && !pad.isSinking()) {
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

        int drawFrogY = frogY - getHopOffset();
        g.setColor(Color.yellow);
        g.fillRect(frogX, drawFrogY, frogSize, frogSize);

        for (Frog f : frogs) {
            if (!f.isCollected()) {
                g.setColor(f.getColor());
                g.fillRect(f.getX(), f.getY(), f.getWidth(), f.getHeight());
            }
        }

        if (gameOver) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(message, 180, 300);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press ENTER to play again", 180, 340);
        }
    }

    private int getHopOffset() {
        if (!isMoving) return 0;
        double progress = Math.PI * hopFrame / hopDuration;
        return (int)(Math.sin(progress) * hopHeight);
    }

    private void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2) {
        float dash[] = {10.0f};
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
        g2d.drawLine(x1, y1, x2, y2);
    }

    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSinkingTime >= SINK_INTERVAL) {
            lastSinkingTime = currentTime;
            int randomIndex = new Random().nextInt(lilyPads.size());
            lilyPads.get(randomIndex).sink();
        }

        if (isMoving) {
            hopFrame++;
            int dx = targetX - frogX;
            int dy = targetY - frogY;
            frogX += dx / Math.max(1, hopDuration - hopFrame);
            frogY += dy / Math.max(1, hopDuration - hopFrame);
            if (frogX == targetX && frogY == targetY) {
                isMoving = false;
                hopFrame = 0;
            }
        }

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

        for (Frog f : frogs) {
            if (!f.isCollected() && f.getX() < frogX + frogSize && f.getX() + f.getWidth() > frogX &&
                    f.getY() < frogY + frogSize && f.getY() + f.getHeight() > frogY) {
                f.collect();
                frogsCollected++;
                break;
            }
        }

        if (frogsCollected == 5) {
            message = "YOU WIN!";
            gameOver = true;
            timer.stop();
        }

        if (frogY < landHeight + waterHeight && frogY >= landHeight) {
            boolean onLilyPad = false;
            for (LilyPad pad : lilyPads) {
                if (!pad.isSinking() && pad.x < frogX + frogSize && pad.x + pad.width > frogX &&
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

        repaint();
    }

    private void resetFrog() {
        frogX = 250;
        frogY = 550;
        targetX = frogX;
        targetY = frogY;
    }

    private void restartGame() {
        resetFrog();
        gameWon = false;
        gameOver = false;
        message = "";
        frogsCollected = 0;
        for (Frog f : frogs) f.collected = false;
        timer.start();
    }

    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                restartGame();
            }
            return;
        }

        if (isMoving) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> targetX = Math.max(frogX - 50, 0);
            case KeyEvent.VK_RIGHT -> targetX = Math.min(frogX + 50, panelWidth - frogSize);
            case KeyEvent.VK_UP -> targetY = Math.max(frogY - 50, 0);
            case KeyEvent.VK_DOWN -> targetY = Math.min(frogY + 50, panelHeight - frogSize);
        }
        isMoving = true;
        hopFrame = 0;
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    private static class Frog {
        private int x, y, width, height;
        private Color color;
        private boolean collected;

        public Frog(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.collected = false;
        }

        public void collect() {
            this.collected = true;
        }

        public boolean isCollected() {
            return collected;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public Color getColor() { return color; }
    }

    private Color getRandomColor() {
        Random rand = new Random();
        return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

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
            return fx < x + width && fx + fSizeX > x && fy < y + height && fy + fSizeY > y;
        }
    }

    private static class LilyPad {
        int x, y, width, height, speed;
        boolean sinking;
        long sinkStartTime;

        public LilyPad(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
            this.sinking = false;
            this.sinkStartTime = 0;
        }

        public void move(int panelWidth) {
            x += speed;
            if (x + width < 0) x = panelWidth;
            if (x > panelWidth) x = -width;
            if (sinking && System.currentTimeMillis() - sinkStartTime >= 2000) {
                sinking = false;
                y = landHeight + (laneHeight + laneGap) * new Random().nextInt(2);
            }
        }

        public void sink() {
            if (!sinking) {
                sinking = true;
                sinkStartTime = System.currentTimeMillis();
                y = panelHeight;
            }
        }

        public boolean isSinking() { return sinking; }
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








