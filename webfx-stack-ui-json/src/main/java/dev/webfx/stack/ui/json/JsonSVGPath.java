package dev.webfx.stack.ui.json;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.console.Console;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * @author Bruno Salmon
 */
public final class JsonSVGPath {

    public static SVGPath createSVGPath(ReadOnlyAstObject json) {
        String content = json.getString("svgPath");
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(content);
        svgPath.setFill(toPaint(json.getString("fill"), null));
        svgPath.setStroke(toPaint(json.getString("stroke"), null));
        svgPath.setStrokeWidth(json.getDouble("strokeWidth", 1d));
        svgPath.setFillRule(toFillRule(json.getString("fillRule")));
        svgPath.setStrokeLineCap(toStrokeLineCap(json.getString("strokeLineCap")));
        svgPath.setStrokeLineJoin(toStrokeLineJoin(json.getString("strokeLineJoin")));
        svgPath.setStrokeMiterLimit(json.getDouble("strokeMiterLimit", 10d));
        return svgPath;
    }

    private static Paint toPaint(String paintText, Paint defaultPaint) {
        Paint result = defaultPaint;
        if (paintText != null) {
            try {
                if (paintText.startsWith("linear-gradient"))
                    result = LinearGradient.valueOf(paintText);
                else
                    result = Color.web(paintText);
            } catch (Exception e) {
                Console.log(e);
            }
        }
        return result;
    }

    private static FillRule toFillRule(String ruleText) {
        if (ruleText != null) {
            switch (ruleText.trim().toLowerCase()) {
                case "evenodd":
                    return FillRule.EVEN_ODD;
                case "nonzero":
                    return FillRule.NON_ZERO;
            }
        }
        return FillRule.NON_ZERO;
    }

    private static StrokeLineCap toStrokeLineCap(String strokeLineCapText) {
        if (strokeLineCapText != null) {
            switch (strokeLineCapText.trim().toLowerCase()) {
                case "square" : return StrokeLineCap.SQUARE;
                case "butt" : return StrokeLineCap.BUTT;
                case "round" : return StrokeLineCap.ROUND;
            }
        }
        return StrokeLineCap.SQUARE;
    }

    private static StrokeLineJoin toStrokeLineJoin(String StrokeLineJoinText) {
        if (StrokeLineJoinText != null) {
            switch (StrokeLineJoinText.trim().toLowerCase()) {
                case "miter": return StrokeLineJoin.MITER;
                case "bevel": return StrokeLineJoin.BEVEL;
                case "round": return StrokeLineJoin.ROUND;
            }
        }
        return StrokeLineJoin.MITER;
    }
}
