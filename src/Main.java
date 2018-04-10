import org.jetbrains.annotations.Contract;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static Random rnd = new Random();

    static CreateMainWindow window = new CreateMainWindow(new MyKeyListener(), 40, 40);
    static int gunX = 20,
            gunY = 39;
    private static final char gunSymbol = '¤';

    static AtomicInteger hit = new AtomicInteger(0), miss = new AtomicInteger(0);
    static Semaphore bulletSemaphore = new Semaphore(3, true);
    private static ReentrantLock randomLock = new ReentrantLock();

    public static void main(String[] args) {
        window.setSymbol(gunX, gunY, gunSymbol, Color.BLACK);
        new CreateEnemy().start();
    }

    public static class MyKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) { }
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getExtendedKeyCode()){
                case KeyEvent.VK_RIGHT: Main.stepRight();
                    break;
                case KeyEvent.VK_LEFT: Main.stepLeft();
                    break;
                case KeyEvent.VK_UP: Main.stepUp();
                    break;
                case KeyEvent.VK_DOWN: Main.stepDown();
                    break;
                case KeyEvent.VK_SPACE: Main.OpenFire();
            }
        }
        @Override
        public void keyReleased(KeyEvent e) { }
    }

    public static class CreateMainWindow extends Frame{
        private static final int
                screenOffset = 8,
                baseX = 8, baseY = 31,
                symbolWidth = 16, symbolHeight = 16;
        private Font defaultFont = new Font(Font.MONOSPACED, Font.PLAIN, symbolHeight);

        private ReentrantLock screenLock = new ReentrantLock();

        CreateMainWindow(KeyListener keyListener, int screenWidth, int screenHeight){
            setSize(baseX + screenOffset + screenWidth * symbolWidth, baseY + screenOffset + screenHeight * symbolHeight);
            setLayout(null);
            setVisible(true);
            addKeyListener(keyListener);
        }
        @Contract(pure = true)
        private int toPixelCoordinateX(int x){
            return x * symbolWidth + baseX;
        }

        @Contract(pure = true)
        private int toPixelCoordinateY(int y){
            return y * symbolHeight + baseY;
        }

        void setSymbol(int x, int y, char symbol, Color color){
            Label label = new Label(String.valueOf(symbol));
            label.setFont(defaultFont);
            label.setForeground(color);
            label.setBounds(toPixelCoordinateX(x), toPixelCoordinateY(y), symbolWidth, symbolHeight);
            screenLock.lock();
            add(label);
            screenLock.unlock();
        }

        void removeSymbol(int x, int y){
            Component component = getComponentAt(toPixelCoordinateX(x),toPixelCoordinateY(y));
            screenLock.lock();
            remove(component);
            screenLock.unlock();
        }

        boolean isNotFree(int x, int y){
            return !getComponentAt(toPixelCoordinateX(x),toPixelCoordinateY(y)).getName().equals("frame0");
        }
    }

    public static class CreateEnemy extends Thread {
        private static final int enemyDelation = 1200;

        @Override
        public void run() {
            do {
                if (Main.getNextRandomInt(2) == 1)
                    new Enemy().start();
                try {
                    sleep(enemyDelation);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    public static class Enemy extends Thread {
        private static final int enemySleepingTime = 400;
        static final char enemy = '-';
        static final Color[] colorMas = {Color.BLUE, Color.GREEN, Color.RED};
        private static Random rnd = new Random();

        @Override
        public void run() {
            int enemyY = Main.getNextRandomInt(25);
            int increment, boundX, enemyX;

            if (enemyY % 2 == 0) {
                increment = 1;
                boundX = 39;
                enemyX = 1;
            } else {
                increment = -1;
                boundX = 0;
                enemyX = 39;
            }

            while (enemyX != boundX){
                if (!Main.window.isNotFree(enemyX, enemyY)) {
                    Main.window.setSymbol(enemyX, enemyY, enemy, colorMas[rnd.nextInt(3)]);
                } else {
                    Main.window.removeSymbol(enemyX, enemyY);
                    break;
                }
                try {
                    sleep(enemySleepingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (Main.window.isNotFree(enemyX, enemyY)) {
                    Main.window.removeSymbol(enemyX, enemyY);
                } else {
                    return;
                }

                enemyX += increment;
            }
        }
    }

    public static class Bullet extends Thread {
        private static final int bulletSleepPeriod = 25;
        static final char bullet = '^';

        @Override
        public void run() {
            if (!Main.bulletSemaphore.tryAcquire()) return;

            int bulletX = Main.gunX, bulletY = Main.gunY - 1;

            while(bulletY >= 0) {
                if (bulletY == 0)
                    Main.IncMiss();
                if (!Main.window.isNotFree(bulletX, bulletY)) {
                    Main.window.setSymbol(bulletX, bulletY, bullet, Color.BLACK);
                } else {
                    Main.IncHit();
                    Main.window.removeSymbol(bulletX, bulletY);
                    break;
                }
                try {
                    sleep(bulletSleepPeriod);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Main.window.removeSymbol(bulletX, bulletY);
                bulletY--;
            }

            Main.bulletSemaphore.release();
        }
    }

    static void stepLeft() {
        if (gunX - 1 >= 0) {
            window.removeSymbol(gunX, gunY);
            window.setSymbol(--gunX, gunY, gunSymbol, Color.BLACK);
        }
    }

    static void stepRight() {
        if (gunX + 1 < 40) {
            window.removeSymbol(gunX, gunY);
            window.setSymbol(++gunX, gunY, gunSymbol, Color.BLACK);
        }
    }

    static void stepUp() {
        if (gunY + 1 > 25) {
            window.removeSymbol(gunX, gunY);
            window.setSymbol(gunX, --gunY, gunSymbol, Color.BLACK);
        }
    }

    static void stepDown() {
        if (gunY + 1 < 40) {
            window.removeSymbol(gunX, gunY);
            window.setSymbol(gunX, ++gunY, gunSymbol, Color.BLACK);
        }
    }

    static void OpenFire(){
        new Bullet().start();
    }

    static int getNextRandomInt(int limit){
        randomLock.lock();
        int randomInt = rnd.nextInt(limit);
        randomLock.unlock();
        return randomInt;
    }

    static void IncHit(){
        hit.addAndGet(1);
    }

    static void IncMiss(){
        miss.addAndGet(1);
    }
}