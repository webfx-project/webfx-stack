package dev.webfx.stack.ui.util.image;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.util.Strings;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/**
 * @author Bruno Salmon
 */
public final class JsonImageViews {

    public static Node createImageViewOrSVGPath(Object urlOrJson) {
        if (urlOrJson instanceof JsonObject)
            return createImageViewOrSVGPath((JsonObject) urlOrJson);
        return createImageViewOrSVGPath((String) urlOrJson);
    }

    public static Node createImageViewOrSVGPath(String urlOrJson) {
        if (!Strings.startsWith(urlOrJson, "{"))
            return createImageView(urlOrJson);
        return createSVGPath(Json.parseObject(urlOrJson));
    }

    public static Node createImageViewOrSVGPath(JsonObject json) {
        if (json.has("svgPath"))
            return createSVGPath(json);
        return createImageView(json);
    }

    public static ImageView createImageView(Object urlOrJson) {
        if (urlOrJson == null || "".equals(urlOrJson))
            return null;
        if (urlOrJson instanceof JsonObject)
            return createImageView((JsonObject) urlOrJson);
        return createImageView(urlOrJson.toString());
    }

    public static ImageView createImageView(String urlOrJson) {
        if (!Strings.startsWith(urlOrJson, "{"))
            return ImageStore.createImageView(urlOrJson);
        return createImageView(Json.parseObject(urlOrJson));
    }

    public static ImageView createImageView(JsonObject json) {
        return ImageStore.createImageView(json.getString("url"), json.getDouble("width"), json.getDouble("height"));
    }

    public static SVGPath createSVGPath(JsonObject json) {
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
