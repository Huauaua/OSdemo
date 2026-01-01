package com.hell.osdemo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 加载主页面FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainPage.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            // 设置窗口样式
            primaryStage.setTitle("进程同步演示系统");
            primaryStage.setScene(scene);
//            primaryStage.initStyle(StageStyle.DECORATED);

            // 显示窗口
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("启动应用程序时出错: " + e.getMessage());
            // 提供更详细的错误信息
            if (e.getCause() != null) {
                System.err.println("原因: " + e.getCause().getMessage());
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("应用程序启动中...");
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
        launch(args);
    }
}