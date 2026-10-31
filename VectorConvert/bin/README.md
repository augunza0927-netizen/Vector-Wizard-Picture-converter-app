# VectorConvert

> **SE 233 Advanced Programming - Term Project I**  
> Desktop Batch Raster-to-Vector Image Conversion Utility (Inspired by *Vector Magic*)

---

## 🏗️ Architecture & Project Structure

The project follows a clean **Model-View-Controller (MVC) + Service Layer** pattern to maintain clear separation of concerns, high testability, and responsiveness:

```
VectorConvert/
├── pom.xml                                     # Maven build definition & JavaFX dependencies
├── README.md                                   # Project documentation and architectural overview
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/vectorconvert/
    │   │       ├── Launcher.java               # Standard entry point bootstrap
    │   │       ├── VectorConvertApp.java       # JavaFX Application lifecycle & Stage setup
    │   │       │
    │   │       ├── model/                      # Data structures & application state
    │   │       │   ├── DetailLevel.java        # Enum: LOW, MEDIUM, HIGH
    │   │       │   ├── ColorMode.java          # Enum: UNLIMITED, CUSTOM
    │   │       │   ├── ConversionConfig.java   # Settings model (detail, colors, background, constraints)
    │   │       │   └── ImageItem.java          # Image metadata, SVG paths, sizes, state
    │   │       │
    │   │       ├── controller/                 # UI event handling & coordination
    │   │       │   ├── MainController.java     # Root layout coordinator, theme toggle & progress bindings
    │   │       │   ├── DropZoneController.java # Drag-and-drop ingest (.jpg, .png, .zip)
    │   │       │   ├── PreviewController.java  # Zoom (In/Out/Fit) & File Size Comparison display
    │   │       │   └── SettingsController.java # Right panel controls & image navigation
    │   │       │
    │   │       ├── service/                    # Business logic & background concurrency
    │   │       │   ├── VectorizationService.java     # Tracing engine integration (Potrace/AutoTrace)
    │   │       │   ├── ZipExtractionService.java     # ZIP extraction without freezing UI
    │   │       │   ├── BatchProcessingManager.java   # Concurrency manager (parallel vs sequential rules)
    │   │       │   └── ThemeManager.java             # Dynamic CSS theme switching (Light / Dark)
    │   │       │
    │   │       └── util/                       # Helpers & reusable tools
    │   │           └── FileUtils.java          # Size formatting (KB/MB), ratio calculation, validation
    │   │
    │   └── resources/
    │       └── com/vectorconvert/
    │           ├── view/                       # FXML Layout definitions
    │           │   ├── MainView.fxml           # Root container with header, content area & status bar
    │           │   ├── DropZoneView.fxml       # Drag-and-drop landing area
    │           │   ├── PreviewView.fxml        # Side-by-side center preview & zoom controls
    │           │   └── SettingsView.fxml       # Right-side configuration & navigation panel
    │           │
    │           └── css/                        # CSS Stylesheets
    │               ├── base.css                # Common structural styling and typography
    │               ├── theme-light.css         # Clean Light Mode palette
    │               └── theme-dark.css          # Modern Dark Mode palette
```

---

## 📋 Rubric Feature Mapping

| Rubric Requirement | Architectural Component | Description |
| :--- | :--- | :--- |
| **Vectorization Format** | `VectorizationService` | Converts `.jpg` and `.png` raster files into valid `.svg` vector files. |
| **UI Navigation & View** | `PreviewController`, `SettingsController` | Top panel: Zoom In, Zoom Out, Zoom to Fit.<br>Right panel: Back & Next image navigation. |
| **Configuration: Detail & BG** | `ConversionConfig`, `SettingsController` | Detail levels (Low, Med, High) and Remove Background checkbox for transparent SVG. |
| **Configuration: Colors** | `ConversionConfig`, `SettingsController` | Unlimited vs Custom color options (auto-detected, capped at 5, defaulting to 2) with instant preview. |
| **Zip Handling** | `ZipExtractionService` | Accepts `.zip` archive, extracts images, and processes them as batch. |
| **Preprocessing** | `BatchProcessingManager` | Detects colors, prepares medium detail/2 colors, shows preview; concurrent processing with UI progress bar. |
| **Export** | `BatchProcessingManager`, `SettingsController` | Prompts output directory via OS file dialog, processes exports concurrently without thread freezing. |
| **Additional Feature 1** | `ThemeManager` + `theme-*.css` | **Theme Toggle (Dark/Light Mode)**: Dynamic CSS swap for enhanced UX/UI rating. |
| **Additional Feature 2** | `FileUtils` + `PreviewController` | **File Size Comparison Display**: Metadata bar comparing original raster vs SVG size and compression ratio. |
