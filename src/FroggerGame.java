import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;

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

    private long lastSinkingTime = 0;
    private static final int SINK_INTERVAL = 5000;

    private int frogsCollected = 0;

    // --- Sound Clips ---
    private Clip squishClip;
    private Clip drownClip;
    private Clip ribbitClip;

    public FroggerGame() {
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(30, this);
        timer.start();

        loadSounds();

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

    /** Load sound files from Resource folder **/
    private void loadSounds() {
        try {
            squishClip = AudioSystem.getClip();
            drownClip = AudioSystem.getClip();
            ribbitClip = AudioSystem.getClip();

            squishClip.open(AudioSystem.getAudioInputStream(new File("Resource/squish.wav")));
            drownClip.open(AudioSystem.getAudioInputStream(new File("Resource/drown.wav")));
            ribbitClip.open(AudioSystem.getAudioInputStream(new File("Resource/ribbit.wav")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Utility to play a clip from start **/
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
        g.setColor(Color.green);
        g.fillRect(0, 0, panelWidth, landHeight);
        g.setColor(Color.blue);
        g.fillRect(0, landHeight, panelWidth, waterHeight);
        g.setColor(new Color(34, 139, 34));
        for (LilyPad pad : lilyPads) {
            if (!pad.isSinking() && pad.y >= landHeight && pad.y + pad.height <= landHeight + waterHeight) {
                g.fillOval(pad.x, pad.y, pad.width, pad.height);
            }
        }
        g.setColor(Color.darkGray);
        g.fillRect(0, landHeight + waterHeight, panelWidth, roadHeight);
        g.setColor(Color.red);
        for (Car car : cars) g.fillRect(car.x, car.y, car.width, car.height);
        g.setColor(Color.white);
        for (int i = 0; i < 4; i++) {
            int y = landHeight + waterHeight + (laneHeight + laneGap) * i + laneHeight/2;
            drawDashedLine(g, 0, y, panelWidth, y);
        }
        int offset = getHopOffset();
        g.setColor(Color.yellow);
        g.fillRect(frogX, frogY - offset, frogSize, frogSize);
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
        double t = Math.PI * hopFrame / hopDuration;
        return (int)(Math.sin(t) * hopHeight);
    }

    private void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2) {
        float dash[] = {10.0f};
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
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
        for (Car car : cars) {
            car.move(panelWidth);
            if (car.intersects(frogX, frogY, frogSize, frogSize)) {
                playSound(squishClip);
                message = "YOU LOSE!";
                gameOver = true;
                timer.stop();
            }
        }
        for (LilyPad pad : lilyPads) pad.move(panelWidth);
        for (Frog f : frogs) {
            if (!f.isCollected() && f.getX() < frogX+frogSize && f.getX()+f.getWidth()>frogX &&
                    f.getY()<frogY+frogSize && f.getY()+f.getHeight()>frogY) {
                f.collect();
                frogsCollected++;
                playSound(ribbitClip);
                break;
            }
        }
        if (frogsCollected==5) {
            message = "YOU WIN!";
            gameOver = true;
            timer.stop();
        }
        if (frogY>=landHeight && frogY<landHeight+waterHeight) {
            boolean onPad=false;
            for (LilyPad pad:lilyPads) {
                if (!pad.isSinking() && frogX<pad.x+pad.width && frogX+frogSize>pad.x &&
                        frogY<pad.y+pad.height && frogY+frogSize>pad.y) {
                    onPad=true; break;
                }
            }
            if (!onPad) {
                playSound(drownClip);
                message="YOU DROWNED!";
                gameOver=true;
                timer.stop();
            }
        }
        repaint();
    }

    private void resetFrog() { frogX=250; frogY=550; targetX=frogX; targetY=frogY; isMoving=false; hopFrame=0; }
    private void restartGame() {
        resetFrog(); frogsCollected=0; frogs.forEach(f->f.collected=false); message=""; gameOver=false; timer.start();
    }

    @Override public void keyPressed(KeyEvent e) {
        if (gameOver && e.getKeyCode()==KeyEvent.VK_ENTER) { restartGame(); return;}
        if (isMoving) return;
        switch(e.getKeyCode()) {
            case KeyEvent.VK_LEFT:  frogX=Math.max(frogX-STEP,0); isMoving=true; break;
            case KeyEvent.VK_RIGHT: frogX=Math.min(frogX+STEP,panelWidth-frogSize); isMoving=true; break;
            case KeyEvent.VK_UP:    frogY=Math.max(frogY-STEP,0); isMoving=true; break;
            case KeyEvent.VK_DOWN:  frogY=Math.min(frogY+STEP,panelHeight-frogSize); isMoving=true; break;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // Inner classes: Car, LilyPad, Frog
    private static class Car { int x,y,width,height,speed; Car(int x,int y,int w,int h,int s){this.x=x;this.y=y;this.width=w;this.height=h;this.speed=s;} void move(int pw){x+=speed; if(speed>0&&x>pw)x=-width; if(speed<0&&x+width<0)x=pw;} boolean intersects(int fx,int fy,int fw,int fh){return fx<x+width&&fx+fw>x&&fy<y+height&&fy+fh>y;} }
    private static class LilyPad { int x,y,width,height,speed; boolean sinking=false; long st; LilyPad(int x,int y,int w,int h,int s){this.x=x;this.y=y;this.width=w;this.height=h;this.speed=s;} void move(int pw){x+=speed; if(x+width<0)x=pw; if(x>pw)x=-width; if(sinking&&System.currentTimeMillis()-st>2000){sinking=false;}} void sink(){if(!sinking){sinking=true;st=System.currentTimeMillis();y=panelHeight;}} boolean isSinking(){return sinking;} }
    private static class Frog { int x,y,width,height; Color color; boolean collected=false; Frog(int x,int y,int w,int h,Color c){this.x=x;this.y=y;this.width=w;this.height=h;this.color=c;} void collect(){collected=true;} boolean isCollected(){return collected;} int getX(){return x;} int getY(){return y;} int getWidth(){return width;} int getHeight(){return height;} Color getColor(){return color;} }

    private Color getRandomColor(){ Random r=new Random(); return new Color(r.nextInt(256),r.nextInt(256),r.nextInt(256)); }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Frogger Game");
        FroggerGame game = new FroggerGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setVisible(true);
    }
}


