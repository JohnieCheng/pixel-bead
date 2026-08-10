# Pixel Bead

拼豆（Perler / Fuse Beads）图纸转换工具 — 导入图片，转换为 Mard 色板限定的拼豆图纸，生成 1:1 打印版图纸和珠子用量统计。

## 功能

- **图片 → 拼豆图纸**：导入 PNG/JPG/GIF/BMP，等比缩放居中，CIEDE2000 感知色差匹配到色板最近色
- **底板配置**：Mard 标准板（2.6mm 50×50 / 29×29，5.0mm 29×29 / 14×14），板数拼接（1×1 ~ 4×4）
- **插值模式**：Bilinear（照片）/ Nearest（像素风）
- **图纸查看**：滚轮缩放、空格/中键拖拽平移、子网格加粗线、悬停显示坐标与色号
- **用量统计**：按色号统计珠子数量，降序列表
- **导出**：
  - PNG（300 DPI，1:1 物理尺寸，色块+色号标注+坐标轴+图例）
  - PDF（PDFBox 单页，物理尺寸精确）
  - Text（色号矩阵，`·` 表示空格）

## 技术栈

Java 21 + JavaFX 21 + Maven（JPMS 模块化），ControlsFX、Ikonli、Jackson、PDFBox。

## 构建与运行

```bash
# 需要 JDK 21（本机: ~/Documents/Environments/Java/jdk-21.0.9.jdk）
export JAVA_HOME=~/Documents/Environments/Java/jdk-21.0.9.jdk/Contents/Home

# 测试
./mvnw test

# 开发运行
./mvnw javafx:run

# 打包 macOS 应用（app-image / dmg）
./build-mac.sh app-image
./build-mac.sh dmg        # 产物: target/dist/Pixel Bead-1.0.0.dmg
```

### Windows 打包（.exe）

必须在 **Windows** 机器上执行（jpackage 不能交叉打包）：

```bat
:: 前置: JDK 21 (JAVA_HOME), Maven 依赖会自动下载
build-win.bat app-image   :: 免安装绿色版目录
build-win.bat exe         :: .exe 安装器（需 WiX Toolset 3.14+）
build-win.bat msi         :: .msi 安装器（需 WiX Toolset 3.14+）
```

产物在 `target\dist\`。脚本会自动使用 Windows 版 JavaFX（win classifier）。

## 架构

```
engine/         核心引擎层（纯 Java + AWT，无 JavaFX 依赖）
  model/        BeadBoard / BeadColor / BeadPalette / PatternProject
  quantizer/    ColorSpace (sRGB→Lab) / ColorDifference (ΔE2000) / ImageDownsampler
  renderer/     PatternRenderer (离线图纸) / PatternExporter (PNG/PDF/Text)
  BeadEngine    转换管线：降采样 → 量化 → PatternProject
ui/             视图控制层
  state/AppState        全局状态（ObjectProperty 响应式）
  components/InteractiveCanvas  无限视口 Canvas（缩放/平移/悬停）
  MainController        布局控制器
resources/
  palettes/mard_standard.json   色板数据
  fxml/main.fxml + css/style.css
```

## 色板说明

`src/main/resources/palettes/mard_standard.json` 当前为**占位数据**（197 色，固定种子生成，覆盖全色相），用于开发测试。替换为真实 Mard 色卡数据时保持 schema：

```json
{ "brand": "Mard", "colors": [ { "code": "CE001", "name": "", "rgb": [60, 85, 93] } ] }
```
