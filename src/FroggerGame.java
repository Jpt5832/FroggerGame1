import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.imageio.ImageIO;

public class FroggerGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int frogX = 250, frogY = 550;
    private final int frogSize = 40;
    private final int panelWidth = 600;
    private static final int panelHeight = 600;
    private boolean isMoving = false;
    private int hopFrame = 0;
    private final int hopDuration = 10;
    private final int hopHeight = 10;
    private static final int STEP = 50;

    private final ArrayList<Car> cars = new ArrayList<>();
    private final ArrayList<LilyPad> lilyPads = new ArrayList<>();
    private final ArrayList<Frog> frogs = new ArrayList<>();

    private final int roadHeight = 250;
    private final int waterHeight = 100;
    private static final int landHeight = 100;

    private boolean gameOver = false;
    private String message = "";

    private static final int laneHeight = 40;
    private static final int laneGap = 15;

    private int frogsCollected = 0;
    private int lives = 3;

    private Clip squishClip;
    private Clip drownClip;
    private Clip ribbitClip;
    private Clip jumpClip;
    private Clip honkClip;

    private long lastHonkTime = 0;
    private int honkCooldown = 5000;
    private final Random rand = new Random();

    private Image playerImage;

    public FroggerGame() {
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(30, this);
        timer.start();

        loadSounds();
        loadPlayerImage();

        // initialize cars
        for (int i = 0; i < 4; i++) {
            int y = landHeight + waterHeight + (laneHeight + laneGap) * i;
            boolean leftToRight = i % 2 == 0;
            for (int j = 0; j < 2; j++) {
                int startX = leftToRight ? -j * 200 : panelWidth + j * 200;
                int speed = rand.nextInt(3) + 2;
                cars.add(new Car(startX, y, 80, laneHeight, speed * (leftToRight ? 1 : -1)));
            }
        }

        // initialize lily pads
        for (int i = 0; i < 2; i++) {
            int y = landHeight + (laneHeight + laneGap) * i;
            boolean leftToRight = i % 2 == 0;
            for (int j = 0; j < 2; j++) {
                int startX = leftToRight ? -j * 200 : panelWidth + j * 200;
                int speed = rand.nextInt(2) + 2;
                lilyPads.add(new LilyPad(startX, y, 100, laneHeight, speed * (leftToRight ? 1 : -1)));
            }
        }

        // initialize collectible frogs
        for (int i = 0; i < 5; i++) {
            int x = (i + 1) * (panelWidth / 6);
            int y = landHeight - frogSize;
            frogs.add(new Frog(x, y, frogSize, frogSize, getRandomColor()));
        }
    }

    private void loadSounds() {
        try {
            squishClip = AudioSystem.getClip();
            drownClip  = AudioSystem.getClip();
            ribbitClip = AudioSystem.getClip();
            jumpClip   = AudioSystem.getClip();
            honkClip   = AudioSystem.getClip();

            squishClip.open(AudioSystem.getAudioInputStream(new File("Resource/squish.wav")));
            drownClip.open(AudioSystem.getAudioInputStream(new File("Resource/drown.wav")));
            ribbitClip.open(AudioSystem.getAudioInputStream(new File("Resource/ribbit.wav")));
            jumpClip.open(AudioSystem.getAudioInputStream(new File("Resource/jump.wav")));
            honkClip.open(AudioSystem.getAudioInputStream(new File("Resource/honk.wav")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerImage() {
        try {
            playerImage = ImageIO.read(new File("Resource/frogger.png"));
        } catch (IOException e) {
            e.printStackTrace();
            playerImage = null;
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // draw grass
        g.setColor(Color.green);
        g.fillRect(0, 0, panelWidth, landHeight);

        // draw water
        g.setColor(Color.blue);
        g.fillRect(0, landHeight, panelWidth, waterHeight);

        // draw lily pads
        g.setColor(new Color(34, 139, 34));
        for (LilyPad pad : lilyPads) {
            if (!pad.isSinking()) {
                g.fillOval(pad.x, pad.y, pad.width, pad.height);
            }
        }

        // draw road
        g.setColor(Color.darkGray);
        g.fillRect(0, landHeight + waterHeight, panelWidth, roadHeight);

        // draw cars
        g.setColor(Color.red);
        for (Car car : cars) {
            g.fillRect(car.x, car.y, car.width, car.height);
        }

        // draw road lines
        g.setColor(Color.white);
        for (int i = 0; i < 4; i++) {
            int y = landHeight + waterHeight + (laneHeight + laneGap) * i + laneHeight / 2;
            drawDashedLine(g, 0, y, panelWidth, y);
        }

        // draw player frog
        int offset = getHopOffset();
        if (playerImage != null) {
            g.drawImage(playerImage, frogX, frogY - offset, frogSize, frogSize, this);
        } else {
            g.setColor(Color.yellow);
            g.fillRect(frogX, frogY - offset, frogSize, frogSize);
        }

        // draw collectible frogs
        for (Frog f : frogs) {
            if (!f.isCollected()) {
                g.setColor(f.getColor());
                g.fillRect(f.getX(), f.getY(), f.getWidth(), f.getHeight());
            }
        }

        // draw UI
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Lives: " + lives, 10, 20);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(message, 180, 300);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press ENTER to play again", 180, 340);
        }
    }

    private int getHopOffset() {
        if (!isMoving) return 0;
        double t = Math.PI * hopFrame / hopDuration;
        return (int) (Math.sin(t) * hopHeight);
    }

    private void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2) {
        float dash[] = {10.0f};
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0, dash, 0));
        g2d.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        if (isMoving) {
            hopFrame++;
            if (hopFrame >= hopDuration) {
                isMoving = false;
                hopFrame = 0;
            }
        }

        // move cars and handle collisions/honks
        for (Car car : cars) {
            car.move(panelWidth);
            if (car.intersects(frogX, frogY, frogSize, frogSize)) {
                playSound(squishClip);
                loseLife();
            }

            int dx = Math.abs(car.x - frogX);
            int dy = Math.abs(car.y - frogY);
            if (dx < 100 && dy < 40) {
                long now = System.currentTimeMillis();
                if (now - lastHonkTime >= honkCooldown) {
                    playSound(honkClip);
                    lastHonkTime = now;
                    honkCooldown = 5000 + rand.nextInt(5000);
                }
            }
        }

        // move lily pads
        for (LilyPad pad : lilyPads) pad.move(panelWidth);

        // collect frogs
        for (Frog f : frogs) {
            if (!f.isCollected()
                    && f.getX() < frogX + frogSize
                    && f.getX() + f.getWidth() > frogX
                    && f.getY() < frogY + frogSize
                    && f.getY() + f.getHeight() > frogY) {

                f.collect();
                frogsCollected++;
                playSound(ribbitClip);
                resetFrog();
                break;
            }
        }

        if (frogsCollected == 5) {
            message = "YOU WIN!";
            gameOver = true;
            timer.stop();
        }

        // drown logic
        if (frogY >= landHeight
                && frogY < landHeight + waterHeight) {

            boolean onPad = false;
            for (LilyPad pad : lilyPads) {
                if (!pad.isSinking()
                        && frogX < pad.x + pad.width
                        && frogX + frogSize > pad.x
                        && frogY < pad.y + pad.height
                        && frogY + frogSize > pad.y) {
                    onPad = true;
                    break;
                }
            }
            if (!onPad) {
                playSound(drownClip);
                loseLife();
            }
        }

        repaint();
    }

    private void loseLife() {
        lives--;
        if (lives <= 0) {
            message = "GAME OVER!";
            gameOver = true;
            timer.stop();
        }
        resetFrog();
    }

    private void resetFrog() {
        frogX = 250;
        frogY = 550;
        isMoving = false;
        hopFrame = 0;
    }

    private void restartGame() {
        resetFrog();
        frogsCollected = 0;
        frogs.forEach(f -> f.collected = false);
        message = "";
        lives = 3;
        gameOver = false;
        timer.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver && e.getKeyCode() == KeyEvent.VK_ENTER) {
            restartGame();
            return;
        }
        if (isMoving) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                frogX = Math.max(frogX - STEP, 0);
                isMoving = true;
                playSound(jumpClip);
                break;
            case KeyEvent.VK_RIGHT:
                frogX = Math.min(frogX + STEP, panelWidth - frogSize);
                isMoving = true;
                playSound(jumpClip);
                break;
            case KeyEvent.VK_UP:
                frogY = Math.max(frogY - STEP, 0);
                isMoving = true;
                playSound(jumpClip);
                break;
            case KeyEvent.VK_DOWN:
                frogY = Math.min(frogY + STEP, panelHeight - frogSize);
                isMoving = true;
                playSound(jumpClip);
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // --- Inner classes Car, LilyPad, Frog unchanged below ---

    private static class Car {
        int x, y, width, height, speed;
        Car(int x, int y, int w, int h, int s) {
            this.x = x; this.y = y;
            this.width = w; this.height = h;
            this.speed = s;
        }
        void move(int pw) {
            x += speed;
            if (speed > 0 && x > pw) x = -width;
            if (speed < 0 && x + width < 0) x = pw;
        }
        boolean intersects(int fx, int fy, int fw, int fh) {
            return fx < x + width && fx + fw > x
                    && fy < y + height && fy + fh > y;
        }
    }

    private static class LilyPad {
        int x, y, width, height, speed;
        boolean sinking = false;
        long st;
        private final int normalY;

        LilyPad(int x, int y, int w, int h, int s) {
            this.x = x; this.y = y;
            this.width = w; this.height = h;
            this.speed = s;
            this.normalY = y;
        }

        void move(int pw) {
            x += speed;
            if (x + width < 0) x = pw;
            if (x > pw) x = -width;
            if (sinking && System.currentTimeMillis() - st > 2000) {
                sinking = false;
                y = normalY;
            }
            if (!sinking && Math.random() < 0.002) {
                sinking = true;
                st = System.currentTimeMillis();
                y = panelHeight;
            }
        }

        boolean isSinking() { return sinking; }
    }

    private static class Frog {
        int x, y, width, height;
        Color color;
        boolean collected = false;
        Frog(int x, int y, int w, int h, Color c) {
            this.x = x; this.y = y;
            this.width = w; this.height = h;
            this.color = c;
        }
        void collect() { collected = true; }
        boolean isCollected() { return collected; }
        int getX() { return x; }
        int getY() { return y; }
        int getWidth() { return width; }
        int getHeight() { return height; }
        Color getColor() { return color; }
    }

    private Color getRandomColor() {
        return new Color(rand.nextInt(256),
                rand.nextInt(256),
                rand.nextInt(256));
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


