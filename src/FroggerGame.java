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
    private final int laneHeight = 60;
    private final int numLanes = 5;

    private boolean gameWon = false;
    private boolean gameOver = false;
    private String message = "";

    public FroggerGame() {
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(30, this);
        timer.start();

        Random rand = new Random();
        for (int i = 0; i < numLanes; i++) {
            int y = laneHeight * (i + 1);
            int speed = rand.nextInt(3) + 2;
            boolean leftToRight = i % 2 == 0;
            cars.add(new Car(leftToRight ? 0 : panelWidth, y, 80, 40, speed * (leftToRight ? 1 : -1)));
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw goal line
        g.setColor(Color.green);
        g.fillRect(0, 0, panelWidth, laneHeight);

        // Draw frog
        g.setColor(Color.yellow);
        g.fillRect(frogX, frogY, frogSize, frogSize);

        // Draw cars
        g.setColor(Color.red);
        for (Car car : cars) {
            g.fillRect(car.x, car.y, car.width, car.height);
        }

        // Show win/lose message
        if (gameOver) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(message, 180, 300);

            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press ENTER to play again", 180, 340);
        }
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

        if (frogY < laneHeight) {
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

    // Car class
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

    // Main method
    public static void main(String[] args) {
        JFrame frame = new JFrame("Frogger");
        FroggerGame game = new FroggerGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setVisible(true);
    }
}

