# Export the trained YOLO model for Android

Android cannot load `my_model.pt` directly. Export it to an ExecuTorch `.pte`
program and put the result at:

```text
app/src/main/assets/currency_yolo11s.pte
```

The app's Identify Money screen then uses the ExecuTorch Android runtime and the
existing Loti/Rand class order. The camera capture workflow stays unchanged.

## Export

Use the same Ultralytics version that trained the checkpoint, or a compatible
version with YOLO11 ExecuTorch export support. The installed environment needs
PyTorch, Ultralytics, and the compatible ExecuTorch export dependencies.

```python
from ultralytics import YOLO

model = YOLO("my_model/my_model.pt")
exported = model.export(format="executorch", imgsz=640, batch=1)
print(exported)
```

Ultralytics writes a model directory containing a `.pte` file and metadata. Copy
the `.pte` file to the Android asset path above and name it
`currency_yolo11s.pte`. Preserve the checkpoint's ten trained classes in their
original order; the Android feedback maps those class indices to Loti and Rand
labels.

## Verify before device testing

Run the exported program on a small labeled image set and compare its detections
to the original `.pt` model, with identical 640-pixel letterbox preprocessing,
confidence threshold, and NMS. Confirm output layout (`[1,14,8400]` or
`[1,8400,14]`) and box format before relying on Android postprocessing. Then
assemble the Android app and test the capture flow on a device. ExecuTorch
inference and accuracy have not yet been verified for this checkpoint.
