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

#### 卡尔曼滤波
* 总结: 根据本次的估计值X和实际测量值Z，结合卡尔曼增益K，得到最优的值；
* 描述公式: Target = X + K(Z - X)
* 描述: 估计值和实测值的方差情况，来得到K，两个值中哪一个值的方差越小，它所占的权重也就更大；

### WiFi

#### WifiManager.startScan()应用程序触发扫描请求的功能将在以后的版本中删除
* Android 9: 及以上2分钟以内只允许扫描4次WiFi，XiaoMi实测前4次扫描间隔大概2S；
* Android 8: 原生系统默认10S扫描一次；
* Android 7: XiaoMi实测扫描间隔约7.5S，并且在扫描成功一次时，一般情况下可以快速扫描第二次，估计也是10S扫描一次；
* Android 4.4 XiaoMi实测扫描间隔约1.5S；