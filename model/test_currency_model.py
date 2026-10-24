#!/usr/bin/env python3
"""Run the bundled currency TFLite model on one local image."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps
from ai_edge_litert.interpreter import Interpreter


CLASSES = [
    "10 Loti", "10 Rand", "100 Loti", "100 Rand", "20 Loti",
    "20 Rand", "200 Loti", "200 Rand", "50 Loti", "50 Rand",
]


def letterbox(image: Image.Image, width: int, height: int):
    scale = min(width / image.width, height / image.height)
    resized = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.BILINEAR)
    canvas = Image.new("RGB", (width, height), (114, 114, 114))
    pad_x = (width - resized.width) // 2
    pad_y = (height - resized.height) // 2
    canvas.paste(resized, (pad_x, pad_y))
    return canvas, scale, pad_x, pad_y


def decode(output: np.ndarray, image_size, input_size, score_threshold, iou_threshold):
    values = np.asarray(output, dtype=np.float32).squeeze()
    if values.ndim != 2:
        raise ValueError(f"Expected rank-2 YOLO output after squeeze, got {values.shape}")
    if values.shape[0] in (14, 15):
        values = values.T
    if values.shape[1] != 14:
        raise ValueError(f"Expected 4 box values + 10 classes, got output shape {values.shape}")

    if np.nanmax(np.abs(values[:, :4])) <= 2.0:
        values[:, [0, 2]] *= input_size[0]
        values[:, [1, 3]] *= input_size[1]

    image_w, image_h = image_size
    input_w, input_h = input_size
    scale = min(input_w / image_w, input_h / image_h)
    pad_x = (input_w - image_w * scale) / 2
    pad_y = (input_h - image_h * scale) / 2
    scores = values[:, 4:]
    labels = scores.argmax(axis=1)
    conf = scores[np.arange(len(scores)), labels]
    keep = conf >= score_threshold
    boxes = values[keep, :4]
    conf = conf[keep]
    labels = labels[keep]

    x1 = (boxes[:, 0] - boxes[:, 2] / 2 - pad_x) / scale
    y1 = (boxes[:, 1] - boxes[:, 3] / 2 - pad_y) / scale
    x2 = (boxes[:, 0] + boxes[:, 2] / 2 - pad_x) / scale
    y2 = (boxes[:, 1] + boxes[:, 3] / 2 - pad_y) / scale
    x1 = np.clip(x1, 0, image_w - 1)
    y1 = np.clip(y1, 0, image_h - 1)
    x2 = np.clip(x2, 0, image_w - 1)
    y2 = np.clip(y2, 0, image_h - 1)

    order = conf.argsort()[::-1]
    selected = []
    while len(order):
        idx = int(order[0])
        if x2[idx] > x1[idx] and y2[idx] > y1[idx]:
            selected.append(idx)
        if len(order) == 1:
            break
        rest = order[1:]
        xx1 = np.maximum(x1[idx], x1[rest])
        yy1 = np.maximum(y1[idx], y1[rest])
        xx2 = np.minimum(x2[idx], x2[rest])
        yy2 = np.minimum(y2[idx], y2[rest])
        inter = np.maximum(0, xx2 - xx1) * np.maximum(0, yy2 - yy1)
        area_a = (x2[idx] - x1[idx]) * (y2[idx] - y1[idx])
        area_b = (x2[rest] - x1[rest]) * (y2[rest] - y1[rest])
        iou = inter / np.maximum(area_a + area_b - inter, 1e-8)
        order = rest[iou < iou_threshold]
    return [(CLASSES[labels[i]], float(conf[i]), (x1[i], y1[i], x2[i], y2[i])) for i in selected]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path, help="Local banknote photo (JPG/PNG)")
    parser.add_argument("--model", type=Path, default=Path(__file__).with_name("best.tflite"))
    parser.add_argument("--threshold", type=float, default=0.25, help="Detection score threshold (default: 0.25)")
    parser.add_argument("--output", type=Path, help="Annotated output image path")
    args = parser.parse_args()

    model_path = args.model.resolve()
    image_path = args.image.resolve()
    interpreter = Interpreter(model_path=str(model_path), num_threads=4)
    interpreter.allocate_tensors()
    in_detail = interpreter.get_input_details()[0]
    out_detail = interpreter.get_output_details()[0]
    print(f"Model input: shape={in_detail['shape'].tolist()} dtype={in_detail['dtype']}")
    print(f"Model output: shape={out_detail['shape'].tolist()} dtype={out_detail['dtype']}")

    shape = in_detail["shape"]
    if len(shape) != 4 or shape[0] != 1:
        raise ValueError(f"Unsupported model input shape: {shape}")
    nhwc = shape[-1] == 3
    height, width = (shape[1], shape[2]) if nhwc else (shape[2], shape[3])
    image = ImageOps.exif_transpose(Image.open(image_path)).convert("RGB")
    prepared, scale, pad_x, pad_y = letterbox(image, width, height)
    array = np.asarray(prepared, dtype=np.float32) / 255.0
    if not nhwc:
        array = array.transpose(2, 0, 1)
    array = array[None, ...]
    if in_detail["dtype"] != np.float32:
        q_scale, q_zero = in_detail["quantization"]
        if not q_scale:
            raise ValueError("Quantized input tensor has no scale")
        limits = np.iinfo(in_detail["dtype"])
        array = np.clip(np.rint(array / q_scale + q_zero), limits.min, limits.max).astype(in_detail["dtype"])
    else:
        array = array.astype(np.float32)

    interpreter.set_tensor(in_detail["index"], array)
    interpreter.invoke()
    raw = interpreter.get_tensor(out_detail["index"])
    if raw.dtype != np.float32:
        q_scale, q_zero = out_detail["quantization"]
        if not q_scale:
            raise ValueError("Quantized output tensor has no scale")
        raw = (raw.astype(np.float32) - q_zero) * q_scale

    detections = decode(raw, image.size, (width, height), args.threshold, 0.45)
    raw_scores = np.asarray(raw, dtype=np.float32).squeeze()
    if raw_scores.ndim == 2:
        if raw_scores.shape[0] in (14, 15):
            raw_scores = raw_scores.T
        if raw_scores.shape[1] >= 14:
            top = raw_scores[:, 4:14].max(axis=1)
            best = int(top.argmax())
            label_index = int(raw_scores[best, 4:14].argmax())
            print(f"Top raw candidate: {CLASSES[label_index]} {float(top[best]):.1%}")
    if not detections:
        print("No banknote detected. Try a closer, sharper photo with the full note visible.")
    for label, score, box in detections:
        print(f"{label}: {score:.1%}  box={[round(float(v), 1) for v in box]}")

    output_path = args.output or image_path.with_name(f"{image_path.stem}_currency_result.jpg")
    annotated = image.copy()
    draw = ImageDraw.Draw(annotated)
    for label, score, box in detections:
        coords = tuple(round(float(v)) for v in box)
        text = f"{label} {score:.0%}"
        draw.rectangle(coords, outline=(0, 230, 118), width=max(3, image.width // 300))
        draw.text((coords[0], max(0, coords[1] - 18)), text, fill=(0, 230, 118), stroke_width=1)
    annotated.save(output_path)
    print(f"Annotated image: {output_path}")


if __name__ == "__main__":
    main()
