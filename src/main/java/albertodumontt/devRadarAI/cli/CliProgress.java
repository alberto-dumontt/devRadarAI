package albertodumontt.devRadarAI.cli;

import org.springframework.stereotype.Component;

@Component
public class CliProgress {

    private static final String[] FRAMES = {"|", "/", "-", "\\"};
    private static final int LINE_WIDTH = 70;

    private volatile String status = "";
    private Thread spinnerThread;

    public void start(String initialStatus) {
        this.status = initialStatus;
        spinnerThread = new Thread(() -> {
            int i = 0;
            while (!Thread.currentThread().isInterrupted()) {
                String line = "  " + FRAMES[i % FRAMES.length] + "  " + status;
                System.out.print("\r" + line + " ".repeat(Math.max(0, LINE_WIDTH - line.length())));
                System.out.flush();
                i++;
                try {
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        spinnerThread.setDaemon(true);
        spinnerThread.start();
    }

    public void update(String newStatus) {
        this.status = newStatus;
    }

    public void stop() {
        if (spinnerThread != null) spinnerThread.interrupt();
        System.out.print("\r" + " ".repeat(LINE_WIDTH) + "\r");
        System.out.flush();
    }
}
