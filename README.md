# Pixel Bead

拼豆（Perler / Fuse Beads）图纸转换工具 — 导入图片，转换为 Mard 色板限定的拼豆图纸，生成 1:1 打印版图纸和珠子用量统计。

## 功能

- **图片 → 拼豆图纸**：导入 PNG/JPG/GIF/BMP，等比缩放居中，CIEDE2000 感知色差匹配到色板最近色
- **底板配置**：Mard 标准板（2.6mm 50×50 / 29×29，5.0mm 29×29 / 14×14），自定义板（≤200×200），板数拼接（1×1 ~ 4×4）
- **Pixel mode**：平均色（默认，区域积分抗噪）/ 最近色
- **插值模式**：Bilinear（照片）/ Nearest（像素风）
- **抖动**：Floyd-Steinberg / Atkinson + 强度调节（Average 模式自动禁用）
- **孤立点清理**：Off / Light / Medium / Strong 四档
- **相似色合并**：ΔE2000 五档（2/4/7/12）+ 最小占比（相对 %），合并相近低频色
- **色盘选择**：Mard 标准 221 色 / 全色 291 色（右侧面板下拉）
- **多语言**：中文 / English 一键切换（状态栏右下角，即时生效）
- **图纸查看**：滚轮/触控板捏合缩放、空格+左键 / 右键 / 中键 / 无工具左键 / 双指平移、子网格加粗线（间隔自适应板尺寸）、悬停显示坐标与色号
- **导入与裁剪**：导入时裁剪选区（拖拽/四边四角调整/锁定 1:1），只转换选中区域
- **编辑**：画笔 / 橡皮 / 吸管工具（默认不选中），色板点击选色，撤销/重做（Cmd+Z / Cmd+Shift+Z），用量统计实时刷新
- **批量替换色块**：统计表悬浮脉冲高亮定位；右键显示相近色（ΔE2000）或从色板选目标色，实时预览替换效果，一步撤销
- **预览模式**：隐藏网格线显示成品效果（工具栏 Preview 或快捷键 P）
- **主题**：深色 / 浅色一键切换（默认浅色），现代 ins 风格 UI（圆角/阴影/蓝紫 accent）
- **用量统计**：按色号统计珠子数量，降序列表
- **导出**：
  - PNG（300 DPI，1:1 物理尺寸，色块+色号标注+坐标轴+图例）
  - PDF（物理尺寸单页；自定义板子网格间隔 ≥2 时输出**多板 Tiling 图纸**：总览页 + 每块一页 A4，页眉标注板号与行列范围，占满居中）
  - Text（色号矩阵，`.` 表示空格）

## 技术栈

Java 21 + JavaFX 21 + Maven（JPMS 模块化），ControlsFX、Ikonli、Jackson、PDFBox。

## 开发

```bash
# 需要 JDK 21（本机: ~/Documents/Environments/Java/jdk-21.0.9.jdk）
export JAVA_HOME=~/Documents/Environments/Java/jdk-21.0.9.jdk/Contents/Home

# 测试
./mvnw test

# 开发运行（macOS Dock 图标自动生效）
./mvnw javafx:run
```

> **macOS Dock 图标**：`mvnw javafx:run` 已配置 `--add-exports`，开发期即显示应用图标。若用 IntelliJ 直接运行（非 Maven goal），需在 Run Configuration 的 VM options 加：
> `--add-exports java.desktop/com.apple.eawt=com.johnie.pixelbead`

## 打包

版本号**单一来源 = `pom.xml` 的 `<version>`**，打包脚本与 CI 自动读取；本地脚本第二个参数可覆盖（如 `./build-mac.sh dmg 1.0.1`）。

### macOS（本机）

```bash
./build-mac.sh app-image   # 免安装 .app
./build-mac.sh dmg         # 安装包 → target/dist/Pixel Bead-*.dmg
```

### Windows（需在 Windows 机器上执行，jpackage 不能交叉打包）

```bat
build-win.bat app-image    :: 免安装目录
build-win.bat all          :: app-image + 便携 zip + .exe 安装器（exe 需 WiX Toolset 3.14+）
build-win.bat exe          :: 仅 .exe 安装器（需 WiX）
build-win.bat msi          :: 仅 .msi 安装器（需 WiX）
```

### GitHub Actions 自动发版

推 `v*` tag 自动构建并创建 Draft Release（macOS dmg + Windows exe + Windows 便携 zip，版本取自 tag）：

```bash
git tag v1.0.0 && git push origin v1.0.0
```

手动触发：Actions 页面 → **Release** → **Run workflow**（版本取自 pom.xml）。

## 架构

```
engine/         核心引擎层（纯 Java + AWT，无 JavaFX 依赖）
  model/        BeadBoard / BeadColor / BeadPalette / PatternProject
  quantizer/    ColorSpace (sRGB→Lab) / ColorDifference (ΔE2000) / ImageDownsampler
  renderer/     PatternRenderer (离线图纸) / PatternExporter (PNG/PDF/Text/多板Tiling)
  BeadEngine    转换管线：降采样 → 量化 → 清理/合并 → PatternProject
enums/          枚举集中管理（Dithering/Quantization/Interpolation/OrphanLevel/
                MergePreset/ExportFormat/PaletteChoice/ToolType/Theme，实现 I18n.Key）
ui/             视图控制层
  state/AppState        全局状态（ObjectProperty 响应式）
  components/InteractiveCanvas  无限视口 Canvas（缩放/平移/悬停/编辑）
  MainController        布局控制器
resources/
  palettes/             mard_standard.json (291) / mard_standard_221.json (221)
  i18n/                 messages*.properties（中英双语）
  fxml/main.fxml + css/style.css
```

## 色板说明

- `palettes/mard_standard.json` — **Mard 全色 291**（221 标准 A-H+M + 70 扩展）
- `palettes/mard_standard_221.json` — **Mard 标准 221**（A-H+M 九系列，官网 Standard range）
- 数据源：官网 pixel-beads.com（经 maxcleme/beadcolors 仓库整理），schema：

```json
{ "brand": "Mard", "range": "standard-221", "colors": [ { "code": "A1", "hex": "#FAF4C8", "rgb": [250, 244, 200] } ] }
```
