## 基于WiFi和PDR的室内定位系统设计

### 关键传感器
- TYPE_ROTATION_VECTOR：方向传感器，融合类型，用来获取手机当前的方向;
- TYPE_ACCELEROMETER：加速度传感器，分为xyz三轴;
- TYPE_STEP_DETECTOR：步数传感器，融合类型，用户每走一步传感器就会产生一次回调;

### PDR
#### 手机的XYZ
- X: 沿手机横向形成的坐标轴，单手握方式为横向;
- Y: 沿手机纵向形成的坐标轴，双手握方式为纵向;
- Z: 垂直手机屏幕形成的坐标轴，人眼看手机为垂直向;
