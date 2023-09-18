package dev.webfx.stack.ui.json;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

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
        return svgPath;
    }

    private static Paint toPaint(String paintText, Paint defaultPaint) {
        Paint result = defaultPaint;
        if (paintText != null)
            try {
                result = Color.web(paintText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        return result;
    }

    private static FillRule toFillRule(String ruleText) {
        if (ruleText != null)
            switch (ruleText.trim().toLowerCase()) {
                case "evenodd" : return FillRule.EVEN_ODD;
                case "nonzero" : return FillRule.NON_ZERO;
            }
        return FillRule.NON_ZERO;
    }
}
