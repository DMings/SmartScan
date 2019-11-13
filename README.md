## SmartScan

[**SmartScan**](https://github.com/DMings/SmartScan)运用OpenGL从零打造一个依赖极低、高灵活、快速预览不变形ZXing扫码库，实现思路写在最后，喜欢可以看下去。

 - 下面是演示效果，可直接下载试试 [演示apk](https://github.com/DMings/SmartScan/blob/master/demosrc/smart_scan_v0.0.1.apk)

![旋转扫码演示](https://github.com/DMings/SmartScan/blob/master/demosrc/rotation_scan_480.gif)
![扫描途中变换参数](https://github.com/DMings/SmartScan/blob/master/demosrc/change_scan_480.gif)

 - **依赖极低：**
 
 做一个老实的扫码库，权限判断应由应用自行判断；除去kotlin自身库仅依赖com.google.zxing:core:3.3.3，并且zxing:core用api方式依赖，方便引用。这里提醒一下3.4.0是有限制的，目前最好不要用，具体可查阅官网
- **高灵活：**
 1. 扫描线和扫描框可以是一张图片；扫描窗口大小可以设置相对高度、宽度的比例，也可以直接设置具体大小，窗口宽高比可变。同时提供属性在宽高不一致情况下约束为正方形（方便二维码扫描）。**若还是不喜欢，可以把界面所有UI隐藏，然后这里提供窗口位置监听，在回调位置变化的时候自行设置自定义UI的位置**
2. 考虑到闪光灯的自定义程度比较高的问题，本库也提供一个默认实现，默认是关闭的，需要手动开启。如不喜欢可以自行定义，同时也提供闪光灯方法控制，即实现UI即可
3. 扫码中途配置可变，可以在扫码途中更改扫码种类，扫码窗口大小等，**所有参数均可改变**
4. 提供丰富的监听：结果监听（包括在UI线程、解码线程监听，同时也提供持续监听和监听到一个结果后停止）、图像数据监听、扫码窗口改变监听
5. 灵活的创建方式，可从xml中创建，同时也能以代码的形式直接new出来
- **快速预览不变形**
1. 为提高识别速度，针对一般扫码只用到二维码，也就是QR码，所以二维码部分仅保留QR识别，默认也仅开启QR识别。一维码种类较多，这里也提供，若需要仅使用一维码或二维码一维码同时使用，也提供相应的属性供设置
2. 不必考虑是否全屏，保证**预览图像不变形**，处理好**图像旋转**问题，包括activity旋转的自动销毁创建问题、配置 android:configChanges="keyboardHidden|orientation|screenSize"  的时候，旋转不销毁问题
- **该库使用方式：**

在build.gradle加入依赖
```
implementation 'com.dming.glScan:smartscan:0.0.1'
```
若引用不上可在项目的build.gradle加上maven仓库地址，如下：
```
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://dl.bintray.com/dming/maven' }
    }
}
```
扫描框、扫描线、背景等基础UI已实现，若没有需求，开箱即用；若有定制，按需设置参数即可

xml创建：
```
<com.dming.glScan.SmartScanView
  android:id="@+id/glScanView"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  app:scanPercentTopOffset="0.2"
  app:scanPercentWidth="0.72"
  app:scanPercentHeight="0.55"
  app:enableFlashlightBtn="true"
  />
```
代码直接创建：
```
val glScanView = SmartScanView(this)
val smartScanParameter = SmartScanParameter()
smartScanParameter.apply {
    this.scanPercentWidth = 0.65f
    this.scanPercentHeight = 0.65f
    this.scanPercentTopOffset = 0.2f
    this.scanMustSquare = true
    this.enableFlashlightBtn = true
}
glScanView.init(smartScanParameter)
```
动态改变配置：
```
val oneDP = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, 1f,
    this.resources.displayMetrics
)
val smartScanParameter = glScanView.getGLScanParameter()
smartScanParameter.scanTopOffset = 80 * oneDP
smartScanParameter.scanWidth = 300 * oneDP
smartScanParameter.scanHeight = 300 * oneDP
smartScanParameter.scanLineWidth = 6 * oneDP
smartScanParameter.onlyOneDCode = false
smartScanParameter.enableFlashlightBtn = true
glScanView.updateConfigure(smartScanParameter)    
```
监听结果：

```
glScanView.setOnResultOnceListener {
  Toasty.success(this, "result: $it", Toast.LENGTH_LONG).show()
}
```
监听扫描框的位置变化，实现自定义UI重要监听器：

```
glScanView.setScanViewChangeListener(object : OnScanViewListener {
   // surface创建的时候回调
   override fun onCreate() {
   }
   // 扫描窗口位置变化即回调
   override fun onChange(rect: Rect) {
   }
   // surface销毁的时候回调
   override fun onDestroy() {
   }
})
```
- 参数配置大全：

```
class SmartScanParameter {
    // 扫描框宽度，单位像素
    var scanWidth: Float = 0f
    // 扫描框高度，单位像素
    var scanHeight: Float = 0f
    // 扫描框宽度百分比 0 - 1.0f
    var scanPercentWidth: Float = 0f
    // 扫描框高度百分比 0 - 1.0f
    var scanPercentHeight: Float = 0f
    // 扫描框距离头部偏移，单位像素
    var scanTopOffset: Float = 0f
    // 扫描框距离头部偏移，相对高度百分比 0 - 1.0f
    var scanPercentTopOffset: Float = 0f
    // 约束扫描框为正方形，将使用最短的边为边长，可用在扫描二维码,默认开启
    var scanMustSquare: Boolean? = null
    // 扫描线drawable，当存在时，优先使用
    var scanLine: Drawable? = null
    // 扫描框drawable，当存在时，优先使用
    var scanCorner: Drawable? = null
    // 扫描背景颜色，一般黑色透明
    var scanBackgroundColor: Int? = null
    // 是否加入一维码解码，默认为QR二维码解码，按需加入
    var addOneDCode: Boolean = false
    // 仅使用一维码解码
    var onlyOneDCode: Boolean = false
    // 扫描角颜色
    var scanCornerColor: Int? = null
    // 自定义扫描角长度
    var scanCornerSize: Float = 0f
    // 自定义扫描角线宽度
    var scanCornerThick: Float = 0f
    // 扫描线颜色
    var scanLineColor: Int? = null
    // 扫描线宽度大小
    var scanLineWidth: Float = 0f
    // 扫描框的细线宽度，一般1px
    var scanFrameLineWidth: Float? = null
    // 扫描框的细线颜色
    var scanFrameLineColor: Int? = null
    // 禁止窗口双指放大，默认开启
    var disableScale: Boolean = false
    // 使能滴一声的扫描成功声音
    var enableBeep: Boolean = false
    // 使能震动扫描成功
    var enableVibrate: Boolean = false
    // 使能闪光灯按钮，由于按钮位置图像可变性太强，默认是不开启的，由用户定制
    var enableFlashlightBtn: Boolean = false
}
```
XML参数也是同名，用法与上相同
```
<declare-styleable name="SmartScanView">
    <attr name="scanWidth" format="dimension" />
    <attr name="scanHeight" format="dimension" />
    <attr name="scanPercentWidth" format="float" />
    <attr name="scanPercentHeight" format="float" />
    <attr name="scanTopOffset" format="dimension" />
    <attr name="scanPercentTopOffset" format="float" />
    <attr name="scanLine" format="reference" />
    <attr name="scanCorner" format="reference" />
    <attr name="scanBackgroundColor" format="color" />
    <attr name="addOneDCode" format="boolean" />
    <attr name="onlyOneDCode" format="boolean" />
    <attr name="scanMustSquare" format="boolean" />
    <attr name="scanCornerSize" format="dimension" />
    <attr name="scanCornerThick" format="dimension" />
    <attr name="scanLineWidth" format="dimension" />
    <attr name="scanFrameLineWidth" format="dimension" />
    <attr name="scanFrameLineColor" format="color" />
    <attr name="scanLineColor" format="color" />
    <attr name="scanCornerColor" format="color" />
    <attr name="disableScale" format="boolean" />
    <attr name="enableBeep" format="boolean" />
    <attr name="enableVibrate" format="boolean" />
    <attr name="enableFlashlightBtn" format="boolean" />
</declare-styleable>
```
 - 以下为所有方法：
 
// 设置获取亮度（灰度）图片监听  
setGrayImgListener  
// 从解码线程中监听解码结果，会不断的触发，可使用stopDecode()关闭  
setOnResultInThreadListener  
// 从解码线程中监听解码结果，成功后会停止，可使用startDecode()开启  
setOnResultOnceInThreadListener  
// 从UI线程中监听解码结果，会不断的触发，可使用stopDecode()关闭  
setOnResultListener    
// 从UI线程中监听解码结果，成功后会停止，可使用startDecode()开启  
setOnResultOnceListener  
// 监听smart的扫码窗口改变，自定义窗口极奇重要的监听  
setScanViewChangeListener  
// 手动开启解码  
startDecode  
// 手动关闭解码  
stopDecode  
// 闪光灯开启关闭  
setFlashLight  
// 扫码过程中改变配置，如窗口大小，扫码类型等  
updateConfigure  

- **实现思路：**

Camera在GL线程开启，摄像头数据由该线程分别以亮度方式绘制到FBO和以原始图像方式绘制到预览界面，解码线程共享GL线程context，先绘制FBO中的亮度纹理数据，然后在绘制结果中读取像素，当然仅仅是读取设置的扫描窗口区域，然后进行解码。由于摄像头数据由自定义GL着色器绘制，可控性高，通过计算绘制出无变形的图像。ZXing解码过程需要转亮度，FBO中的纹理已经是亮度纹理，也就不用专门转亮度，算是提高了一丢丢的效率。考虑到activity旋转问题和surface的生命周期，surface在旋转、离开activity后会被销毁；既然surface销毁了，预览界面都没了，camera也就不必存在了，故把camera与surface捆绑起来，同生死。此外，剩下的问题就是摄像头旋转问题，这个需要拿getDefaultDisplay().getRotation()真实的旋转角度来计算出合适的旋转角度进行矫正。
