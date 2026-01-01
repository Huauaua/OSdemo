package com.hell.osdemo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;

public class ProducerConsumerController implements Initializable {

    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Label producerStatus;
    @FXML private Label consumerStatus;
    @FXML private Label bufferCount;
    @FXML private HBox bufferContainer;
    @FXML private TextArea logArea;

    private static final int BUFFER_SIZE = 5;
    private String[] buffer; // 共享缓冲区
    private boolean isRunning = false;

    // 同步信号量
    private Semaphore emptySlots;    // 空槽位信号量
    private Semaphore fullSlots;     // 满槽位信号量
    private Semaphore mutex;         // 互斥信号量

    private Thread producerThread;
    private Thread consumerThread;
    private int itemId = 0;
    private int in = 0;  // 生产者指针
    private int out = 0; // 消费者指针


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeBuffer();
        initializeSemaphores();
        pauseButton.setDisable(true);
    }

    private void initializeBuffer() {
        buffer = new String[BUFFER_SIZE];
        bufferContainer.getChildren().clear();

        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = null;
            VBox slotContainer = createBufferSlot(i);
            bufferContainer.getChildren().add(slotContainer);
        }
        updateBufferCount();
    }

    private void initializeSemaphores() {
        emptySlots = new Semaphore(BUFFER_SIZE); // 初始有BUFFER_SIZE个空槽位
        fullSlots = new Semaphore(0);            // 初始没有满槽位
        mutex = new Semaphore(1);                // 二进制信号量，用于互斥
    }

    private VBox createBufferSlot(int index) {
        VBox slotContainer = new VBox(5);
        slotContainer.setAlignment(javafx.geometry.Pos.CENTER);

        Rectangle rect = new Rectangle(60, 60);
        rect.setArcWidth(5);
        rect.setArcHeight(5);
        rect.setStroke(javafx.scene.paint.Color.BLACK);
        rect.setStrokeWidth(2);
        rect.setFill(javafx.scene.paint.Color.LIGHTGRAY);

        Text text = new Text("空");

        slotContainer.getChildren().addAll(rect, text);
        return slotContainer;
    }

    @FXML
    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;

            // 启动生产者线程
            producerThread = new Thread(this::producer);
            producerThread.setDaemon(true);

            // 启动消费者线程
            consumerThread = new Thread(this::consumer);
            consumerThread.setDaemon(true);

            producerThread.start();
            consumerThread.start();

            logMessage("信号量同步模拟开始...");
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        }
    }

    private void producer() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Platform.runLater(() -> producerStatus.setText("等待生产..."));

                // 生产者算法
                String item = "物品" + (++itemId);

                logMessage("生产者准备生产: " + item);

                emptySlots.acquire(); // 等待空槽位
                mutex.acquire();      // 进入临界区

                // 生产物品
                buffer[in] = item;
                int currentIn = in;
                in = (in + 1) % BUFFER_SIZE;

                Platform.runLater(() -> {
                    updateBufferSlot(currentIn, item, true);
                    logMessage("[线程消息] 生产者生产了: " + item + " [位置:" + currentIn + "]");
                    producerStatus.setText("生产中");
                });

                mutex.release(); // 离开临界区
                fullSlots.release(); // 增加一个满槽位

                // 模拟生产时间
                Thread.sleep((long) (Math.random() * 800 + 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void consumer() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Platform.runLater(() -> consumerStatus.setText("等待消费..."));

                // 消费者算法
                logMessage("消费者准备消费...");

                fullSlots.acquire(); // 等待满槽位
                mutex.acquire();     // 进入临界区

                // 消费物品
                String item = buffer[out];
                int currentOut = out;
                buffer[out] = null;
                out = (out + 1) % BUFFER_SIZE;

                Platform.runLater(() -> {
                    updateBufferSlot(currentOut, "空", false);
                    logMessage("[线程消息] 消费者消费了: " + item + " [位置:" + currentOut + "]");
                    consumerStatus.setText("消费中");
                });

                mutex.release(); // 离开临界区
                emptySlots.release(); // 增加一个空槽位

                // 模拟消费时间
                Thread.sleep((long) (Math.random() * 800 + 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateBufferSlot(int index, String text, boolean isFull) {
        VBox slotContainer = (VBox) bufferContainer.getChildren().get(index);
        Rectangle rect = (Rectangle) slotContainer.getChildren().get(0);
        Text textNode = (Text) slotContainer.getChildren().get(1);

        if (isFull) {
            rect.setFill(javafx.scene.paint.Color.GREEN);
        } else {
            rect.setFill(javafx.scene.paint.Color.LIGHTGRAY);
        }
        textNode.setText(text);
        updateBufferCount();
    }

    private void updateBufferCount() {
        int count = (int) Arrays.stream(buffer).filter(Objects::nonNull).count();

        Platform.runLater(() -> {
            bufferCount.setText(count + "/" + BUFFER_SIZE);

            // 显示信号量状态
            String semaphoreInfo = String.format(" [空:%d 满:%d 互斥:%s]",
                    emptySlots.availablePermits(),
                    fullSlots.availablePermits(),
                    mutex.availablePermits() > 0 ? "空闲" : "锁定");
            bufferCount.setText(count + "/" + BUFFER_SIZE + semaphoreInfo);
        });
    }

    private void logMessage(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() ->
                logArea.appendText("[" + timestamp + "] " + message + "\n"));
    }

    @FXML
    private void pauseSimulation() {
        if (isRunning) {
            isRunning = false;

            if (producerThread != null) {
                producerThread.interrupt();
            }
            if (consumerThread != null) {
                consumerThread.interrupt();
            }

            logMessage("模拟暂停");
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        }
    }

    @FXML
    private void resetSimulation() {
        pauseSimulation();

        // 重置信号量
        initializeSemaphores();

        // 清空缓冲区
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = null;
        }
        in = 0;
        out = 0;
        itemId = 0;

        Platform.runLater(() -> {
            initializeBuffer();
            producerStatus.setText("等待");
            consumerStatus.setText("等待");
            logArea.clear();
            logMessage("模拟已重置 - 信号量重新初始化");
        });
    }
}