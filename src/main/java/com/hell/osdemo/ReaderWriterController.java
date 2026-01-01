package com.hell.osdemo;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReaderWriterController implements Initializable {

    // FXML组件
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button resetBtn;
    @FXML private ChoiceBox<String> strategyChoice;

    @FXML private FlowPane readersPane;
    @FXML private FlowPane writersPane;

    @FXML private Label readerCountLabel;
    @FXML private Label activeReadersLabel;
    @FXML private Label writerStatusLabel;
    @FXML private Label activeWriterLabel;
    @FXML private Label currentStrategyLabel;
    @FXML private Label totalReadCountLabel;
    @FXML private Label totalWriteCountLabel;
    @FXML private TextArea logArea;

    // 常量
    private static final int READER_COUNT = 3;
    private static final int WRITER_COUNT = 2;

    // 同步控制
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Semaphore readerSemaphore = new Semaphore(READER_COUNT);
    private Semaphore writerSemaphore = new Semaphore(1);
    private int currentReaders = 0;
    private boolean isWriting = false;

    // 线程相关
    private ExecutorService executor;
    private List<ReaderThread> readerThreads;
    private List<WriterThread> writerThreads;
    private volatile boolean isRunning = false;
    private AnimationTimer animationTimer;

    // 状态记录
    private enum ReaderState { THINKING, WAITING, READING }
    private enum WriterState { THINKING, WAITING, WRITING }

    private List<ReaderState> readerStates;
    private List<WriterState> writerStates;
    private List<Rectangle> readerRectangles;
    private List<Rectangle> writerRectangles;
    private List<Text> readerLabels;
    private List<Text> writerLabels;

    // 统计
    private int totalReadCount = 0;
    private int totalWriteCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeInterface();
        setupStrategies();
        log("系统初始化完成，等待开始模拟");
    }

    private void initializeInterface() {
        // 清空容器
        readersPane.getChildren().clear();
        writersPane.getChildren().clear();

        // 初始化状态列表
        readerStates = new ArrayList<>();
        writerStates = new ArrayList<>();
        readerRectangles = new ArrayList<>();
        writerRectangles = new ArrayList<>();
        readerLabels = new ArrayList<>();
        writerLabels = new ArrayList<>();

        // 创建读者卡片
        for (int i = 0; i < READER_COUNT; i++) {
            VBox readerCard = createPersonCard(i, "读者", Color.LIGHTBLUE, Color.DARKBLUE);
            readersPane.getChildren().add(readerCard);

            Rectangle rect = (Rectangle) readerCard.getChildren().get(0);
            Text label = (Text) readerCard.getChildren().get(1);
            Text status = (Text) readerCard.getChildren().get(2);

            readerRectangles.add(rect);
            readerLabels.add(status);
            readerStates.add(ReaderState.THINKING);
        }

        // 创建写者卡片
        for (int i = 0; i < WRITER_COUNT; i++) {
            VBox writerCard = createPersonCard(i, "写者", Color.LIGHTCORAL, Color.DARKRED);
            writersPane.getChildren().add(writerCard);

            Rectangle rect = (Rectangle) writerCard.getChildren().get(0);
            Text label = (Text) writerCard.getChildren().get(1);
            Text status = (Text) writerCard.getChildren().get(2);

            writerRectangles.add(rect);
            writerLabels.add(status);
            writerStates.add(WriterState.THINKING);
        }

        // 初始化线程
        readerThreads = new ArrayList<>();
        writerThreads = new ArrayList<>();

        for (int i = 0; i < READER_COUNT; i++) {
            final int readerId = i;
            readerThreads.add(new ReaderThread(readerId));
        }

        for (int i = 0; i < WRITER_COUNT; i++) {
            final int writerId = i;
            writerThreads.add(new WriterThread(writerId));
        }
    }

    private VBox createPersonCard(int id, String type, Color fillColor, Color strokeColor) {
        VBox card = new VBox(5);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setStyle("-fx-padding: 10;");

        // 矩形表示人物
        Rectangle rect = new Rectangle(60, 80);
        rect.setFill(fillColor);
        rect.setStroke(strokeColor);
        rect.setStrokeWidth(2);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        // 标识标签
        Text label = new Text(type + id);
        label.setStyle("-fx-font-weight: bold;");

        // 状态标签
        Text status = new Text("思考中");
        status.setStyle("-fx-font-size: 11;");

        card.getChildren().addAll(rect, label, status);
        return card;
    }

    private void setupStrategies() {
        // 设置默认策略
        strategyChoice.getSelectionModel().selectFirst();
        currentStrategyLabel.setText("当前策略: 读者优先");

        // 策略选择监听
        strategyChoice.setOnAction(e -> {
            String strategy = strategyChoice.getValue();
            currentStrategyLabel.setText("当前策略: " + strategy);

            if (isRunning) {
                resetSimulation();
                startSimulation();
            } else {
                log("已选择策略: " + strategy);
            }
        });
    }

    @FXML
    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;
            startBtn.setDisable(true);
            pauseBtn.setDisable(false);

            log("开始模拟 - 策略: " + strategyChoice.getValue());

            // 创建线程池
            executor = Executors.newFixedThreadPool(READER_COUNT + WRITER_COUNT);

            // 启动所有读者线程
            for (ReaderThread thread : readerThreads) {
                thread.reset();
                executor.execute(thread);
            }

            // 启动所有写者线程
            for (WriterThread thread : writerThreads) {
                thread.reset();
                executor.execute(thread);
            }

            // 启动UI更新定时器
            startUITimer();
        }
    }

    @FXML
    private void pauseSimulation() {
        if (isRunning) {
            isRunning = false;
            startBtn.setDisable(false);
            pauseBtn.setDisable(true);

            log("模拟暂停");

            // 暂停所有线程
            readerThreads.forEach(ReaderThread::pause);
            writerThreads.forEach(WriterThread::pause);

            // 停止UI定时器
            if (animationTimer != null) {
                animationTimer.stop();
            }
        }
    }

    @FXML
    private void resetSimulation() {
        // 停止模拟
        if (executor != null) {
            executor.shutdownNow();
        }

        if (animationTimer != null) {
            animationTimer.stop();
        }

        // 重置状态
        isRunning = false;
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        currentReaders = 0;
        isWriting = false;
        totalReadCount = 0;
        totalWriteCount = 0;

        // 重置同步对象
        lock = new ReentrantReadWriteLock();
        readerSemaphore = new Semaphore(READER_COUNT);
        writerSemaphore = new Semaphore(1);

        // 重置状态显示
        initializeInterface();
        updateStatusDisplay();

        // 清空日志
        logArea.clear();
        log("模拟已重置");
    }

    @FXML
    private void clearLog() {
        logArea.clear();
        log("日志已清空");
    }

    private void startUITimer() {
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 100_000_000) { // 100ms更新一次
                    updateStatusDisplay();
                    lastUpdate = now;
                }
            }
        };
        animationTimer.start();
    }

    private void updateStatusDisplay() {
        Platform.runLater(() -> {
            // 更新读者状态显示
            for (int i = 0; i < READER_COUNT; i++) {
                Rectangle rect = readerRectangles.get(i);
                Text statusLabel = readerLabels.get(i);

                switch (readerStates.get(i)) {
                    case THINKING:
                        rect.setFill(Color.LIGHTBLUE);
                        statusLabel.setText("思考中");
                        break;
                    case WAITING:
                        rect.setFill(Color.LIGHTYELLOW);
                        statusLabel.setText("等待中");
                        break;
                    case READING:
                        rect.setFill(Color.LIGHTGREEN);
                        statusLabel.setText("阅读中");
                        break;
                }
            }

            // 更新写者状态显示
            for (int i = 0; i < WRITER_COUNT; i++) {
                Rectangle rect = writerRectangles.get(i);
                Text statusLabel = writerLabels.get(i);

                switch (writerStates.get(i)) {
                    case THINKING:
                        rect.setFill(Color.LIGHTCORAL);
                        statusLabel.setText("思考中");
                        break;
                    case WAITING:
                        rect.setFill(Color.LIGHTYELLOW);
                        statusLabel.setText("等待中");
                        break;
                    case WRITING:
                        rect.setFill(Color.LIGHTGREEN);
                        statusLabel.setText("写作中");
                        break;
                }
            }

            // 更新统计信息
            readerCountLabel.setText(String.valueOf(currentReaders));
            writerStatusLabel.setText(isWriting ? "写作中" : "等待");

            // 更新活跃用户显示
            List<String> activeReaders = new ArrayList<>();
            for (int i = 0; i < READER_COUNT; i++) {
                if (readerStates.get(i) == ReaderState.READING) {
                    activeReaders.add("读者" + i);
                }
            }
            activeReadersLabel.setText(activeReaders.isEmpty() ? "无" : String.join(", ", activeReaders));

            String activeWriter = "无";
            for (int i = 0; i < WRITER_COUNT; i++) {
                if (writerStates.get(i) == WriterState.WRITING) {
                    activeWriter = "写者" + i;
                    break;
                }
            }
            activeWriterLabel.setText(activeWriter);

            // 更新统计计数
            totalReadCountLabel.setText(String.valueOf(totalReadCount));
            totalWriteCountLabel.setText(String.valueOf(totalWriteCount));
        });
    }

    private void log(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            logArea.appendText("[" + time + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // 读者线程类
    class ReaderThread implements Runnable {
        private final int id;
        private volatile boolean running = true;
        private volatile boolean paused = false;

        public ReaderThread(int id) {
            this.id = id;
        }

        public void pause() {
            paused = true;
        }

        public void reset() {
            running = true;
            paused = false;
            readerStates.set(id, ReaderState.THINKING);
        }

        @Override
        public void run() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    if (paused) {
                        Thread.sleep(100);
                        continue;
                    }

                    // 思考阶段
                    readerStates.set(id, ReaderState.THINKING);
                    log("读者" + id + " 开始思考");
                    Thread.sleep(1000 + (int)(Math.random() * 2000));

                    // 尝试阅读
                    readerStates.set(id, ReaderState.WAITING);
                    log("读者" + id + " 尝试阅读");

                    // 根据策略尝试获取读锁
                    if (tryRead()) {
                        readerStates.set(id, ReaderState.READING);
                        totalReadCount++;

                        // 阅读阶段
                        log("读者" + id + " 开始阅读，当前读者数: " + currentReaders);
                        Thread.sleep(1500 + (int)(Math.random() * 2000));

                        // 结束阅读
                        finishRead();
                        log("读者" + id + " 结束阅读，当前读者数: " + currentReaders);
                    } else {
                        Thread.sleep(500); // 等待后重试
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private boolean tryRead() {
            String strategy = strategyChoice.getValue();

            try {
                switch (strategy) {
                    case "读者优先":
                        // 读者优先：读者可以直接进入
                        lock.readLock().lock();
                        currentReaders++;
                        return true;

                    case "写者优先":
                        // 写者优先：读者需要等待写者完成
                        if (readerSemaphore.tryAcquire()) {
                            lock.readLock().lock();
                            currentReaders++;
                            return true;
                        }
                        return false;

                    case "公平策略":
                        // 公平策略：使用公平锁
                        if (lock.readLock().tryLock()) {
                            currentReaders++;
                            return true;
                        }
                        return false;

                    default:
                        lock.readLock().lock();
                        currentReaders++;
                        return true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        private void finishRead() {
            currentReaders--;
            lock.readLock().unlock();
            if (strategyChoice.getValue().equals("写者优先")) {
                readerSemaphore.release();
            }
        }
    }

    // 写者线程类
    class WriterThread implements Runnable {
        private final int id;
        private volatile boolean running = true;
        private volatile boolean paused = false;

        public WriterThread(int id) {
            this.id = id;
        }

        public void pause() {
            paused = true;
        }

        public void reset() {
            running = true;
            paused = false;
            writerStates.set(id, WriterState.THINKING);
        }

        @Override
        public void run() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    if (paused) {
                        Thread.sleep(100);
                        continue;
                    }

                    // 思考阶段
                    writerStates.set(id, WriterState.THINKING);
                    log("写者" + id + " 开始思考");
                    Thread.sleep(1500 + (int)(Math.random() * 2000));

                    // 尝试写作
                    writerStates.set(id, WriterState.WAITING);
                    log("写者" + id + " 尝试写作");

                    // 根据策略尝试获取写锁
                    if (tryWrite()) {
                        writerStates.set(id, WriterState.WRITING);
                        isWriting = true;
                        totalWriteCount++;

                        // 写作阶段
                        log("写者" + id + " 开始写作");
                        Thread.sleep(2000 + (int)(Math.random() * 2000));

                        // 结束写作
                        finishWrite();
                        log("写者" + id + " 结束写作");
                    } else {
                        Thread.sleep(500); // 等待后重试
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private boolean tryWrite() {
            String strategy = strategyChoice.getValue();

            try {
                switch (strategy) {
                    case "读者优先":
                        // 读者优先：写者需要等待没有读者
                        if (writerSemaphore.tryAcquire()) {
                            lock.writeLock().lock();
                            return true;
                        }
                        return false;

                    case "写者优先":
                        // 写者优先：写者优先获取锁
                        lock.writeLock().lock();
                        return true;

                    case "公平策略":
                        // 公平策略：使用公平锁
                        if (lock.writeLock().tryLock()) {
                            return true;
                        }
                        return false;

                    default:
                        lock.writeLock().lock();
                        return true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        private void finishWrite() {
            isWriting = false;
            lock.writeLock().unlock();
            if (strategyChoice.getValue().equals("读者优先")) {
                writerSemaphore.release();
            }
        }
    }
}