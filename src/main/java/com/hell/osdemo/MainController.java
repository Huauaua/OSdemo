package com.hell.osdemo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class MainController {

    @FXML private StackPane demoPane;
    @FXML private Text infoText;

    @FXML private Button producerConsumerBtn;
    @FXML private Button diningPhilosopherBtn;
    @FXML private Button readerWriterBtn;

    private String currentDemoType = "none";

    @FXML
    public void initialize() {

    }

    @FXML
    private void showProducerConsumerDemo() {
        setActiveButton(producerConsumerBtn);
        currentDemoType = "producerConsumer";
        infoText.setText("生产者-消费者问题演示\n使用信号量实现进程同步");

        // 加载生产者-消费者演示界面
        loadDemoContent("ProducerConsumerDemo.fxml");
    }

    @FXML
    private void showDiningPhilosopherDemo() {
        setActiveButton(diningPhilosopherBtn);
        currentDemoType = "diningPhilosopher";
        infoText.setText("哲学家就餐问题演示\n演示死锁避免机制");

        // 加载哲学家就餐演示界面
        loadDemoContent("DiningPhilosopherDemo.fxml");
    }

    @FXML
    private void showReaderWriterDemo() {
        setActiveButton(readerWriterBtn);
        currentDemoType = "readerWriter";
        infoText.setText("读者-写者问题演示\n演示读写锁机制");

        // 加载读者-写者演示界面
        loadDemoContent("ReaderWriterDemo.fxml");
    }

    private void setActiveButton(Button activeButton) {
        // 重置所有按钮颜色
        producerConsumerBtn.setStyle("-fx-font-size: 14; -fx-background-color: #6c757d; -fx-text-fill: white;");
        diningPhilosopherBtn.setStyle("-fx-font-size: 14; -fx-background-color: #6c757d; -fx-text-fill: white;");
        readerWriterBtn.setStyle("-fx-font-size: 14; -fx-background-color: #6c757d; -fx-text-fill: white;");

        // 设置活动按钮颜色
        switch (activeButton.getId()) {
            case "producerConsumerBtn":
                activeButton.setStyle("-fx-font-size: 14; -fx-background-color: #3498db; -fx-text-fill: white;");
                break;
            case "diningPhilosopherBtn":
                activeButton.setStyle("-fx-font-size: 14; -fx-background-color: #9b59b6; -fx-text-fill: white;");
                break;
            case "readerWriterBtn":
                activeButton.setStyle("-fx-font-size: 14; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                break;
        }
    }

    private void loadDemoContent(String fxmlFile) {
        try {
            // 清空演示区域
            demoPane.getChildren().clear();

            // 加载对应的演示FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent demoContent = loader.load();

            // 添加到演示区域
            demoPane.getChildren().add(demoContent);


        } catch (Exception e) {
            // 显示错误信息
            VBox errorBox = new VBox();
            errorBox.setAlignment(javafx.geometry.Pos.CENTER);
            errorBox.setSpacing(10);

            Label errorLabel = new Label("无法加载演示内容");
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16;");

            Label detailLabel = new Label("请确保文件存在：" + fxmlFile);
            detailLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

            errorBox.getChildren().addAll(errorLabel, detailLabel);
            demoPane.getChildren().add(errorBox);
        }
    }

    @FXML
    private void startDemo() {
        if ("none".equals(currentDemoType)) {
            return;
        }

        // 这里可以添加具体演示的启动逻辑
        switch (currentDemoType) {
            case "producerConsumer":
                break;
            case "diningPhilosopher":
                break;
            case "readerWriter":
                break;
        }
    }
}