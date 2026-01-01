package com.hell.osdemo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class DiningPhilosophersController implements Initializable {

    // FXML组件
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button resetBtn;
    @FXML private ChoiceBox<String> strategyChoice;

    @FXML private Label statusLabel;
    @FXML private Label currentStrategyLabel;
    @FXML private Label runningStatusLabel;
    @FXML private Label eatCountLabel;
    @FXML private Label deadlockWarningLabel;
    @FXML private TextArea logArea;

    @FXML private Pane chopsticksPane;
    @FXML private Pane philosophersPane;
    @FXML private Circle tableCircle;

    // 常量
    private static final int NUM_PHILOSOPHERS = 5;
    private static final double PHILOSOPHER_RADIUS = 25;
    private static final double CHOPSTICK_LENGTH = 40;

    // 多线程相关
    private ExecutorService executor;
    private List<PhilosopherThread> philosopherThreads;
    private volatile boolean isRunning = false;
    private ScheduledExecutorService uiScheduler;

    // 哲学家和筷子
    private List<Philosopher> philosophers;
    private List<Fork> forks;

    // 图形元素
    private List<Circle> philosopherCircles;
    private List<Line> chopstickLines;

    // 统计
    private int totalEatCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 等待StackPane布局完成后再初始化位置
        Platform.runLater(() -> {
            initializeTable();
            setupStrategies();
            log("系统初始化完成，等待开始模拟");
        });
    }

    private void initializeTable() {
        // 清除现有元素
        chopsticksPane.getChildren().clear();
        philosophersPane.getChildren().clear();

        // 初始化数据
        philosophers = new ArrayList<>();
        forks = new ArrayList<>();
        philosopherCircles = new ArrayList<>();
        chopstickLines = new ArrayList<>();
        philosopherThreads = new ArrayList<>();

        // 获取StackPane的实际大小
        double containerWidth = philosophersPane.getWidth();
        double containerHeight = philosophersPane.getHeight();

        // 如果容器大小未确定，使用默认值
        if (containerWidth <= 0 || containerHeight <= 0) {
            containerWidth = 400;
            containerHeight = 400;
        }

        double centerX = containerWidth / 2;
        double centerY = containerHeight / 2;
        double tableRadius = Math.min(centerX, centerY) * 0.8; // 圆桌半径为容器尺寸的80%

        // 设置圆桌位置和大小
        tableCircle.setCenterX(centerX);
        tableCircle.setCenterY(centerY);
        tableCircle.setRadius(tableRadius);

        // 创建筷子
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks.add(new Fork(i));
        }

        // 创建哲学家图形和对象
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            double angle = 2 * Math.PI * i / NUM_PHILOSOPHERS;

            // 哲学家位置（在圆桌边缘）
            double philosopherX = centerX + (tableRadius - PHILOSOPHER_RADIUS - 10) * Math.cos(angle);
            double philosopherY = centerY + (tableRadius - PHILOSOPHER_RADIUS - 10) * Math.sin(angle);

            // 创建哲学家图形
            Circle circle = new Circle(PHILOSOPHER_RADIUS);
            circle.setCenterX(philosopherX);
            circle.setCenterY(philosopherY);
            circle.setFill(Color.LIGHTBLUE); // 思考状态
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(2);

            Text label = new Text("P" + i);
            label.setX(philosopherX - 6);
            label.setY(philosopherY + 5);
            label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            philosopherCircles.add(circle);
            philosophersPane.getChildren().addAll(circle, label);

            // 创建哲学家对象
            Philosopher philosopher = new Philosopher(i, forks.get(i), forks.get((i + 1) % NUM_PHILOSOPHERS));
            philosophers.add(philosopher);

            // 创建哲学家线程
            philosopherThreads.add(new PhilosopherThread(philosopher));

            // 筷子位置（在哲学家之间）
            double chopstickAngle = angle + Math.PI / NUM_PHILOSOPHERS;
            double chopstickX = centerX + tableRadius * 0.7 * Math.cos(chopstickAngle);
            double chopstickY = centerY + tableRadius * 0.7 * Math.sin(chopstickAngle);

            // 计算筷子端点
            double endX = chopstickX + CHOPSTICK_LENGTH * Math.cos(chopstickAngle);
            double endY = chopstickY + CHOPSTICK_LENGTH * Math.sin(chopstickAngle);

            Line chopstick = new Line();
            chopstick.setStartX(chopstickX);
            chopstick.setStartY(chopstickY);
            chopstick.setEndX(endX);
            chopstick.setEndY(endY);
            chopstick.setStrokeWidth(4);
            chopstick.setStroke(Color.GOLD);
            chopstick.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

            chopstickLines.add(chopstick);
            chopsticksPane.getChildren().add(chopstick);
        }
    }

    // 添加容器大小监听，动态调整布局
    private void setupContainerListeners() {
        // 监听容器大小变化
        philosophersPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                Platform.runLater(this::initializeTable);
            }
        });

        philosophersPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                Platform.runLater(this::initializeTable);
            }
        });
    }

    private void setupStrategies() {
        // 设置默认策略
        strategyChoice.getSelectionModel().selectFirst();
        currentStrategyLabel.setText("当前策略: 无策略");

        // 策略选择监听
        strategyChoice.setOnAction(e -> {
            String strategy = strategyChoice.getValue();
            currentStrategyLabel.setText("当前策略: " + strategy);

            if (isRunning) {
                // 如果正在运行，重新开始以应用新策略
                resetSimulation();
                startSimulation();
            } else {
                log("已选择策略: " + strategy);
            }
        });

        // 设置容器监听
        setupContainerListeners();
    }

    // 其他方法保持不变...
    @FXML
    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;
            startBtn.setDisable(true);
            pauseBtn.setDisable(false);
            resetBtn.setDisable(false);
            runningStatusLabel.setText("运行中");
            statusLabel.setText("模拟运行中...");

            String strategy = strategyChoice.getValue();
            log("开始模拟 - 策略: " + strategy);

            // 创建线程池
            executor = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

            // 启动所有哲学家线程
            Strategy currentStrategy = getCurrentStrategy();
            for (PhilosopherThread thread : philosopherThreads) {
                thread.setStrategy(currentStrategy);
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
            runningStatusLabel.setText("已暂停");
            statusLabel.setText("模拟已暂停");

            log("模拟暂停");

            // 暂停所有哲学家线程
            for (PhilosopherThread thread : philosopherThreads) {
                thread.pause();
            }

            // 停止UI更新
            if (uiScheduler != null) {
                uiScheduler.shutdownNow();
            }
        }
    }

    @FXML
    private void resetSimulation() {
        // 停止UI更新
        if (uiScheduler != null) {
            uiScheduler.shutdownNow();
            uiScheduler = null;
        }

        // 停止模拟线程
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        // 重置状态
        isRunning = false;
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        resetBtn.setDisable(false);
        runningStatusLabel.setText("已停止");
        statusLabel.setText("等待开始模拟...");
        deadlockWarningLabel.setText("");
        totalEatCount = 0;
        eatCountLabel.setText("0");

        // 重置所有哲学家
        philosophers.forEach(Philosopher::reset);
        forks.forEach(Fork::reset);

        // 重置哲学家线程
        for (PhilosopherThread thread : philosopherThreads) {
            thread.reset();
        }

        // 重置显示
        updateDisplay();

        log("模拟已重置");
    }

    @FXML
    private void clearLog() {
        logArea.clear();
        log("日志已清空");
    }

    private Strategy getCurrentStrategy() {
        String strategy = strategyChoice.getValue();
        switch (strategy) {
            case "限制进餐人数":
                return Strategy.LIMIT_DINERS;
            case "确保左右筷子可用":
                return Strategy.ENSURE_BOTH_CHOPSTICKS;
            default:
                return Strategy.NO_STRATEGY;
        }
    }

    private void startUITimer() {
        // 使用定时任务更新UI
        uiScheduler = Executors.newSingleThreadScheduledExecutor();
        uiScheduler.scheduleAtFixedRate(() -> {
            if (isRunning) {
                Platform.runLater(() -> {
                    updateDisplay();
                    checkDeadlock();
                });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void updateDisplay() {
        // 更新哲学家状态
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            Philosopher philosopher = philosophers.get(i);
            Circle circle = philosopherCircles.get(i);

            // 更新哲学家颜色
            switch (philosopher.getState()) {
                case THINKING:
                    circle.setFill(Color.LIGHTBLUE);
                    break;
                case HUNGRY:
                    circle.setFill(Color.ORANGE);
                    break;
                case EATING:
                    circle.setFill(Color.LIGHTGREEN);
                    break;
            }

            // 更新筷子颜色
            Fork leftFork = forks.get(i);
            Fork rightFork = forks.get((i + 1) % NUM_PHILOSOPHERS);

            Line leftChopstick = chopstickLines.get(i);
            Line rightChopstick = chopstickLines.get((i + 1) % NUM_PHILOSOPHERS);

            if (leftFork.isTaken()) {
                leftChopstick.setStroke(Color.RED);
            } else {
                leftChopstick.setStroke(Color.GOLD);
            }

            if (rightFork.isTaken()) {
                rightChopstick.setStroke(Color.RED);
            } else {
                rightChopstick.setStroke(Color.GOLD);
            }
        }

        // 更新统计
        totalEatCount = philosophers.stream().mapToInt(Philosopher::getEatCount).sum();
        eatCountLabel.setText(String.valueOf(totalEatCount));
    }

    private void checkDeadlock() {
        boolean allHungry = philosophers.stream()
                .allMatch(p -> p.getState() == Philosopher.State.HUNGRY);
        boolean allForksTaken = forks.stream()
                .allMatch(Fork::isTaken);

        if (allHungry && allForksTaken) {
            deadlockWarningLabel.setText("⚠️ 检测到死锁！所有哲学家都饥饿且叉子被占用");
            statusLabel.setText("死锁状态！");
        } else {
            deadlockWarningLabel.setText("");
        }
    }

    private void log(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            logArea.appendText("[" + time + "] " + message + "\n");
            // 自动滚动到底部
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    // 策略枚举
    enum Strategy {
        NO_STRATEGY,
        LIMIT_DINERS,
        ENSURE_BOTH_CHOPSTICKS
    }

    // 哲学家类
    class Philosopher {
        enum State {
            THINKING,
            HUNGRY,
            EATING
        }

        private final int id;
        private volatile State state = State.THINKING;
        private final Fork leftFork;
        private final Fork rightFork;
        private int eatCount = 0;

        public Philosopher(int id, Fork leftFork, Fork rightFork) {
            this.id = id;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        public void think() throws InterruptedException {
            state = State.THINKING;
            log("哲学家 P" + id + " 开始思考");
            Thread.sleep(1000 + (int)(Math.random() * 2000));
        }

        public void eat() throws InterruptedException {
            state = State.EATING;
            eatCount++;
            log("哲学家 P" + id + " 开始就餐 (第" + eatCount + "次)");
            Thread.sleep(800 + (int)(Math.random() * 1500));

            // 放下筷子
            leftFork.release();
            rightFork.release();

            log("哲学家 P" + id + " 吃完放下筷子");
        }

        public State getState() {
            return state;
        }

        public int getEatCount() {
            return eatCount;
        }

        public Fork getLeftFork() {
            return leftFork;
        }

        public Fork getRightFork() {
            return rightFork;
        }

        public void reset() {
            state = State.THINKING;
            eatCount = 0;
            leftFork.release();
            rightFork.release();
        }
    }

    // 哲学家线程类
    class PhilosopherThread implements Runnable {
        private final Philosopher philosopher;
        private Strategy strategy;
        private volatile boolean running = true;
        private volatile boolean paused = false;

        // 用于限制进餐人数的信号量（最多允许4个哲学家同时尝试就餐）
        private static final Semaphore dinerSemaphore = new Semaphore(NUM_PHILOSOPHERS - 1);

        public PhilosopherThread(Philosopher philosopher) {
            this.philosopher = philosopher;
        }

        public void setStrategy(Strategy strategy) {
            this.strategy = strategy;
        }

        public void pause() {
            paused = true;
        }

        public void reset() {
            running = true;
            paused = false;
        }

        @Override
        public void run() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    if (paused) {
                        Thread.sleep(100);
                        continue;
                    }

                    // 思考
                    philosopher.think();

                    // 饥饿
                    philosopher.state = Philosopher.State.HUNGRY;
                    log("哲学家 P" + philosopher.id + " 饿了，尝试拿筷子");

                    // 尝试就餐（根据策略）
                    boolean canEat = tryToEat();

                    if (canEat) {
                        philosopher.eat();
                    } else {
                        // 拿不到筷子，等待一段时间
                        Thread.sleep(300 + (int)(Math.random() * 500));
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private boolean tryToEat() throws InterruptedException {
            switch (strategy) {
                case NO_STRATEGY:
                    return tryEatNoStrategy();
                case LIMIT_DINERS:
                    return tryEatLimitDiners();
                case ENSURE_BOTH_CHOPSTICKS:
                    return tryEatEnsureBothChopsticks();
                default:
                    return tryEatNoStrategy();
            }
        }

        private boolean tryEatNoStrategy() {
            // 无策略：直接尝试拿筷子
            if (philosopher.getLeftFork().tryTake() && philosopher.getRightFork().tryTake()) {
                return true;
            }
            // 拿不到就释放
            philosopher.getLeftFork().release();
            philosopher.getRightFork().release();
            return false;
        }

        private boolean tryEatLimitDiners() throws InterruptedException {
            // 限制进餐人数：最多允许4个哲学家同时尝试就餐
            if (dinerSemaphore.tryAcquire()) {
                try {
                    if (philosopher.getLeftFork().tryTake() && philosopher.getRightFork().tryTake()) {
                        return true;
                    }
                    philosopher.getLeftFork().release();
                    philosopher.getRightFork().release();
                } finally {
                    dinerSemaphore.release();
                }
            }
            return false;
        }

        private boolean tryEatEnsureBothChopsticks() {
            // 确保左右筷子可用：原子性地获取两个筷子
            synchronized (philosopher.getLeftFork()) {
                synchronized (philosopher.getRightFork()) {
                    if (!philosopher.getLeftFork().isTaken() && !philosopher.getRightFork().isTaken()) {
                        philosopher.getLeftFork().take();
                        philosopher.getRightFork().take();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // 叉子类
    class Fork {
        private final int id;
        private volatile boolean taken = false;

        public Fork(int id) {
            this.id = id;
        }

        public synchronized boolean tryTake() {
            if (!taken) {
                taken = true;
                return true;
            }
            return false;
        }

        public synchronized void take() {
            taken = true;
        }

        public synchronized void release() {
            taken = false;
        }

        public boolean isTaken() {
            return taken;
        }

        public void reset() {
            taken = false;
        }
    }
}