# Test the currency model on Windows

The model can be run locally against a photo without connecting an Android phone.
Install 64-bit Python 3.12, then from the project folder run in PowerShell:

```powershell
py -3.12 -m venv .venv-model-test
.\.venv-model-test\Scripts\python.exe -m pip install -r model\requirements-test.txt
.\.venv-model-test\Scripts\python.exe model\test_currency_model.py "model\07a92952-IMG_8424_JPG.rf.47129cda342d6a0819f5ecc90564d467.jpg"
```

The script prints tensor shapes, the highest raw class score, and detections, then
saves an annotated image beside the input. Use a new photo path or specify `--output`
to keep earlier results. For example:

```powershell
.\.venv-model-test\Scripts\python.exe model\test_currency_model.py "C:\path\to\banknote-photo.jpg" --threshold 0.25 --output result.jpg
```

The script uses the bundled `best.tflite`, model-matching letterbox preprocessing,
automatic normalized-or-pixel box conversion, and the app's class order and NMS.
Use a bright, sharp photo with one whole note visible and little obstruction. This is
a desktop CPU check; camera capture and Android performance still need separate checks.
